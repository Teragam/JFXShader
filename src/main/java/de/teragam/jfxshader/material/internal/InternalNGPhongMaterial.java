package de.teragam.jfxshader.material.internal;

import com.sun.javafx.sg.prism.NGPhongMaterial;

import de.teragam.jfxshader.material.ShaderMaterial;

public class InternalNGPhongMaterial extends NGPhongMaterial {

    private final ShaderMaterial material;

    public InternalNGPhongMaterial(ShaderMaterial material) {
        this.material = material;
    }

    public ShaderMaterial getMaterial() {
        return this.material;
    }

}
