package de.teragam.jfxshader.samples.effects.pixelate;


import java.util.Map;

import com.sun.prism.ps.Shader;

import de.teragam.jfxshader.ShaderDeclaration;
import de.teragam.jfxshader.effect.EffectPeer;
import de.teragam.jfxshader.effect.ShaderEffectPeer;
import de.teragam.jfxshader.effect.ShaderEffectPeerConfig;

@EffectPeer("Pixelate")
class PixelateEffectPeer extends ShaderEffectPeer<Pixelate> {

    protected PixelateEffectPeer(ShaderEffectPeerConfig config) {
        super(config);
    }

    @Override
    protected ShaderDeclaration createShaderDeclaration() {
        final Map<String, Integer> samplers = Map.of("baseImg", 0);
        final Map<String, Integer> params = Map.of("pixelSize", 0, "offset", 1, "resolution", 2, "texCoords", 3, "viewport", 4);
        return new ShaderDeclaration(samplers, params, Pixelate.class.getResourceAsStream("/samples/effects/pixelate/pixelate.frag"),
                Pixelate.class.getResourceAsStream("/samples/effects/pixelate/pixelate.obj"));
    }

    @Override
    protected void updateShader(Shader shader, Pixelate effect) {
        shader.setConstant("pixelSize", Math.max((float) effect.getPixelWidth(), 1), Math.max((float) effect.getPixelHeight(), 1));
        shader.setConstant("offset", (float) effect.getOffsetX(), (float) effect.getOffsetY());
        shader.setConstant("resolution", (float) this.getDestNativeBounds().width, (float) this.getDestNativeBounds().height);
        final float[] texCoords = this.getTextureCoords(0);
        shader.setConstant("texCoords", texCoords[0], texCoords[1], texCoords[2], texCoords[3]);
        shader.setConstant("viewport", (float) this.getTransform().getMxt(), (float) this.getTransform().getMyt(), (float) this.getTransform().getMxx(),
                (float) this.getTransform().getMyy());
    }

}
