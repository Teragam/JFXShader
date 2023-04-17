package de.teragam.jfxshader.internal;

import javafx.scene.effect.ShaderEffectBase;

import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.ImageData;
import com.sun.scenario.effect.impl.state.RenderState;

public class InternalEffect extends InternalCoreEffectBase {

    public InternalEffect(ShaderEffectBase effect, int inputs) {
        super(effect, inputs);
    }

    @Override
    public ImageData filterImageDatas(FilterContext fctx, BaseTransform transform, Rectangle outputClip, RenderState rstate, ImageData... inputs) {
        return ShaderController.getEffectRenderer(this.getEffect()).render(this, fctx, transform, outputClip, rstate, inputs);
    }
}
