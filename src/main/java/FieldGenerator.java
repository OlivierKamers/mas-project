import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class FieldGenerator {
    // For testing: matrix with 10 rows
    private static final int MATRIX_STEP = 10;
    // For testing: calculate the field every second (so it should be equal to 1 round of pickups)
    private static final int TIME_STEP = (int) Duration.between(Helper.START_TIME, Helper.STOP_TIME).getSeconds();//50;
    private double[][][] field;
    private int xDim;
    private int yDim;

    public FieldGenerator() {
        this.xDim = (int) (MATRIX_STEP * Helper.getXScale());
        this.yDim = (int) (MATRIX_STEP * Helper.getYScale());
        this.field = new double[TIME_STEP][this.xDim][this.yDim];
    }

    public static void main(String[] args) {
        FieldGenerator f = new FieldGenerator();
        DiscreteField df = f.load();
    }

    public DiscreteField load() {
        MySQLDataLoader loader = new MySQLDataLoader();
        Duration timeDuration = Duration.between(Helper.START_TIME, Helper.STOP_TIME).dividedBy(TIME_STEP);
        LocalDateTime curTime = Helper.START_TIME;
        for (int i = 0; i < TIME_STEP; i++) {
            this.field[i] = parseData(loader.read(curTime, curTime.plus(timeDuration)));
            curTime = curTime.plus(timeDuration);
        }
        return new DiscreteField(this.field, timeDuration);
    }

    private double[][] parseData(List<HistoricalData> data) {
        double[][] fieldFrame = new double[this.xDim][this.yDim];
        double max = 0;
        for (HistoricalData h : data) {
            int xBin = (int) Math.floor(h.getPickupPoint().x / Helper.ROADMODEL_BOUNDARIES_SCALE * xDim);
            int yBin = (int) Math.floor(h.getPickupPoint().y / Helper.ROADMODEL_BOUNDARIES_SCALE * yDim);
            fieldFrame[xBin][yBin] += 1;
            max = fieldFrame[xBin][yBin] > max ? fieldFrame[xBin][yBin] : max;
        }

        //Normalize
        if (max > 0) {
            for (int x = 0; x < xDim; x++) {
                for (int y = 0; y < yDim; y++) {
                    fieldFrame[x][y] /= max;
                }
            }
        }
        return fieldFrame;
    }
}
