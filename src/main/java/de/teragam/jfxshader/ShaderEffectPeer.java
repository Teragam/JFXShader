package de.teragam.jfxshader;

import com.sun.scenario.effect.impl.prism.ps.PPSMultiSamplerPeer;
import com.sun.scenario.effect.impl.state.RenderState;

import de.teragam.jfxshader.internal.ShaderEffectPeerConfig;

public abstract class ShaderEffectPeer<T extends ShaderEffect> extends PPSMultiSamplerPeer<RenderState, T> {

    protected ShaderEffectPeer(ShaderEffectPeerConfig config) {
        super(config);
    }

}
