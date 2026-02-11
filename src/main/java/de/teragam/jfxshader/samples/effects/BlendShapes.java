package de.teragam.jfxshader.samples.effects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Rectangle2D;

import de.teragam.jfxshader.effect.EffectDependencies;
import de.teragam.jfxshader.effect.TwoSamplerEffect;

/**
 * An effect that blends two effect inputs based on given rectangular shapes.
 * Each shape defines a rectangular area with a corner radius, feather, and opacity.
 * The result of the primary effect input is only visible within the shapes, while the secondary effect input is visible outside the shapes.
 * The blending can be inverted using the invertMask property.
 * This effect can be used to limit some effect to a specific region, for example to fake background blur on a specific area.
 *
 * <pre>{@code
 *     BlendShapes blendShapes = new BlendShapes();
 *     ObjectProperty<BlendShapes.Shape> shapeProperty = blendShapes.createShapeProperty();
 *     shapeProperty.set(new BlendShapes.Shape(new Rectangle2D(50, 50, 200, 100), 10, 0, 1));
 *     // it is not required to set both inputs
 *     blendShapes.setPrimaryInput(new ColorAdjust(0.5, 0, 0.2, 0));
 *     someNode.setEffect(blendShapes.getFXEffect());
 * }</pre>
 */
@EffectDependencies(BlendShapesEffectPeer.class)
public class BlendShapes extends TwoSamplerEffect {

    private final List<ObjectProperty<Shape>> shapes;
    private final BooleanProperty invertMask;

    public BlendShapes() {
        this.shapes = new ArrayList<>();
        this.invertMask = super.createEffectBooleanProperty(false, "invertMask");
    }

    public ObjectProperty<Shape> createShapeProperty() {
        final ObjectProperty<Shape> property = super.createEffectObjectProperty(null, "shape");
        this.shapes.add(property);
        return property;
    }

    public List<ObjectProperty<Shape>> getBlendShapes() {
        return Collections.unmodifiableList(this.shapes);
    }

    public boolean isInvertMask() {
        return this.invertMaskProperty().get();
    }

    public BooleanProperty invertMaskProperty() {
        return this.invertMask;
    }

    public void setInvertMask(boolean invertMask) {
        this.invertMaskProperty().set(invertMask);
    }

    public static class Shape {

        private final Rectangle2D bounds;
        private final double cornerRadius;
        private final double feather;
        private final double opacity;

        /**
         * Creates a new Shape with the specified properties.
         *
         * @param bounds       The rectangular bounds of the shape in pixels.
         * @param cornerRadius The corner radius of the shape in pixels.
         * @param feather      Smoothing distance at the shape's edge in pixels. A feather of 0 means a hard edge. Positive values smooth outward, negative
         *                     values inward.
         * @param opacity      The opacity of the shape, between 0 (fully transparent) and 1 (fully opaque).
         */
        public Shape(Rectangle2D bounds, double cornerRadius, double feather, double opacity) {
            this.bounds = bounds;
            this.cornerRadius = Math.max(cornerRadius, 0);
            this.feather = feather;
            this.opacity = Math.max(0, Math.min(opacity, 1));
        }

        public Rectangle2D getBounds() {
            return this.bounds;
        }

        public double getCornerRadius() {
            return this.cornerRadius;
        }

        public double getFeather() {
            return this.feather;
        }

        public double getOpacity() {
            return this.opacity;
        }

    }
}
