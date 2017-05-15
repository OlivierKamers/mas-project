import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.ui.renderers.CanvasRenderer.AbstractCanvasRenderer;
import com.github.rinde.rinsim.ui.renderers.ViewPort;
import com.google.auto.value.AutoValue;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;

class DiscreteFieldRenderer extends AbstractCanvasRenderer {

    static final double DIAMETER_MUL = 6d;
    static final RGB GREEN = new RGB(0, 255, 0);
    static final RGB RED = new RGB(255, 0, 0);

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
        int frameIndex = this.discreteField.getFrameIndexForTime(time);
        System.out.println(time);
        System.out.println(frameIndex);
        for (int x = 0; x < this.discreteField.getXDimension(); x++) {
            for (int y = 0; y < this.discreteField.getYDimension(); y++) {
                double fieldVal = this.discreteField.getValue(frameIndex, x, y);
                RGB rgb = new RGB((int) (255 * fieldVal), (int) (255 * (1 - fieldVal)), 0);
                gc.setBackground(new Color(gc.getDevice(), rgb));
                gc.fillRectangle(vp.toCoordX((double) (x) / this.discreteField.getXDimension() * Helper.ROADMODEL_BOUNDARIES_SCALE * Helper.getXScale()),
                        vp.toCoordY((double) (y) / this.discreteField.getYDimension() * Helper.ROADMODEL_BOUNDARIES_SCALE * Helper.getYScale()),
                        vp.scale((double)(Helper.ROADMODEL_BOUNDARIES_SCALE)/this.discreteField.getXDimension()),
                        vp.scale((double)(Helper.ROADMODEL_BOUNDARIES_SCALE)/this.discreteField.getYDimension()));
            }
        }
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
//            final DiscreteField df = dependencyProvider.get(DiscreteField.class);
            return new DiscreteFieldRenderer(df());
        }

        public Builder withField(DiscreteField df) {
            return create(df);
        }

    }
}
