package de.teragam.jfxshader.samples.effects;

import javafx.scene.image.Image;

import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.ImageData;
import com.sun.scenario.effect.impl.state.RenderState;

import de.teragam.jfxshader.ShaderController;
import de.teragam.jfxshader.effect.IEffectRenderer;
import de.teragam.jfxshader.effect.InternalEffect;

class CanvasInputEffectRenderer implements IEffectRenderer {

    @Override
    public ImageData render(InternalEffect effect, FilterContext fctx, BaseTransform transform, Rectangle outputClip, RenderState rstate, ImageData... inputs) {
        inputs[0].unref();
        final CanvasInput canvasInput = (CanvasInput) effect.getEffect();
        final Image canvasImage = canvasInput.getImageCanvas().getImage();
        final ImageData canvasImageData = ShaderController.createImageData(fctx, canvasImage, BaseTransform.IDENTITY_TRANSFORM);
        final ImageData result = ShaderController.renderPeer("CanvasInput", effect, fctx, transform, outputClip, rstate, canvasImageData);
        canvasImageData.unref();
        return result;
    }
}
