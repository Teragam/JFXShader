package de.teragam.jfxshader.samples.effects.pixelate;

import javafx.beans.property.DoubleProperty;

import de.teragam.jfxshader.effect.EffectDependencies;
import de.teragam.jfxshader.effect.OneSamplerEffect;

@EffectDependencies(PixelateEffectPeer.class)
public class Pixelate extends OneSamplerEffect {

    private DoubleProperty offsetX;
    private DoubleProperty offsetY;
    private DoubleProperty pixelWidth;
    private DoubleProperty pixelHeight;

    public Pixelate() {
    }

    public Pixelate(double pixelWidth, double pixelHeight) {
        this.setPixelWidth(pixelWidth);
        this.setPixelHeight(pixelHeight);
    }

    public DoubleProperty offSetXProperty() {
        if (this.offsetX == null) {
            this.offsetX = super.createEffectDoubleProperty(0.0, "offsetX");
        }
        return this.offsetX;
    }

    public double getOffsetX() {
        return this.offSetXProperty().get();
    }

    public void setOffsetX(double offsetX) {
        this.offSetXProperty().set(offsetX);
    }

    public DoubleProperty offSetYProperty() {
        if (this.offsetY == null) {
            this.offsetY = super.createEffectDoubleProperty(0.0, "offsetY");
        }
        return this.offsetY;
    }

    public double getOffsetY() {
        return this.offSetYProperty().get();
    }

    public void setOffsetY(double offsetY) {
        this.offSetYProperty().set(offsetY);
    }

    public DoubleProperty pixelWidthProperty() {
        if (this.pixelWidth == null) {
            this.pixelWidth = super.createEffectDoubleProperty(10.0, "pixelWidth");
        }
        return this.pixelWidth;
    }

    public double getPixelWidth() {
        return this.pixelWidthProperty().get();
    }

    public void setPixelWidth(double pixelWidth) {
        this.pixelWidthProperty().set(pixelWidth);
    }

    public DoubleProperty pixelHeightProperty() {
        if (this.pixelHeight == null) {
            this.pixelHeight = super.createEffectDoubleProperty(10.0, "pixelHeight");
        }
        return this.pixelHeight;
    }

    public double getPixelHeight() {
        return this.pixelHeightProperty().get();
    }

    public void setPixelHeight(double pixelHeight) {
        this.pixelHeightProperty().set(pixelHeight);
    }

}
