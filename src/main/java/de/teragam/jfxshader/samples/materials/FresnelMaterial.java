package de.teragam.jfxshader.samples.materials;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import de.teragam.jfxshader.material.MaterialDependency;
import de.teragam.jfxshader.material.ShaderMaterial;

@MaterialDependency(FresnelMaterialPeer.class)
public class FresnelMaterial extends ShaderMaterial {

    private final DoubleProperty power;
    private final ObjectProperty<Color> glowColor;
    private final ObjectProperty<Image> diffuseImage;

    public FresnelMaterial() {
        this(2.0);
    }

    public FresnelMaterial(double power) {
        this.power = super.createMaterialDoubleProperty(power, "power");
        this.glowColor = super.createMaterialObjectProperty(Color.WHITE, "diffuseColor");
        this.diffuseImage = super.createMaterialImageProperty(null, "diffuseImage");
    }

    public double getPower() {
        return this.powerProperty().get();
    }

    public DoubleProperty powerProperty() {
        return this.power;
    }

    public void setPower(double power) {
        this.powerProperty().set(power);
    }

    public Image getDiffuseImage() {
        return this.diffuseImageProperty().get();
    }

    public ObjectProperty<Image> diffuseImageProperty() {
        return this.diffuseImage;
    }

    public void setDiffuseImage(Image diffuseImage) {
        this.diffuseImageProperty().set(diffuseImage);
    }

    public Color getGlowColor() {
        return this.glowColorProperty().get();
    }

    public ObjectProperty<Color> glowColorProperty() {
        return this.glowColor;
    }

    public void setGlowColor(Color glowColor) {
        this.glowColorProperty().set(glowColor);
    }

}
