package de.teragam.jfxshader.samples.effects;

import java.util.Map;

import com.sun.prism.ps.Shader;

import de.teragam.jfxshader.ShaderDeclaration;
import de.teragam.jfxshader.effect.EffectPeer;
import de.teragam.jfxshader.effect.ShaderEffectPeer;
import de.teragam.jfxshader.effect.ShaderEffectPeerConfig;

@EffectPeer(value = "ZoomRadialBlur")
class ZoomRadialBlurEffectPeer extends ShaderEffectPeer<ZoomRadialBlur> {

    protected ZoomRadialBlurEffectPeer(ShaderEffectPeerConfig config) {
        super(config);
    }

    @Override
    protected ShaderDeclaration createShaderDeclaration() {
        final Map<String, Integer> samplers = Map.of("baseImg", 0);
        final Map<String, Integer> params = Map.of("resolution", 0, "center", 1, "viewport", 2, "texCoords", 3, "blurSteps", 4, "strength", 5);
        return new ShaderDeclaration(samplers, params,
                ZoomRadialBlurEffectPeer.class.getResourceAsStream("/samples/effects/zoomradialblur/zoomradialblur.frag"),
                ZoomRadialBlurEffectPeer.class.getResourceAsStream("/samples/effects/zoomradialblur/zoomradialblur.obj"));
    }

    @Override
    protected void updateShader(Shader shader, ZoomRadialBlur effect) {
        // TODO: clamp image to resolution
        shader.setConstant("resolution", (float) this.getDestBounds().width, (float) this.getDestBounds().height);
        shader.setConstant("center", (float) effect.getCenterX(), (float) effect.getCenterY());
        shader.setConstant("viewport", (float) this.getTransform().getMxt(), (float) this.getTransform().getMyt(), (float) this.getTransform().getMxx(),
                (float) this.getTransform().getMyy());
        final float[] texCoords = this.getTextureCoords(0);
        shader.setConstant("texCoords", texCoords[0], texCoords[1], texCoords[2], texCoords[3]);
        shader.setConstant("blurSteps", Math.max(Math.min(effect.getBlurSteps(), 64), 1));
        shader.setConstant("strength", (float) effect.getStrength());
    }

}
