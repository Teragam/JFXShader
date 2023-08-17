package de.teragam.jfxshader.samples.effects;


import java.util.Map;

import de.teragam.jfxshader.JFXShader;
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
        return new ShaderDeclaration(samplers, params, Pixelate.class.getResourceAsStream("/de/teragam/jfxshader/samples/effects/pixelate/pixelate.frag"),
                Pixelate.class.getResourceAsStream("/de/teragam/jfxshader/samples/effects/pixelate/pixelate.obj"));
    }

    @Override
    protected void updateShader(JFXShader shader, Pixelate effect) {
        shader.setConstant("pixelSize", Math.max((float) effect.getPixelWidth(), 1), Math.max((float) effect.getPixelHeight(), 1));
        shader.setConstant("offset", (float) effect.getOffsetX(), (float) effect.getOffsetY());
        shader.setConstant("resolution", (float) this.getDestNativeBounds().width, (float) this.getDestNativeBounds().height);
        final float[] texCoords = this.getTextureCoords(0);
        shader.setConstant("texCoords", texCoords[0], texCoords[1], texCoords[2], texCoords[3]);
        final double scaleX = Math.hypot(this.getTransform().getMxx(), this.getTransform().getMyx());
        final double scaleY = Math.hypot(this.getTransform().getMxy(), this.getTransform().getMyy());
        shader.setConstant("viewport", 0f, 0f, (float) scaleX, (float) scaleY);
    }

}
