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

//    static Builder builder() {
//        return new AutoValue_DiscreteField_Builder();
//    }

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

    public double getValue(int tDim, int xDim, int yDim) {
        return this.fieldData[tDim][xDim][yDim];
    }

//    @AutoValue
//    abstract static class Builder implements Serializable {
//
//        private static final long serialVersionUID = 4464819196521333418L;
//
//        Builder() {
//        }
//
//        public DiscreteField build(DependencyProvider dependencyProvider) {
//
//        }
//    }
}
