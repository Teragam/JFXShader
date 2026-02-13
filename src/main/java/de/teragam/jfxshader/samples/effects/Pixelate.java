package de.teragam.jfxshader.samples.effects;

import javafx.beans.property.DoubleProperty;

import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.scenario.effect.Effect;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.ImageData;
import com.sun.scenario.effect.impl.prism.PrRenderInfo;
import com.sun.scenario.effect.impl.state.RenderState;

import de.teragam.jfxshader.effect.EffectDependencies;
import de.teragam.jfxshader.effect.OneSamplerEffect;

@EffectDependencies(PixelateEffectPeer.class)
public class Pixelate extends OneSamplerEffect {

    private final DoubleProperty pixelWidth;
    private final DoubleProperty pixelHeight;

    public Pixelate() {
        this(10, 10);
    }

    public Pixelate(double pixelWidth, double pixelHeight) {
        this.pixelWidth = super.createEffectDoubleProperty(pixelWidth, "pixelWidth");
        this.pixelHeight = super.createEffectDoubleProperty(pixelHeight, "pixelHeight");
    }

    public double getPixelWidth() {
        return this.pixelWidth.get();
    }

    public DoubleProperty pixelWidthProperty() {
        return this.pixelWidth;
    }

    public void setPixelWidth(double pixelWidth) {
        this.pixelWidth.set(pixelWidth);
    }

    public double getPixelHeight() {
        return this.pixelHeight.get();
    }

    public DoubleProperty pixelHeightProperty() {
        return this.pixelHeight;
    }

    public void setPixelHeight(double pixelHeight) {
        this.pixelHeight.set(pixelHeight);
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
                final float scaleX = (float) Math.hypot(transform.getMxx(), transform.getMyx());
                final float scaleY = (float) Math.hypot(transform.getMxy(), transform.getMyy());
                final double scaledPixelWidth = Math.max(Pixelate.this.pixelWidth.get(), 1.0) * scaleX;
                final double scaledPixelHeight = Math.max(Pixelate.this.pixelHeight.get(), 1.0) * scaleY;
                rectangle.grow((int) Math.ceil(scaledPixelWidth / 2.0), (int) Math.ceil(scaledPixelHeight / 2.0));
                return rectangle;
            }
        };
    }


    @Override
    public Rectangle getResultBounds(BaseTransform transform, Rectangle outputClip, ImageData... inputDatas) {
        final float scaleX = (float) Math.hypot(transform.getMxx(), transform.getMyx());
        final float scaleY = (float) Math.hypot(transform.getMxy(), transform.getMyy());
        final double scaledPixelWidth = Math.max(this.pixelWidth.get(), 1.0) * scaleX;
        final double scaledPixelHeight = Math.max(this.pixelHeight.get(), 1.0) * scaleY;
        outputClip.grow(-(int) Math.ceil(scaledPixelWidth / 2.0), -(int) Math.ceil(scaledPixelHeight / 2.0));
        return outputClip;
    }

}
