package de.teragam.jfxshader.samples.materials.toonshading;

import javafx.beans.property.DoubleProperty;

import de.teragam.jfxshader.material.MaterialDependency;
import de.teragam.jfxshader.material.ShaderMaterial;

@MaterialDependency(ToonShaderMaterialPeer.class)
public class ToonShaderMaterial extends ShaderMaterial {

    private DoubleProperty edgeWidth;

    public ToonShaderMaterial() {
        this(0.1);
    }

    public ToonShaderMaterial(double edgeWidth) {
        this.setEdgeWidth(edgeWidth);
    }

    public double getEdgeWidth() {
        return this.edgeWidthProperty().get();
    }

    public DoubleProperty edgeWidthProperty() {
        if (this.edgeWidth == null) {
            this.edgeWidth = super.createMaterialDoubleProperty(0.1, "edgeWidth");
        }
        return this.edgeWidth;
    }

    public void setEdgeWidth(double edgeWidth) {
        this.edgeWidthProperty().set(edgeWidth);
    }

}
