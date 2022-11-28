package de.teragam.jfxshader.internal;

import java.util.List;

import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.scenario.effect.Effect;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.ImageData;
import com.sun.scenario.effect.InternalCoreEffectBase;
import com.sun.scenario.effect.impl.Renderer;
import com.sun.scenario.effect.impl.state.RenderState;

import de.teragam.jfxshader.EffectPeer;
import de.teragam.jfxshader.IEffectRenderer;
import de.teragam.jfxshader.ShaderEffectPeer;

@SuppressWarnings("unchecked")
public class DefaultEffectRenderer implements IEffectRenderer {

    @Override
    public ImageData render(Effect effect, FilterContext fctx, BaseTransform transform, Rectangle outputClip, RenderState rstate, ImageData... inputs) {
        if (effect instanceof InternalCoreEffectBase) {
            final List<Class<? extends ShaderEffectPeer<?>>> peerList = ShaderController.getPeerDependencies(
                    ((InternalCoreEffectBase) effect).getEffect().getClass());
            if (!peerList.isEmpty()) {
                return Renderer.getRenderer(fctx).getPeerInstance(fctx, peerList.get(0).getAnnotation(EffectPeer.class).value(), -1)
                        .filter(effect, rstate, transform, outputClip, inputs);
            }
        }
        return new ImageData(fctx, null, null);
    }

}
