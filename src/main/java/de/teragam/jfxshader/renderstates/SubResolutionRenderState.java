package de.teragam.jfxshader.renderstates;

import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.scenario.effect.impl.state.RenderState;

public class SubResolutionRenderState implements RenderState {

    private final double resolutionDividend;

    public SubResolutionRenderState(double resolutionDividend) {
        this.resolutionDividend = resolutionDividend;
    }


    @Override
    public EffectCoordinateSpace getEffectTransformSpace() {
        return EffectCoordinateSpace.CustomSpace;
    }

    @Override
    public BaseTransform getInputTransform(BaseTransform filterTransform) {
        final BaseTransform baseTransform = filterTransform.copy();
        baseTransform.setToIdentity();
        baseTransform.deriveWithScale(1 / this.resolutionDividend, 1 / this.resolutionDividend, 1);
        return baseTransform;
    }

    @Override
    public BaseTransform getResultTransform(BaseTransform filterTransform) {
        return filterTransform.copy().deriveWithScale(this.resolutionDividend, this.resolutionDividend, 1);
    }

    @Override
    public Rectangle getInputClip(int i, Rectangle filterClip) {
        return filterClip;
    }

}
