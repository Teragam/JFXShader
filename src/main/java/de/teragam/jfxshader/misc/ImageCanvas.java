package de.teragam.jfxshader.misc;

import java.io.InputStream;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
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

    public ImageCanvas() {
        this(0, 0);
    }

    public ImageCanvas(double width, double height) {
        super(width, height);
        this.image = new ReadOnlyObjectWrapper<>();
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

}
