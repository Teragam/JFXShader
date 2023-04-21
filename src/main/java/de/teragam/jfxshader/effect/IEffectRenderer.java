package de.teragam.jfxshader.effect;

import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.ImageData;
import com.sun.scenario.effect.impl.state.RenderState;

import de.teragam.jfxshader.effect.internal.InternalEffect;

public interface IEffectRenderer {

    ImageData render(InternalEffect effect, FilterContext fctx, BaseTransform transform, Rectangle outputClip, RenderState rstate, ImageData... inputs);

}
