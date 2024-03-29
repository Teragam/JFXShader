package de.teragam.jfxshader.effect;

import java.util.LinkedHashMap;
import java.util.Map;

import com.sun.javafx.geom.Point2D;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.scenario.effect.Blend;
import com.sun.scenario.effect.Effect;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.ImageData;
import com.sun.scenario.effect.impl.EffectPeer;
import com.sun.scenario.effect.impl.state.RenderState;

import de.teragam.jfxshader.ShaderController;
import de.teragam.jfxshader.effect.internal.ShaderEffectBase;
import de.teragam.jfxshader.util.Reflect;

public class InternalEffect extends Blend {

    private final int maxInputs;
    private final ShaderEffect effect;
    private final Map<String, EffectPeer<? super RenderState>> peerMap;

    public InternalEffect(ShaderEffect effect, int inputs) {
        super(Mode.SRC_OVER, null, null);
        this.effect = effect;
        this.peerMap = new LinkedHashMap<>();
        this.maxInputs = inputs;
        for (int i = 0; i < inputs; i++) {
            this.setInput(i, null);
        }
    }

    @Override
    public ImageData filter(FilterContext fctx, BaseTransform transform, Rectangle outputClip, Object renderHelper, Effect defaultInput) {
        ShaderController.register(fctx, this.effect);
        return super.filter(fctx, transform, outputClip, renderHelper, defaultInput);
    }

    @Override
    public ImageData filterImageDatas(FilterContext fctx, BaseTransform transform, Rectangle outputClip, RenderState rstate, ImageData... inputs) {
        return ShaderController.getEffectRenderer(this.getEffect()).render(this, fctx, transform, outputClip, rstate, inputs);
    }

    public ShaderEffect getEffect() {
        return this.effect;
    }

    @Override
    public void setInput(int index, Effect input) {
        if (index < this.maxInputs) {
            super.setInput(index, input);
        }
    }

    @Override
    public RenderState getRenderState(FilterContext fctx, BaseTransform transform, Rectangle outputClip, Object renderHelper, Effect defaultInput) {
        return this.effect.getRenderState();
    }

    @Override
    public boolean reducesOpaquePixels() {
        boolean allReduces = true;
        for (final Effect input : this.getInputs()) {
            if (input == null || !input.reducesOpaquePixels()) {
                allReduces = false;
                break;
            }
        }
        return allReduces;
    }

    @Override
    public Point2D transform(Point2D p, Effect defaultInput) {
        final Effect input = Reflect.on(Effect.class).<Effect>method("getDefaultedInput", int.class, Effect.class).invoke(this, 0, defaultInput);
        return input.transform(p, defaultInput);
    }

    @Override
    public Point2D untransform(Point2D p, Effect defaultInput) {
        final Effect input = Reflect.on(Effect.class).<Effect>method("getDefaultedInput", int.class, Effect.class).invoke(this, 0, defaultInput);
        return input.untransform(p, defaultInput);
    }

    @Override
    public void setBottomInput(Effect bottomInput) {
        // no-op
    }

    @Override
    public void setTopInput(Effect topInput) {
        // no-op
    }

    /**
     * This override is necessary to detect shader updates, because {@link javafx.scene.effect.Effect#update() Effect.update()} cannot be overridden when
     * using the java module system.
     * <p>
     * setMode() is called by {@link javafx.scene.effect.Blend#update() Blend.update()}, which is the chosen parent of {@link ShaderEffectBase}.
     */
    @Override
    public void setMode(Mode mode) {
        if (this.effect != null) {
            ((ShaderEffectBase) this.effect.getFXEffect()).updateInputs();
        }
    }

    public Map<String, EffectPeer<? super RenderState>> getPeerMap() {
        return this.peerMap;
    }

}
