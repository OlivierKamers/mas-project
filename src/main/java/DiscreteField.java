import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.google.auto.value.AutoValue;

import java.io.Serializable;
import java.time.Duration;

public class DiscreteField {
    double[][][] fieldData;
    int tDim;
    int xDim;
    int yDim;
    Duration durationPerFrame;

    public DiscreteField(double[][][] data, Duration durationPerFrame) {
        this.fieldData = data;
        this.tDim = fieldData.length;
        this.xDim = fieldData[0].length;
        this.yDim = fieldData[0][0].length;
        this.durationPerFrame = durationPerFrame;
    }

    static Builder builder() {
        return new AutoValue_DiscreteField_Builder();
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
        return Math.min(this.getTDimension(), (int) Math.floor(time / durationPerFrame.toMillis() / 1000));
    }

    public double getValue(int tDim, int xDim, int yDim) {
        return this.fieldData[tDim][xDim][yDim];
    }

    @AutoValue
    abstract static class Builder implements Serializable {

        private static final long serialVersionUID = 4464819196521333418L;

        Builder() {
        }

        public DiscreteField build(DependencyProvider dependencyProvider) {
            FieldGenerator f = new FieldGenerator();
            return f.load();
        }
    }
}
