package de.teragam.jfxshader.effect.internal;

import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.ImageData;
import com.sun.scenario.effect.impl.state.RenderState;

import de.teragam.jfxshader.effect.IEffectRenderer;
import de.teragam.jfxshader.effect.InternalEffect;

public class DefaultEffectRenderer implements IEffectRenderer {

    @Override
    public ImageData render(InternalEffect effect, FilterContext fctx, BaseTransform transform, Rectangle outputClip, RenderState rstate, ImageData... inputs) {
        return effect.getPeerMap().values().stream().findFirst()
                .map(peer -> peer.filter(effect, rstate, transform, outputClip, inputs))
                .orElse(new ImageData(fctx, null, null));
    }

}
