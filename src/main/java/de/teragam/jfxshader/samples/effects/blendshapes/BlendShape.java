package de.teragam.jfxshader.samples.effects.blendshapes;

import javafx.geometry.Rectangle2D;

public class BlendShape {

    private final Rectangle2D bounds;
    private final double width;
    private final double feather;
    private final double opacity;

    public BlendShape(Rectangle2D bounds, double width, double feather, double opacity) {
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
