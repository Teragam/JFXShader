package de.teragam.jfxshader.samples.blendshapes;


import com.sun.javafx.geom.Rectangle;

public class BlendShape {

    private final Rectangle bounds;
    private final double width;
    private final double feather;
    private final double opacity;

    public BlendShape(Rectangle bounds, double width, double feather, double opacity) {
        this.bounds = bounds;
        this.width = width;
        this.feather = feather;
        this.opacity = opacity;
    }

    public Rectangle getBounds() {
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
