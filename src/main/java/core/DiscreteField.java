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
    private static double CAPACITY_WEIGHT = 0.5;
    private double[][][] fieldData;
    private double[] maxFieldValues;
    private int tDim;
    private int xDim;
    private int yDim;
    private Duration durationPerFrame;

    public DiscreteField(double[][][] data, double[] maxFieldValues, Duration durationPerFrame) {
        this.fieldData = data;
        this.maxFieldValues = maxFieldValues;
        this.tDim = fieldData.length;
        this.xDim = fieldData[0].length;
        this.yDim = fieldData[0][0].length;
        this.durationPerFrame = durationPerFrame;
    }

    public DiscreteField() {
        this.fieldData = new double[0][0][0];
        this.maxFieldValues = new double[0];
        this.tDim = 0;
        this.xDim = 0;
        this.yDim = 0;
        this.durationPerFrame = Duration.ofMillis(0);
    }

    public static void printField(double[][] field) {
        int yDim = field[0].length;
        for (int y = 0; y < yDim; y++) {
            StringBuilder sb = new StringBuilder();
            for (double[] row : field) {
                sb.append(row[y]).append(' ');
            }
            System.out.println(sb.toString());
        }
    }

    public void printField(int t) {
        printField(fieldData[t]);
    }

    public int getTDimension() {
        return this.tDim;
    }

    public int getXDimension() {
        return this.xDim;
    }

    public int getYDimension() {
        return this.yDim;
    }

    public int getFrameIndexForTime(long time) {
        return Math.min(this.getTDimension() - 1, (int) Math.floor(time / durationPerFrame.toMillis()));
    }

    public double getValue(int t, int x, int y) {
        return this.fieldData[t][x][y];
    }

    public double getMaxValue(int t) {
        return this.maxFieldValues[t];
    }

    public int[] convertMapToFieldCoordinates(Point p) {
        int xBin = (int) Math.min(xDim - 1, Math.floor(p.x * xDim / (Helper.ROADMODEL_BOUNDARIES_SCALE * Helper.getXScale())));
        int yBin = (int) Math.min(yDim - 1, Math.floor(p.y * yDim / (Helper.ROADMODEL_BOUNDARIES_SCALE * Helper.getYScale())));
        return new int[]{xBin, yBin};
    }

    // Middle of square
    public Point convertFieldToMapCoordinates(int xBin, int yBin) {
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

    public Vector2D getNextPosition(Taxi taxi, long time, ImmutableList<Message> messages, int range) {
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
            vector = updateVectorWithPB(taxiPosition, vector, pb);
        }

        for (int offset = 0; offset < FieldGenerator.MATRIX_STEP; offset++) {
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
                    vector = updateVectorWithField(taxiPosition, vector, fieldPoint, fieldValue);
                }
            }

            if (nonZero && offset > range) {
                break;
            }
        }

        return vector;
    }

    private Vector2D updateVectorWithPB(Point taxiPosition, Vector2D vector, PositionBroadcast pb) {
        Vector2D diff = new Vector2D(pb.getPosition().x - taxiPosition.x, pb.getPosition().y - taxiPosition.y);
        return vector.add(-1.0 * CAPACITY_WEIGHT * pb.getFreeCapacity() * (1 - Math.min(1, Point.distance(taxiPosition, pb.getPosition()) / Taxi.FIELD_INFLUENCE_RANGE)), diff);
    }

    private Vector2D updateVectorWithField(Point taxiPosition, Vector2D vector, Point fieldPoint, double fieldValue) {
        Vector2D diff = new Vector2D(fieldPoint.x - taxiPosition.x, fieldPoint.y - taxiPosition.y);
        return vector.add(fieldValue / Point.distance(taxiPosition, fieldPoint), diff);
    }
}
