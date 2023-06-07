package de.teragam.jfxshader.effect;

import com.sun.scenario.effect.impl.state.RenderState;

import de.teragam.jfxshader.effect.internal.PPSMultiSamplerPeer;

public abstract class ShaderEffectPeer<T extends ShaderEffect> extends PPSMultiSamplerPeer<RenderState, T> {

    protected ShaderEffectPeer(ShaderEffectPeerConfig config) {
        super(config);
    }

}
