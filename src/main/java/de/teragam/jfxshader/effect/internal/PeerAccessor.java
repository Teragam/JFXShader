package de.teragam.jfxshader.effect.internal;

import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.scenario.effect.impl.state.RenderState;

import de.teragam.jfxshader.util.ReflectProxy;

public interface PeerAccessor<T extends RenderState> extends ReflectProxy {
    T getRenderState();

    Rectangle getInputBounds(int inputIndex);

    BaseTransform getInputTransform(int inputIndex);

    Rectangle getInputNativeBounds(int inputIndex);

    float[] getSourceRegion(int inputIndex);

    Rectangle getDestBounds();

    Rectangle getDestNativeBounds();

    Object getSamplerData(int i);

}
