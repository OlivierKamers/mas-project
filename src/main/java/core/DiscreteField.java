package core;

import com.github.rinde.rinsim.core.model.comm.Message;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.ImmutableList;
import core.messages.PositionBroadcast;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DiscreteField {
    static double DEFAULT_TAXI_INFLUENCE_RANGE = 0.5;
    private static double CAPACITY_WEIGHT = 0.5;
    private double[][][] fieldData;
    private double[] maxFieldValues;
    private int tDim;
    private int xDim;
    private int yDim;
    private Duration durationPerFrame;
    private int matrixStep;
    private double taxiInfluenceRange;

    DiscreteField(double[][][] data, double[] maxFieldValues, Duration durationPerFrame, int matrixStep, double taxiInfluenceRange) {
        this.fieldData = data;
        this.maxFieldValues = maxFieldValues;
        this.tDim = fieldData.length;
        this.xDim = fieldData[0].length;
        this.yDim = fieldData[0][0].length;
        this.durationPerFrame = durationPerFrame;
        this.matrixStep = matrixStep;
        this.taxiInfluenceRange = taxiInfluenceRange;
    }

    DiscreteField() {
        this.fieldData = new double[0][0][0];
        this.maxFieldValues = new double[0];
        this.tDim = 0;
        this.xDim = 0;
        this.yDim = 0;
        this.durationPerFrame = Duration.ofMillis(0);
        this.matrixStep = 0;
        this.taxiInfluenceRange = 0;
    }

    private int getTDimension() {
        return this.tDim;
    }

    int getXDimension() {
        return this.xDim;
    }

    int getYDimension() {
        return this.yDim;
    }

    int getFrameIndexForTime(long time) {
        return Math.min(this.getTDimension() - 1, (int) Math.floor(time / durationPerFrame.toMillis()));
    }

    double getValue(int t, int x, int y) {
        return this.fieldData[t][x][y];
    }

    double getMaxValue(int t) {
        return this.maxFieldValues[t];
    }

    private int[] convertMapToFieldCoordinates(Point p) {
        int xBin = (int) Math.min(xDim - 1, Math.floor(p.x * xDim / (Helper.ROADMODEL_BOUNDARIES_SCALE * Helper.getXScale())));
        int yBin = (int) Math.min(yDim - 1, Math.floor(p.y * yDim / (Helper.ROADMODEL_BOUNDARIES_SCALE * Helper.getYScale())));
        return new int[]{xBin, yBin};
    }

    // Middle of square
    private Point convertFieldToMapCoordinates(int xBin, int yBin) {
        double x = (xBin + 0.5) * Helper.ROADMODEL_BOUNDARIES_SCALE * Helper.getXScale() / xDim;
        double y = (yBin + 0.5) * Helper.ROADMODEL_BOUNDARIES_SCALE * Helper.getYScale() / yDim;
        return new Point(x, y);
    }

    private List<int[]> getLeftCoords(int offset, int xPos, int yPos) {
        List<int[]> res = new ArrayList<>();
        int x = xPos - offset;
        if (x >= 0) {
            for (int y = Math.max(0, yPos - offset); y <= Math.min(yPos + offset, yDim - 1); y++) {
                res.add(new int[]{x, y});
            }
        }
        return res;
    }

    private List<int[]> getRightCoords(int offset, int xPos, int yPos) {
        List<int[]> res = new ArrayList<>();
        int x = xPos + offset;
        if (x < xDim) {
            for (int y = Math.max(0, yPos - offset); y <= Math.min(yPos + offset, yDim - 1); y++) {
                res.add(new int[]{x, y});
            }
        }
        return res;
    }

    private List<int[]> getTopCoords(int offset, int xPos, int yPos) {
        List<int[]> res = new ArrayList<>();
        int y = yPos - offset;
        if (y >= 0) {
            for (int x = Math.max(0, xPos - offset); x <= Math.min(xPos + offset, xDim - 1); x++) {
                res.add(new int[]{x, y});
            }
        }
        return res;
    }

    private List<int[]> getBottomCoords(int offset, int xPos, int yPos) {
        List<int[]> res = new ArrayList<>();
        int y = yPos + offset;
        if (y < yDim) {
            for (int x = Math.max(0, xPos - offset); x <= Math.min(xPos + offset, xDim - 1); x++) {
                res.add(new int[]{x, y});
            }
        }
        return res;
    }

    Vector2D getNextPosition(Taxi taxi, long time, ImmutableList<Message> messages, int range) {
        int t = getFrameIndexForTime(time);
        int[] pos = convertMapToFieldCoordinates(taxi.getPosition().get());
        int xPos = pos[0];
        int yPos = pos[1];
        boolean nonZero = false;
        Point taxiPosition = taxi.getPosition().get();

        Vector2D vector = new Vector2D(0, 0);

        List<PositionBroadcast> positionBroadcasts = messages.stream()
                .filter(m -> m.getContents() instanceof PositionBroadcast)
                .map(m -> (PositionBroadcast) m.getContents())
                .collect(Collectors.toList());

        for (PositionBroadcast pb : positionBroadcasts) {
            Vector2D diff = new Vector2D(pb.getPosition().x - taxiPosition.x, pb.getPosition().y - taxiPosition.y);
            vector = vector.add(-1.0 * CAPACITY_WEIGHT * pb.getFreeCapacity() * (1 - Math.min(1, Point.distance(taxiPosition, pb.getPosition()) / taxiInfluenceRange)), diff);
        }

        for (int offset = 0; offset < matrixStep; offset++) {
            List<int[]> positions = new ArrayList<>();
            positions.addAll(getLeftCoords(offset, xPos, yPos));
            positions.addAll(getRightCoords(offset, xPos, yPos));
            positions.addAll(getTopCoords(offset, xPos, yPos));
            positions.addAll(getBottomCoords(offset, xPos, yPos));

            for (int[] p : positions) {
                double fieldValue = getValue(t, p[0], p[1]);
                if (fieldValue > 10e-3) {
                    nonZero = true;
                    Point fieldPoint = convertFieldToMapCoordinates(p[0], p[1]);
                    Vector2D diff = new Vector2D(fieldPoint.x - taxiPosition.x, fieldPoint.y - taxiPosition.y);
                    vector = vector.add(fieldValue / Point.distance(taxiPosition, fieldPoint), diff);
                }
            }

            if (nonZero && offset > range) {
                break;
            }
        }

        return vector;
    }
}
