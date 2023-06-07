package de.teragam.jfxshader.samples.effects;

import javafx.beans.property.DoubleProperty;

import de.teragam.jfxshader.effect.EffectDependencies;
import de.teragam.jfxshader.effect.OneSamplerEffect;

@EffectDependencies(PixelateEffectPeer.class)
public class Pixelate extends OneSamplerEffect {

    private final DoubleProperty offsetX;
    private final DoubleProperty offsetY;
    private final DoubleProperty pixelWidth;
    private final DoubleProperty pixelHeight;

    public Pixelate() {
        this(10, 10);
    }

    public Pixelate(double pixelWidth, double pixelHeight) {
        this.pixelWidth = super.createEffectDoubleProperty(pixelWidth, "pixelWidth");
        this.pixelHeight = super.createEffectDoubleProperty(pixelHeight, "pixelHeight");
        this.offsetX = super.createEffectDoubleProperty(0.0, "offsetX");
        this.offsetY = super.createEffectDoubleProperty(0.0, "offsetY");
    }

    public double getOffsetX() {
        return this.offsetX.get();
    }

    public DoubleProperty offsetXProperty() {
        return this.offsetX;
    }

    public void setOffsetX(double offsetX) {
        this.offsetX.set(offsetX);
    }

    public double getOffsetY() {
        return this.offsetY.get();
    }

    public DoubleProperty offsetYProperty() {
        return this.offsetY;
    }

    public void setOffsetY(double offsetY) {
        this.offsetY.set(offsetY);
    }

    public double getPixelWidth() {
        return this.pixelWidth.get();
    }

    public DoubleProperty pixelWidthProperty() {
        return this.pixelWidth;
    }

    public void setPixelWidth(double pixelWidth) {
        this.pixelWidth.set(pixelWidth);
    }

    public double getPixelHeight() {
        return this.pixelHeight.get();
    }

    public DoubleProperty pixelHeightProperty() {
        return this.pixelHeight;
    }

    public void setPixelHeight(double pixelHeight) {
        this.pixelHeight.set(pixelHeight);
    }

}
