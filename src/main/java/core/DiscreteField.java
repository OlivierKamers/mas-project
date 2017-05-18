package core;

import com.github.rinde.rinsim.geom.Point;

import java.time.Duration;

public class DiscreteField {
    private double[][][] fieldData;
    private int tDim;
    private int xDim;
    private int yDim;
    private Duration durationPerFrame;

    public DiscreteField(double[][][] data, Duration durationPerFrame) {
        this.fieldData = data;
        this.tDim = fieldData.length;
        this.xDim = fieldData[0].length;
        this.yDim = fieldData[0][0].length;
        this.durationPerFrame = durationPerFrame;
    }

    public DiscreteField() {
        this.fieldData = new double[0][0][0];
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

    public int[] convertMapToFieldCoordinates(Point p) {
        int xBin = (int) Math.floor(p.x / Helper.ROADMODEL_BOUNDARIES_SCALE / Helper.getXScale() * xDim);
        int yBin = (int) Math.floor(p.y / Helper.ROADMODEL_BOUNDARIES_SCALE / Helper.getYScale() * yDim);
        return new int[]{xBin, yBin};
    }

    // Middle of square
    public Point convertFieldToMapCoordinates(int xBin, int yBin) {
        double x = (xBin + 0.5) * Helper.ROADMODEL_BOUNDARIES_SCALE * Helper.getXScale() / xDim;
        double y = (yBin + 0.5) * Helper.ROADMODEL_BOUNDARIES_SCALE * Helper.getYScale() / yDim;
        return new Point(x, y);
    }

    public Point getNextPosition(Taxi taxi, long time, int range) {
        int t = getFrameIndexForTime(time);
        int[] pos = convertMapToFieldCoordinates(taxi.getPosition().get());
        int xPos = pos[0];
        int yPos = pos[1];

        double maxField = Double.MIN_VALUE;
        Point maxPoint = taxi.getPosition().get();
        for (int x = Math.max(0, xPos - range); x <= Math.min(xPos + range, xDim - 1); x++) {
            for (int y = Math.max(0, yPos - range); y <= Math.min(yPos + range, yDim - 1); y++) {
                //TODO geen vierkant veld
                double curVal = getValue(t, x, y);
                if (curVal > maxField) {
                    maxPoint = convertFieldToMapCoordinates(x, y);
                    maxField = curVal;
                }
            }
        }
        return maxPoint;
    }
}
