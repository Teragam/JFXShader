package de.teragam.jfxshader.misc.internal;

import java.nio.IntBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javafx.animation.AnimationTimer;

import com.sun.javafx.sg.prism.NGCanvas;
import com.sun.javafx.tk.PlatformImage;
import com.sun.prism.Graphics;
import com.sun.prism.GraphicsPipeline;
import com.sun.prism.Image;
import com.sun.prism.RTTexture;
import com.sun.prism.ResourceFactory;
import com.sun.prism.Texture;
import com.sun.prism.impl.BaseResourceFactory;

import de.teragam.jfxshader.misc.ImageCanvas;
import de.teragam.jfxshader.util.Reflect;

public class InternalNGCanvas extends NGCanvas {

    private Image platformImage;
    private RTTexture newTexture;

    private final ImageCanvas canvas;
    private final Map<Image, Integer> staleImages;
    private final ConcurrentLinkedQueue<Image> disposableImages;
    private final AtomicBoolean canvasChanged;
    private final AtomicReference<Runnable> switchTexture;
    private final AnimationTimer timer;

    public InternalNGCanvas(ImageCanvas canvas) {
        super();
        this.canvas = canvas;
        this.staleImages = new ConcurrentHashMap<>();
        this.disposableImages = new ConcurrentLinkedQueue<>();
        this.canvasChanged = new AtomicBoolean();
        this.switchTexture = new AtomicReference<>();

        this.timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                for (final Map.Entry<Image, Integer> entry : InternalNGCanvas.this.staleImages.entrySet()) {
                    InternalNGCanvas.this.staleImages.computeIfPresent(entry.getKey(), (image, framesToKeep) -> {
                        if (framesToKeep <= 1) {
                            InternalNGCanvas.this.disposableImages.add(image);
                            return null;
                        } else {
                            return framesToKeep - 1;
                        }
                    });
                }
                final Runnable switchActionOpt = InternalNGCanvas.this.switchTexture.getAndSet(null);
                if (switchActionOpt != null) {
                    switchActionOpt.run();
                }
                if (InternalNGCanvas.this.canvasChanged.getAndSet(false)) {
                    Reflect.on(javafx.scene.image.Image.class).method("pixelsDirty").invoke(InternalNGCanvas.this.canvas.getImage());
                } else {
                    if (InternalNGCanvas.this.staleImages.isEmpty()) {
                        this.stop();
                    }
                }
            }
        };
    }

    private boolean cloneTexture(RTTexture srcTex, int srcWidth, int srcHeight, int width, int height) {
        boolean texCreated = false;
        if (srcTex == null) {
            return texCreated;
        }
        if (this.newTexture == null || this.newTexture.isSurfaceLost() || this.newTexture.getContentWidth() != width || this.newTexture.getContentHeight() != height) {
            this.newTexture = GraphicsPipeline.getDefaultResourceFactory().createRTTexture(width, height, Texture.WrapMode.CLAMP_TO_ZERO);
            if (this.newTexture == null || this.newTexture.isSurfaceLost()) {
                return texCreated;
            }
            this.newTexture.contentsUseful();
            this.newTexture.makePermanent();
            this.newTexture.lock();
            texCreated = true;
        }
        this.newTexture.createGraphics().blit(srcTex, null, 0, 0, srcWidth, srcHeight, 0, 0, width, height);
        return texCreated;
    }

    @Override
    protected void renderContent(Graphics g) {
        final Reflect<NGCanvas> canvasReflect = Reflect.on(NGCanvas.class);
        // The buffer is null if nothing needs to be rendered.
        final Object thebuf = canvasReflect.getFieldValue("thebuf", this);
        super.renderContent(g);
        final Object cv = canvasReflect.getFieldValue("cv", this);
        final RTTexture tex = Reflect.on(cv.getClass()).getFieldValue("tex", cv);
        final int width = canvasReflect.getFieldValue("tw", this);
        final int height = canvasReflect.getFieldValue("th", this);
        // The size of the canvas texture corresponds to the canvas size multiplied by a pixel scale factor to support HiDPI displays.
        // This factor is calculated based on the highest pixel scale of all displays and rounded up to the next integer.
        // This may lead to unexpected image sizes so the provided JavaFX image is downscaled to the canvas size, unless deactivated by setHighDpiScaling(true).
        float highestPixelScale = canvasReflect.getFieldValue("highestPixelScale", this);
        if (this.canvas.isHighDpiScaling()) {
            highestPixelScale = 1.0f;
        }
        final int scaledWidth = (int) (width / highestPixelScale);
        final int scaledHeight = (int) (height / highestPixelScale);
        // Whenever the canvas size changes, the NGCanvas disposes the old texture which may still be used by other nodes for at least the next frame.
        // Cloning the texture allows controlling the disposal of the old canvas content.
        final boolean texCreated = this.cloneTexture(tex, width, height, scaledWidth, scaledHeight);
        this.canvasChanged.compareAndSet(false, texCreated || (thebuf != null));
        if (texCreated) {
            // The new platform image does not provide any CPU-side buffer as the texture always resides on the GPU.
            final Image newPlatformImage = Image.fromIntArgbPreData(IntBuffer.wrap(new int[0]), scaledWidth, scaledHeight, 0, highestPixelScale);
            this.switchTexture.set(() -> {
                // setPlatformImageWH notifies all listeners of the JavaFX image about the new platform image and dimensions.
                // This is done on the JavaFX Application Thread. Additionally, renderContent may be called multiple times in a single frame, so the
                // texture switch is deferred to the AnimationTimer to ensure it only happens once per frame at most.
                final Reflect<javafx.scene.image.Image> imageReflect = Reflect.on(javafx.scene.image.Image.class);
                imageReflect.method("setPlatformImageWH", PlatformImage.class, double.class, double.class)
                        .invoke(this.canvas.getImage(), newPlatformImage, scaledWidth, scaledHeight);
            });
            final Image oldImage = this.platformImage;
            if (oldImage != null) {
                // Keeps the old texture alive for 2 frames to allow nodes that are rendered before the ImageCanvas to still use the old texture.
                this.staleImages.put(oldImage, 2);
            }
            this.platformImage = newPlatformImage;
            this.replaceCachedTexture(newPlatformImage, this.newTexture, g.getResourceFactory());
        }
        this.timer.start();
    }

    private void replaceCachedTexture(Image newPlatformImage, RTTexture associatedTexture, ResourceFactory factory) {
        if (factory == null) {
            return;
        }
        final Reflect<BaseResourceFactory> reflect = Reflect.on(BaseResourceFactory.class);
        // The clampTexCache of the resource factory associates platform images to their corresponding textures.
        // By replacing the texture in the cache, all nodes that use the platform image will use the new texture as well.
        final Map<Image, Texture> clampTexCache = reflect.getFieldValue("clampTexCache", factory);
        Image image;
        while ((image = this.disposableImages.poll()) != null) {
            final Texture texture = clampTexCache.remove(image);
            if (texture != null) {
                texture.dispose();
            }
        }
        clampTexCache.compute(newPlatformImage, (img, oldTexture) -> {
            if (oldTexture != associatedTexture && (oldTexture != null)) {
                oldTexture.dispose();
            }
            return associatedTexture;
        });
    }

}
