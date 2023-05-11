package de.teragam.jfxshader.samples.effects.zoomradialblur;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;

import de.teragam.jfxshader.effect.EffectDependencies;
import de.teragam.jfxshader.effect.OneSamplerEffect;

@EffectDependencies(ZoomRadialBlurEffectPeer.class)
public class ZoomRadialBlur extends OneSamplerEffect {

    private DoubleProperty strength;
    private IntegerProperty blurSteps;
    private DoubleProperty centerX;
    private DoubleProperty centerY;

    public ZoomRadialBlur() {
    }

    public ZoomRadialBlur(double strength) {
        this.setStrength(strength);
    }

    public DoubleProperty strengthProperty() {
        if (this.strength == null) {
            this.strength = super.createEffectDoubleProperty(100.0, "strength");
        }
        return this.strength;
    }

    public double getStrength() {
        return this.strengthProperty().get();
    }

    public void setStrength(double strength) {
        this.strengthProperty().set(strength);
    }

    public IntegerProperty blurStepsProperty() {
        if (this.blurSteps == null) {
            this.blurSteps = super.createEffectIntegerProperty(16, "blurSteps");
        }
        return this.blurSteps;
    }

    public int getBlurSteps() {
        return this.blurStepsProperty().get();
    }

    public void setBlurSteps(int blurSteps) {
        this.blurStepsProperty().set(blurSteps);
    }

    public DoubleProperty centerXProperty() {
        if (this.centerX == null) {
            this.centerX = super.createEffectDoubleProperty(0.0, "centerX");
        }
        return this.centerX;
    }

    public double getCenterX() {
        return this.centerXProperty().get();
    }

    public void setCenterX(double centerX) {
        this.centerXProperty().set(centerX);
    }

    public DoubleProperty centerYProperty() {
        if (this.centerY == null) {
            this.centerY = super.createEffectDoubleProperty(0.0, "centerY");
        }
        return this.centerY;
    }

    public double getCenterY() {
        return this.centerYProperty().get();
    }

    public void setCenterY(double centerY) {
        this.centerYProperty().set(centerY);
    }

}
