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
        discreteField = df;
    }

    static Builder builder() {
        return new AutoValue_DiscreteFieldRenderer_Builder();
    }

    @Override
    public void renderStatic(GC gc, ViewPort vp) {
    }

    @Override
    public void renderDynamic(GC gc, ViewPort vp, long time) {
        int frameIndex = this.discreteField.getFrameIndexForTime(time);
        for (int x = 0; x < this.discreteField.getXDimension(); x++) {
            for (int y = 0; y < this.discreteField.getYDimension(); y++) {
                double fieldVal = this.discreteField.getValue(frameIndex, x, y);
                gc.setBackground(new Color(gc.getDevice(), new RGB((float) (255 * fieldVal), (float) (255 * (1 - fieldVal)), 0f)));
                gc.drawRectangle(vp.toCoordX(x), vp.toCoordX(x + 1), vp.toCoordY(y), vp.toCoordY(y + 1));
            }
        }
    }

    @AutoValue
    abstract static class Builder extends AbstractModelBuilder<DiscreteFieldRenderer, Void> {

        Builder() {
            setDependencies(DiscreteField.class);
        }

        @Override
        public DiscreteFieldRenderer build(DependencyProvider dependencyProvider) {
            final DiscreteField df = dependencyProvider.get(DiscreteField.class);
            return new DiscreteFieldRenderer(df);
        }
    }
}
