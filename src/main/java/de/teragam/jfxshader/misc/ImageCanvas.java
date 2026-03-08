package de.teragam.jfxshader.misc;

import java.io.InputStream;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.scene.canvas.CanvasHelper;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.util.Utils;

import de.teragam.jfxshader.misc.internal.InternalNGCanvas;
import de.teragam.jfxshader.util.Reflect;

/**
 * A custom {@link Canvas} implementation that exposes the underlying platform texture as a JavaFX {@link Image} property.
 * The image can be used to display a copy of the canvas content in an ImageView.
 * <p>
 * <strong>Important:</strong> Some nodes that consume an {@link Image} may not be able to use the image provided by this utility.
 * For example, it is not possible to use the image for an JavaFX ImageInput effect as the effect uses the CPU-side pixel data which is not
 * available for canvas drawings.
 * The image is only updated on the GPU and can be used for custom effects that support platform textures directly.
 * The {@link de.teragam.jfxshader.samples.effects.CanvasInput} effect provides the ability to use the canvas image as an input for shader effects.
 * <p>
 * <strong>Known limitations:</strong>
 * <ul>
 *     <li>Depending on the rendering order which gets inferred from the scene graph structure, the nodes that are rendered before the {@link ImageCanvas} lag
 *     one frame behind as the canvas image gets rendered later in the rendering process.</li>
 *     <li>JavaFX renders the canvas with the desired resolution multiplied by the highest pixel scale factor of all displays rounded up to the next integer.
 *     The {@link ImageCanvas} downscales the provided canvas image to the canvas size to avoid unexpectedly large images if a high DPI display is present.
 *     The lower resolution may be noticeable on high DPI displays with a high pixel scale factor.
 *     This default behavior can be disabled by setting the {@link #highDpiScalingProperty()} to true.
 *     </li>
 * </ul>
 */
public class ImageCanvas extends Canvas {

    static {
        Utils.forceInit(CanvasHelper.class);
        final CanvasHelper.CanvasAccessor accessor = Reflect.on(CanvasHelper.class).getFieldValue("canvasAccessor", null);
        final CanvasHelper.CanvasAccessor newAccessor = new CanvasHelper.CanvasAccessor() {
            @Override
            public NGNode doCreatePeer(Node node) {
                if (node instanceof ImageCanvas) {
                    return new InternalNGCanvas((ImageCanvas) node);
                }
                return accessor.doCreatePeer(node);
            }

            @Override
            public void doUpdatePeer(Node node) {
                accessor.doUpdatePeer(node);
            }

            @Override
            public BaseBounds doComputeGeomBounds(Node node, BaseBounds bounds, BaseTransform tx) {
                return accessor.doComputeGeomBounds(node, bounds, tx);
            }

            @Override
            public boolean doComputeContains(Node node, double localX, double localY) {
                return accessor.doComputeContains(node, localX, localY);
            }
        };
        Reflect.on(CanvasHelper.class).setFieldValue("canvasAccessor", null, newAccessor);
    }

    private final ReadOnlyObjectWrapper<Image> image;
    private final BooleanProperty highDpiScaling;

    public ImageCanvas() {
        this(0, 0);
    }

    public ImageCanvas(double width, double height) {
        super(width, height);
        this.image = new ReadOnlyObjectWrapper<>();
        this.highDpiScaling = new SimpleBooleanProperty() {
            @Override
            protected void invalidated() {
                NodeHelper.markDirty(ImageCanvas.this, DirtyBits.NODE_CONTENTS);
            }
        };
        final Reflect<Image> imageReflect = Reflect.on(Image.class);
        final Image dummyImage = imageReflect
                .constructor(String.class, InputStream.class, double.class, double.class, boolean.class, boolean.class, boolean.class)
                .create(null, null, 0, 0, false, false, false);
        imageReflect.method("setProgress", double.class).invoke(dummyImage, 1.0);
        // Marking the image as animated tells nodes like an ImageView to subscribe to platform image updates and update the displayed image accordingly.
        // It also disables the pixel reader to prevent CPU-side access to the image data which is not available for canvas drawings.
        imageReflect.setFieldValue("isAnimated", dummyImage, true);
        this.image.set(dummyImage);
    }

    public ReadOnlyObjectProperty<Image> imageProperty() {
        return this.image.getReadOnlyProperty();
    }

    public Image getImage() {
        return this.image.get();
    }

    /**
     * JavaFX renders the canvas with a higher resolution if a display with a pixel scale factor > 1 is present even if the application window is on a
     * different display with a pixel scale factor of 1.
     * This can lead to unexpected image sizes for the canvas content.
     * The {@link ImageCanvas} downscales the provided canvas image to the canvas size by default to avoid this issue.
     * Displaying the downscaled image on high DPI displays may lead to a blurry image.
     * Setting this property to true re-enables the high DPI scaling and provides a higher resolution image at the cost of having to account for this
     * behavior when using the image.
     *
     * @return The boolean property.
     */
    public BooleanProperty highDpiScalingProperty() {
        return this.highDpiScaling;
    }

    public boolean isHighDpiScaling() {
        return this.highDpiScaling.get();
    }

    public void setHighDpiScaling(boolean highDpiScaling) {
        this.highDpiScaling.set(highDpiScaling);
    }
}
