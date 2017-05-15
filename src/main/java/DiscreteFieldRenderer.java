import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.ui.renderers.CanvasRenderer.AbstractCanvasRenderer;
import com.github.rinde.rinsim.ui.renderers.ViewPort;
import com.google.auto.value.AutoValue;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.jetbrains.annotations.NotNull;

class DiscreteFieldRenderer extends AbstractCanvasRenderer {

    static final RGB COLOR_LOW = new RGB(0, 255, 255);
    static final RGB COLOR_HIGH = new RGB(255, 0, 0);

    DiscreteField discreteField;

    DiscreteFieldRenderer(DiscreteField df) {
        this.discreteField = df;
    }

    static Builder builder() {
        return new AutoValue_DiscreteFieldRenderer_Builder(new DiscreteField());
    }

    @Override
    public void renderStatic(GC gc, ViewPort vp) {
    }

    @Override
    public void renderDynamic(GC gc, ViewPort vp, long time) {
        int frameIndex = discreteField.getFrameIndexForTime(time);
        for (int x = 0; x < discreteField.getXDimension(); x++) {
            for (int y = 0; y < discreteField.getYDimension(); y++) {
                gc.setBackground(new Color(gc.getDevice(), getRgb(this.discreteField.getValue(frameIndex, x, y))));
                gc.fillRectangle(
                        vp.toCoordX(1.0 * x / this.discreteField.getXDimension() * Helper.ROADMODEL_BOUNDARIES_SCALE * Helper.getXScale()),
                        vp.toCoordY(1.0 * y / this.discreteField.getYDimension() * Helper.ROADMODEL_BOUNDARIES_SCALE * Helper.getYScale()),
                        vp.scale(1.0 * Helper.ROADMODEL_BOUNDARIES_SCALE / this.discreteField.getXDimension()),
                        vp.scale(1.0 * Helper.ROADMODEL_BOUNDARIES_SCALE) / this.discreteField.getYDimension()
                );
            }
        }
    }

    @NotNull
    private RGB getRgb(double fieldVal) {
        int red = (int) (COLOR_HIGH.red * fieldVal + COLOR_LOW.red * (1 - fieldVal));
        int green = (int) (COLOR_HIGH.green * fieldVal + COLOR_LOW.green * (1 - fieldVal));
        int blue = (int) (COLOR_HIGH.blue * fieldVal + COLOR_LOW.blue * (1 - fieldVal));
        return new RGB(red, green, blue);
    }

    @AutoValue
    abstract static class Builder extends AbstractModelBuilder<DiscreteFieldRenderer, Void> {

        Builder() {
        }

        static Builder create(DiscreteField df) {
            return new AutoValue_DiscreteFieldRenderer_Builder(df);
        }

        abstract DiscreteField df();

        @Override
        public DiscreteFieldRenderer build(DependencyProvider dependencyProvider) {
            return new DiscreteFieldRenderer(df());
        }

        public Builder withField(DiscreteField df) {
            return create(df);
        }

    }
}
