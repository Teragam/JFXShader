package de.teragam.jfxshader.samples.effects;


import java.util.Map;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.Rectangle;

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
        final Map<String, Integer> params = Map.of("pixelSize", 0, "resolution", 1, "viewport", 2);
        return new ShaderDeclaration(samplers, params, Pixelate.class.getResourceAsStream("/de/teragam/jfxshader/samples/effects/pixelate/pixelate.frag"),
                Pixelate.class.getResourceAsStream("/de/teragam/jfxshader/samples/effects/pixelate/pixelate.obj"));
    }

    @Override
    protected void updateShader(JFXShader shader, Pixelate effect) {
        final BaseBounds contentBounds = super.getInputEffectBounds(0);
        final Rectangle bounds = this.getInputBounds(0);
        final Rectangle inputNativeBounds = this.getInputNativeBounds(0);
        final float startX = (bounds.x - contentBounds.getMinX()) / inputNativeBounds.width;
        final float startY = (bounds.y - contentBounds.getMinY()) / inputNativeBounds.height;
        final float pixelWidth = Math.max((float) effect.getPixelWidth(), 1) / contentBounds.getWidth();
        final float pixelHeight = Math.max((float) effect.getPixelHeight(), 1) / contentBounds.getHeight();
        final float scaleX = (float) Math.hypot(this.getTransform().getMxx(), this.getTransform().getMyx());
        final float scaleY = (float) Math.hypot(this.getTransform().getMxy(), this.getTransform().getMyy());
        shader.setConstant("pixelSize", pixelWidth * scaleX, pixelHeight * scaleY);
        shader.setConstant("resolution", (float) this.getDestNativeBounds().width, (float) this.getDestNativeBounds().height);
        shader.setConstant("viewport", startX, startY, contentBounds.getWidth() / inputNativeBounds.width,
                contentBounds.getHeight() / inputNativeBounds.height);
    }

}
