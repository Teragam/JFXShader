package de.teragam.jfxshader.samples.effects.zoomradialblur;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;

import de.teragam.jfxshader.effect.EffectDependencies;
import de.teragam.jfxshader.effect.OneSamplerEffect;

@EffectDependencies(ZoomRadialBlurEffectPeer.class)
public class ZoomRadialBlur extends OneSamplerEffect {

    private final DoubleProperty strength;
    private final IntegerProperty blurSteps;
    private final DoubleProperty centerX;
    private final DoubleProperty centerY;

    public ZoomRadialBlur() {
        this(100.0);
    }

    public ZoomRadialBlur(double strength) {
        this(strength, 16);
    }

    public ZoomRadialBlur(double strength, int blurSteps) {
        this.strength = super.createEffectDoubleProperty(strength, "strength");
        this.blurSteps = super.createEffectIntegerProperty(blurSteps, "blurSteps");
        this.centerX = super.createEffectDoubleProperty(0.0, "centerX");
        this.centerY = super.createEffectDoubleProperty(0.0, "centerY");
    }

    public double getStrength() {
        return this.strength.get();
    }

    public DoubleProperty strengthProperty() {
        return this.strength;
    }

    public void setStrength(double strength) {
        this.strength.set(strength);
    }

    public int getBlurSteps() {
        return this.blurSteps.get();
    }

    public IntegerProperty blurStepsProperty() {
        return this.blurSteps;
    }

    public void setBlurSteps(int blurSteps) {
        this.blurSteps.set(blurSteps);
    }

    public double getCenterX() {
        return this.centerX.get();
    }

    public DoubleProperty centerXProperty() {
        return this.centerX;
    }

    public void setCenterX(double centerX) {
        this.centerX.set(centerX);
    }

    public double getCenterY() {
        return this.centerY.get();
    }

    public DoubleProperty centerYProperty() {
        return this.centerY;
    }

    public void setCenterY(double centerY) {
        this.centerY.set(centerY);
    }

}
