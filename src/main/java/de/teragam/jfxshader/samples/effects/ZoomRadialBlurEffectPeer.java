package de.teragam.jfxshader.samples.effects;

import java.util.Map;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.NoninvertibleTransformException;

import de.teragam.jfxshader.JFXShader;
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
        final Map<String, Integer> params = Map.of("center", 0, "blurSteps", 1, "strength", 2);
        return new ShaderDeclaration(samplers, params,
                ZoomRadialBlurEffectPeer.class.getResourceAsStream("/de/teragam/jfxshader/samples/effects/zoomradialblur/zoomradialblur.frag"),
                ZoomRadialBlurEffectPeer.class.getResourceAsStream("/de/teragam/jfxshader/samples/effects/zoomradialblur/zoomradialblur.obj"));
    }

    @Override
    protected void updateShader(JFXShader shader, ZoomRadialBlur effect) {
        final BaseBounds contentBounds = super.getInputEffectBounds(0);
        final Rectangle bounds = this.getInputBounds(0);
        final Rectangle inputNativeBounds = this.getInputNativeBounds(0);
        final double scaleX = Math.hypot(this.getTransform().getMxx(), this.getTransform().getMyx());
        final double scaleY = Math.hypot(this.getTransform().getMxy(), this.getTransform().getMyy());
        final float centerX = (float) (effect.getCenterX() * scaleX - (bounds.x - contentBounds.getMinX()));
        final float centerY = (float) (effect.getCenterY() * scaleY - (bounds.y - contentBounds.getMinY()));
        final float[] center = new float[]{centerX, centerY};
        try {
            super.getInputTransform(0).inverseDeltaTransform(center, 0, center, 0, 1);
        } catch (NoninvertibleTransformException e) {
            // Ignore
        }
        center[0] /= inputNativeBounds.width;
        center[1] /= inputNativeBounds.height;
        shader.setConstant("center", center[0], center[1]);
        shader.setConstant("blurSteps", Math.max(Math.min(effect.getBlurSteps(), 64), 1));
        final double strength = effect.getStrength() * (1.0 / Math.max(contentBounds.getWidth(), contentBounds.getHeight())) * Math.max(scaleX, scaleY);
        shader.setConstant("strength", (float) (strength));
    }

}
