package de.teragam.jfxshader.samples.effects;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.DirtyRegionContainer;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.scenario.effect.Effect;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.ImageData;
import com.sun.scenario.effect.impl.prism.PrRenderInfo;
import com.sun.scenario.effect.impl.state.RenderState;

import de.teragam.jfxshader.effect.EffectDependencies;
import de.teragam.jfxshader.effect.OneSamplerEffect;
import de.teragam.jfxshader.effect.ShaderEffect;

@EffectDependencies(ZoomRadialBlurEffectPeer.class)
public class ZoomRadialBlur extends OneSamplerEffect {

    private final DoubleProperty strength;
    private final IntegerProperty blurSteps;
    private final DoubleProperty centerX;
    private final DoubleProperty centerY;

    public ZoomRadialBlur() {
        this(100.0);
    }

    public ZoomRadialBlur(double strength) {
        this(strength, 16);
    }

    public ZoomRadialBlur(double strength, int blurSteps) {
        this.strength = super.createEffectDoubleProperty(strength, "strength");
        this.blurSteps = super.createEffectIntegerProperty(blurSteps, "blurSteps");
        this.centerX = super.createEffectDoubleProperty(0.0, "centerX");
        this.centerY = super.createEffectDoubleProperty(0.0, "centerY");
    }

    @Override
    public RenderState getRenderState(FilterContext fctx, BaseTransform transform, Rectangle outputClip, PrRenderInfo renderHelper, Effect defaultInput) {
        return new RenderState() {
            @Override
            public EffectCoordinateSpace getEffectTransformSpace() {
                return EffectCoordinateSpace.CustomSpace;
            }

            @Override
            public BaseTransform getInputTransform(BaseTransform baseTransform) {
                return baseTransform;
            }

            @Override
            public BaseTransform getResultTransform(BaseTransform baseTransform) {
                return BaseTransform.IDENTITY_TRANSFORM;
            }

            @Override
            public Rectangle getInputClip(int i, Rectangle rectangle) {
                final BaseBounds inputBounds = ZoomRadialBlur.super.getInputBounds(0, BaseTransform.IDENTITY_TRANSFORM, defaultInput);
                final Rectangle untransformedClip = ShaderEffect.untransformClip(transform, outputClip);
                final BaseBounds bounds = ZoomRadialBlur.this.calcInfluenceBounds(inputBounds, new RectBounds(untransformedClip));
                transform.transform(bounds, bounds);
                bounds.roundOut();
                return new Rectangle((int) bounds.getMinX(), (int) bounds.getMinY(), (int) bounds.getWidth(), (int) bounds.getHeight());
            }
        };
    }

    @Override
    public BaseBounds getBounds(BaseBounds inputBounds) {
        return inputBounds.deriveWithNewBounds(this.calcBounds(inputBounds, inputBounds));
    }

    @Override
    public Rectangle getResultBounds(BaseTransform transform, Rectangle outputClip, ImageData... inputDatas) {
        return outputClip;
    }

    @Override
    public void writeDirtyRegions(BaseBounds inputBounds, DirtyRegionContainer drc) {
        for (int i = 0; i < drc.size(); i++) {
            final RectBounds region = drc.getDirtyRegion(i);
            if (region == null || region.isEmpty()) {
                break;
            }
            region.deriveWithNewBounds(this.calcBounds(inputBounds, region));
        }
    }

    private RectBounds calcBounds(BaseBounds inputBounds, BaseBounds region) {
        final double calcStrength = this.strength.get() * (1.0 / Math.max(inputBounds.getWidth(), inputBounds.getHeight()));
        final float maxOffset = (float) (calcStrength * (this.getBlurSteps() - 1) / this.getBlurSteps());
        final float minXOffset = (float) ((region.getMinX() - this.getCenterX() - inputBounds.getMinX()) * maxOffset);
        final float minYOffset = (float) ((region.getMinY() - this.getCenterY() - inputBounds.getMinY()) * maxOffset);
        final float maxXOffset = (float) ((region.getMaxX() - this.getCenterX() - inputBounds.getMinX()) * maxOffset);
        final float maxYOffset = (float) ((region.getMaxY() - this.getCenterY() - inputBounds.getMinY()) * maxOffset);
        final float newMinX = (float) (region.getMinX() + Math.min(minXOffset / (1.0 - maxOffset), 0));
        final float newMinY = (float) (region.getMinY() + Math.min(minYOffset / (1.0 - maxOffset), 0));
        final float newMaxX = (float) (region.getMaxX() + Math.max(maxXOffset / (1.0 - maxOffset), 0));
        final float newMaxY = (float) (region.getMaxY() + Math.max(maxYOffset / (1.0 - maxOffset), 0));
        return new RectBounds(newMinX, newMinY, newMaxX, newMaxY);
    }

    private RectBounds calcInfluenceBounds(BaseBounds inputBounds, BaseBounds region) {
        final double calcStrength = this.strength.get() * (1.0 / Math.max(inputBounds.getWidth(), inputBounds.getHeight()));
        final float maxOffset = (float) (calcStrength * (this.getBlurSteps() - 1) / this.getBlurSteps());
        final float newMinX = (float) (region.getMinX() * (-maxOffset) + region.getMinX() + maxOffset * (this.getCenterX() + inputBounds.getMinX()));
        final float newMinY = (float) (region.getMinY() * (-maxOffset) + region.getMinY() + maxOffset * (this.getCenterY() + inputBounds.getMinY()));
        final float newMaxX = (float) (region.getMaxX() * (-maxOffset) + region.getMaxX() + maxOffset * (this.getCenterX() + inputBounds.getMinX()));
        final float newMaxY = (float) (region.getMaxY() * (-maxOffset) + region.getMaxY() + maxOffset * (this.getCenterY() + inputBounds.getMinY()));
        final BaseBounds result = new RectBounds(newMinX, newMinY, newMaxX, newMaxY).deriveWithUnion(region);
        final BaseBounds totalRegion = this.calcBounds(inputBounds, inputBounds);
        result.intersectWith(totalRegion);
        return result.flattenInto(null);
    }

    public double getStrength() {
        return this.strength.get();
    }

    public DoubleProperty strengthProperty() {
        return this.strength;
    }

    public void setStrength(double strength) {
        this.strength.set(strength);
    }

    public int getBlurSteps() {
        return this.blurSteps.get();
    }

    public IntegerProperty blurStepsProperty() {
        return this.blurSteps;
    }

    public void setBlurSteps(int blurSteps) {
        this.blurSteps.set(blurSteps);
    }

    public double getCenterX() {
        return this.centerX.get();
    }

    public DoubleProperty centerXProperty() {
        return this.centerX;
    }

    public void setCenterX(double centerX) {
        this.centerX.set(centerX);
    }

    public double getCenterY() {
        return this.centerY.get();
    }

    public DoubleProperty centerYProperty() {
        return this.centerY;
    }

    public void setCenterY(double centerY) {
        this.centerY.set(centerY);
    }

}
