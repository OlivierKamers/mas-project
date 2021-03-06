package core;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

class FieldGenerator {
    private static final int DEFAULT_MATRIX_STEP = 100;
    private static double FIELD_INFLUENCE = 0.5;
    private double[][][] field;
    private double[] maxFieldValues;
    private int xDim;
    private int yDim;
    private int matrixStep;
    private int timeStep;

    FieldGenerator(int matrixStep, int minPerFrame) {
        this.matrixStep = matrixStep == 0 ? DEFAULT_MATRIX_STEP : matrixStep;
        this.xDim = (int) (this.matrixStep * Helper.getXScale());
        this.yDim = (int) (this.matrixStep * Helper.getYScale());
        this.timeStep = (int) (Duration.between(Helper.START_TIME, Helper.STOP_TIME).getSeconds() / 60.0 / minPerFrame);
        this.field = new double[this.timeStep][this.xDim][this.yDim];
        this.maxFieldValues = new double[this.timeStep];
    }

    private int getMatrixStep() {
        return matrixStep;
    }

    DiscreteField load(double taxiInfluenceRange) {
        MySQLDataLoader loader = new MySQLDataLoader();
        Duration timeDuration = Duration.between(Helper.START_TIME, Helper.STOP_TIME).dividedBy(this.timeStep);
        LocalDateTime curTime = Helper.START_TIME.minus(Helper.FIELD_TIME_OFFSET);
        for (int i = 0; i < this.timeStep; i++) {
            this.field[i] = parseData(loader.read(curTime, curTime.plus(timeDuration)));
            curTime = curTime.plus(timeDuration);
        }
        smooth();
        findMaxValues();
        return new DiscreteField(this.field, this.maxFieldValues, timeDuration, getMatrixStep(), taxiInfluenceRange);
    }

    private double[][] parseData(List<HistoricalData> data) {
        double[][] fieldFrame = new double[this.xDim][this.yDim];
        double max = 0;
        for (HistoricalData h : data) {
            int xBin = (int) Math.floor(h.getPickupPoint().x / Helper.ROADMODEL_BOUNDARIES_SCALE / Helper.getXScale() * xDim);
            int yBin = (int) Math.floor(h.getPickupPoint().y / Helper.ROADMODEL_BOUNDARIES_SCALE / Helper.getYScale() * yDim);
            fieldFrame[xBin][yBin] += 1;
            max = fieldFrame[xBin][yBin] > max ? fieldFrame[xBin][yBin] : max;
        }
        return fieldFrame;
    }

    private void smooth() {
        for (int t = 0; t < this.field.length; t++) {
            for (int x = 0; x < xDim; x++) {
                for (int y = 0; y < yDim; y++) {
                    this.field[t][x][y] += FIELD_INFLUENCE * this.field[Math.max(0, t - 1)][x][y]
                            + FIELD_INFLUENCE / 1 * this.field[Math.min(this.timeStep - 1, t + 1)][x][y]
                            + FIELD_INFLUENCE / 2 * this.field[Math.min(this.timeStep - 1, t + 2)][x][y]
                            + FIELD_INFLUENCE / 4 * this.field[Math.min(this.timeStep - 1, t + 3)][x][y];
                }
            }
        }
    }

    private void findMaxValues() {
        for (int t = 0; t < this.field.length; t++) {
            // Find max
            double max = 0;
            for (int x = 0; x < xDim; x++) {
                for (int y = 0; y < yDim; y++) {
                    if (this.field[t][x][y] > max) {
                        max = this.field[t][x][y];
                    }
                }
            }
            maxFieldValues[t] = max;
        }
    }
}
