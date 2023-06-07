package de.teragam.jfxshader.samples.effects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Rectangle2D;

import de.teragam.jfxshader.effect.EffectDependencies;
import de.teragam.jfxshader.effect.TwoSamplerEffect;

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
        private final double width;
        private final double feather;
        private final double opacity;

        public Shape(Rectangle2D bounds, double width, double feather, double opacity) {
            this.bounds = bounds;
            this.width = width;
            this.feather = feather;
            this.opacity = opacity;
        }

        public Rectangle2D getBounds() {
            return this.bounds;
        }

        public double getWidth() {
            return this.width;
        }

        public double getFeather() {
            return this.feather;
        }

        public double getOpacity() {
            return this.opacity;
        }

    }
}
