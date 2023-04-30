package de.teragam.jfxshader.samples.materials.fresnel;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import de.teragam.jfxshader.material.MaterialDependency;
import de.teragam.jfxshader.material.ShaderMaterial;

@MaterialDependency(FresnelMaterialPeer.class)
public class FresnelMaterial extends ShaderMaterial {

    private DoubleProperty power;
    private ObjectProperty<Image> diffuseImage;
    private ObjectProperty<Color> glowColor;

    public FresnelMaterial() {
        this(2.0);
    }

    public FresnelMaterial(double power) {
        this.setPower(power);
    }

    public double getPower() {
        return this.powerProperty().get();
    }

    public DoubleProperty powerProperty() {
        if (this.power == null) {
            this.power = super.createMaterialDoubleProperty(2.0, "power");
        }
        return this.power;
    }

    public void setPower(double power) {
        this.powerProperty().set(power);
    }

    public Image getDiffuseImage() {
        return this.diffuseImageProperty().get();
    }

    public ObjectProperty<Image> diffuseImageProperty() {
        if (this.diffuseImage == null) {
            this.diffuseImage = super.createMaterialImageProperty(null, "diffuseImage");
        }
        return this.diffuseImage;
    }

    public void setDiffuseImage(Image diffuseImage) {
        this.diffuseImageProperty().set(diffuseImage);
    }

    public Color getGlowColor() {
        return this.glowColorProperty().get();
    }

    public ObjectProperty<Color> glowColorProperty() {
        if (this.glowColor == null) {
            this.glowColor = super.createMaterialObjectProperty(Color.WHITE, "diffuseColor");
        }
        return this.glowColor;
    }

    public void setGlowColor(Color glowColor) {
        this.glowColorProperty().set(glowColor);
    }

}
