package de.teragam.jfxshader.samples.materials.fresnel;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import de.teragam.jfxshader.material.MaterialDependency;
import de.teragam.jfxshader.material.ShaderMaterial;

@MaterialDependency(FresnelMaterialPeer.class)
public class FresnelMaterial extends ShaderMaterial {

    private DoubleProperty glowStrength;
    private ObjectProperty<Image> diffuseImage;
    private ObjectProperty<Color> diffuseColor;

    public FresnelMaterial() {
        this(0.1);
    }

    public FresnelMaterial(double glowStrength) {
        this.setGlowStrength(glowStrength);
    }

    public double getGlowStrength() {
        return this.glowStrengthProperty().get();
    }

    public DoubleProperty glowStrengthProperty() {
        if (this.glowStrength == null) {
            this.glowStrength = super.createMaterialDoubleProperty(0.1, "glowStrength");
        }
        return this.glowStrength;
    }

    public void setGlowStrength(double glowStrength) {
        this.glowStrengthProperty().set(glowStrength);
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

    public Color getDiffuseColor() {
        return this.diffuseColorProperty().get();
    }

    public ObjectProperty<Color> diffuseColorProperty() {
        if (this.diffuseColor == null) {
            this.diffuseColor = super.createMaterialObjectProperty(Color.WHITE, "diffuseColor");
        }
        return this.diffuseColor;
    }

    public void setDiffuseColor(Color diffuseColor) {
        this.diffuseColorProperty().set(diffuseColor);
    }

}
