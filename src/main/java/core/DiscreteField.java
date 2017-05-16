package core;

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
}
