package de.teragam.jfxshader.effect;

import java.util.Arrays;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.effect.Effect;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.DirtyRegionContainer;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.ImageData;
import com.sun.scenario.effect.impl.prism.PrRenderInfo;
import com.sun.scenario.effect.impl.state.RenderState;

import de.teragam.jfxshader.ShaderController;
import de.teragam.jfxshader.effect.internal.DefaultEffectRenderer;
import de.teragam.jfxshader.effect.internal.ShaderEffectBase;

@EffectRenderer(DefaultEffectRenderer.class)
public abstract class ShaderEffect {

    private final ShaderEffectBase effectBase;
    private final int inputs;

    protected ShaderEffect(int inputs) {
        ShaderController.injectEffectAccessor();
        this.inputs = inputs;
        this.effectBase = new ShaderEffectBase(this, inputs);
    }

    public Effect getFXEffect() {
        return this.effectBase;
    }

    public ShaderEffect copy() {
        throw new UnsupportedOperationException("No copy functionality specified");
    }

    public void markDirty() {
        this.effectBase.markDirty();
    }

    public int getInputs() {
        return this.inputs;
    }

    public RenderState getRenderState(FilterContext fctx, BaseTransform transform, Rectangle outputClip, PrRenderInfo renderHelper,
                                      com.sun.scenario.effect.Effect defaultInput) {
        return RenderState.RenderSpaceRenderState;
    }

    public BaseBounds getBounds(BaseBounds inputBounds) {
        return inputBounds;
    }

    public Rectangle getResultBounds(BaseTransform transform, Rectangle outputClip, ImageData... inputDatas) {
        return Arrays.stream(inputDatas)
                .map(imageData -> new Rectangle(imageData.getTransformedBounds(outputClip)))
                .filter(rect -> !rect.isEmpty())
                .reduce((rectangle, rectangle2) -> {
                    rectangle.add(rectangle2);
                    return rectangle;
                }).orElse(new Rectangle());
    }

    public void writeDirtyRegions(BaseBounds inputBounds, DirtyRegionContainer drc) {
        // NO-OP. By default, the dirty regions contain the altered scene graph nodes.
    }

    protected BaseBounds getInputBounds(int index, BaseTransform transform, com.sun.scenario.effect.Effect defaultInput) {
        return this.effectBase.getInternalPeer().getDefaultedInput(index, defaultInput).getBounds(transform, defaultInput);
    }

    protected ObjectProperty<Effect> getInputProperty(int index) {
        if (this.inputs > 0 && index == 0) {
            return this.effectBase.topInputProperty();
        } else if (this.inputs > 1 && index == 1) {
            return this.effectBase.bottomInputProperty();
        } else {
            throw new IllegalArgumentException(String.format("Only indexes 0 to %d are supported. Requested was %d.", this.inputs - 1, index));
        }
    }

    protected void setContinuousRendering(boolean continuousRendering) {
        this.effectBase.setContinuousRendering(continuousRendering);
    }

    protected DoubleProperty createEffectDoubleProperty(double value, String name) {
        return new SimpleDoubleProperty(ShaderEffect.this, name, value) {

            @Override
            public void invalidated() {
                ShaderEffect.this.markDirty();
            }

        };
    }

    protected IntegerProperty createEffectIntegerProperty(int value, String name) {
        return new SimpleIntegerProperty(ShaderEffect.this, name, value) {

            @Override
            public void invalidated() {
                ShaderEffect.this.markDirty();
            }

        };
    }

    protected BooleanProperty createEffectBooleanProperty(boolean value, String name) {
        return new SimpleBooleanProperty(ShaderEffect.this, name, value) {

            @Override
            public void invalidated() {
                ShaderEffect.this.markDirty();
            }

        };
    }

    protected <T> ObjectProperty<T> createEffectObjectProperty(T value, String name) {
        return new SimpleObjectProperty<>(ShaderEffect.this, name, value) {

            @Override
            public void invalidated() {
                ShaderEffect.this.markDirty();
            }

        };
    }

    protected static Rectangle untransformClip(BaseTransform transform, Rectangle clip) {
        return InternalEffect.untransformClip(transform, clip);
    }

}
