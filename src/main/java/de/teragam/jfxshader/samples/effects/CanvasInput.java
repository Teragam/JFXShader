package de.teragam.jfxshader.samples.effects;

import java.util.Objects;

import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.scene.image.Image;

import com.sun.javafx.beans.event.AbstractNotifyListener;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.tk.Toolkit;
import com.sun.scenario.effect.Effect;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.impl.prism.PrRenderInfo;
import com.sun.scenario.effect.impl.state.RenderState;

import de.teragam.jfxshader.effect.EffectDependencies;
import de.teragam.jfxshader.effect.EffectRenderer;
import de.teragam.jfxshader.effect.ShaderEffect;
import de.teragam.jfxshader.misc.ImageCanvas;

/**
 * A custom shader effect that displays the image from an {@link ImageCanvas}.
 * Works in the same way as the JavaFX {@link javafx.scene.effect.ImageInput} effect but uses the GPU-side image of the canvas instead of the
 * CPU-side pixel data.
 * <p>
 * The effect is designed to be used in combination with other effects to allow using the image from an {@link ImageCanvas} as an input for other shader
 * effects.
 */
@EffectDependencies(CanvasInputEffectPeer.class)
@EffectRenderer(CanvasInputEffectRenderer.class)
public class CanvasInput extends ShaderEffect {

    private final ImageCanvas imageCanvas;
    private final ObjectProperty<Image> source;
    private final AbstractNotifyListener platformImageChangeListener;

    public CanvasInput(ImageCanvas imageCanvas) {
        super(1);
        this.imageCanvas = Objects.requireNonNull(imageCanvas, "imageCanvas can not be null");
        this.platformImageChangeListener = new AbstractNotifyListener() {
            @Override
            public void invalidated(Observable valueModel) {
                CanvasInput.this.markDirty();
            }
        };
        this.source = this.createEffectObjectProperty(null, "source");
        this.source.addListener((obs, oldImage, newImage) -> {
            if (oldImage != null) {
                Toolkit.getImageAccessor().getImageProperty(oldImage).removeListener(CanvasInput.this.platformImageChangeListener.getWeakListener());
            }
            if (newImage != null) {
                Toolkit.getImageAccessor().getImageProperty(newImage).addListener(CanvasInput.this.platformImageChangeListener.getWeakListener());
            }
        });
        this.source.bind(imageCanvas.imageProperty());
    }

    @Override
    public BaseBounds getBounds(BaseBounds inputBounds) {
        final Image localSource = this.source.get();
        if (localSource != null && Toolkit.getImageAccessor().getPlatformImage(localSource) != null) {
            final float localWidth = (float) localSource.getWidth();
            final float localHeight = (float) localSource.getHeight();
            return new RectBounds(0, 0, localWidth, localHeight);
        } else {
            return new RectBounds();
        }
    }

    @Override
    public CanvasInput copy() {
        return new CanvasInput(this.imageCanvas);
    }

    public ImageCanvas getImageCanvas() {
        return this.imageCanvas;
    }

    @Override
    public RenderState getRenderState(FilterContext fctx, BaseTransform transform, Rectangle outputClip, PrRenderInfo renderHelper, Effect defaultInput) {
        return RenderState.UserSpaceRenderState;
    }

}
