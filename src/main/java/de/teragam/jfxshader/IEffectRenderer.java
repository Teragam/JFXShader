package de.teragam.jfxshader;

import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.scenario.effect.Effect;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.ImageData;
import com.sun.scenario.effect.impl.state.RenderState;

public interface IEffectRenderer {

    ImageData render(Effect effect, FilterContext fctx, BaseTransform transform, Rectangle outputClip, RenderState rstate, ImageData... inputs);

}
