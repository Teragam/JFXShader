package com.sun.scenario.effect;

import javafx.scene.effect.ShaderEffectBase;

import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.scenario.effect.impl.state.RenderState;

import de.teragam.jfxshader.internal.ShaderController;

public class InternalCoreEffectBase extends CoreEffect<RenderState> {

    private final int maxInputs;
    private final ShaderEffectBase effect;

    public InternalCoreEffectBase(ShaderEffectBase effect, int inputs) {
        super(null, null);
        this.effect = effect;
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
        return ShaderController.getEffectRenderer(this.effect).render(this, fctx, transform, outputClip, rstate, inputs);
    }

    public ShaderEffectBase getEffect() {
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

}
