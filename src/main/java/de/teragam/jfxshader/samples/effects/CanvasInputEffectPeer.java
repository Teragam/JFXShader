package de.teragam.jfxshader.samples.effects;

import java.util.Map;

import de.teragam.jfxshader.JFXShader;
import de.teragam.jfxshader.ShaderDeclaration;
import de.teragam.jfxshader.effect.EffectPeer;
import de.teragam.jfxshader.effect.ShaderEffectPeer;
import de.teragam.jfxshader.effect.ShaderEffectPeerConfig;

@EffectPeer("CanvasInput")
class CanvasInputEffectPeer extends ShaderEffectPeer<CanvasInput> {

    protected CanvasInputEffectPeer(ShaderEffectPeerConfig config) {
        super(config);
    }

    @Override
    protected ShaderDeclaration createShaderDeclaration() {
        final Map<String, Integer> samplers = Map.of("baseImg", 0);
        final Map<String, Integer> params = Map.of();
        return new ShaderDeclaration(samplers, params,
                CanvasInput.class.getResourceAsStream("/de/teragam/jfxshader/samples/effects/canvasinput/canvasinput.frag"),
                CanvasInput.class.getResourceAsStream("/de/teragam/jfxshader/samples/effects/canvasinput/canvasinput.obj"));
    }

    @Override
    protected void updateShader(JFXShader shader, CanvasInput effect) {
        // No parameters to update
    }

}
