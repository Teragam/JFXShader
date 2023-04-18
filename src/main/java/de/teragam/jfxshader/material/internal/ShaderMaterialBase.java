package de.teragam.jfxshader.material.internal;

import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;

import com.sun.javafx.sg.prism.NGPhongMaterial;

import de.teragam.jfxshader.internal.Reflect;
import de.teragam.jfxshader.material.ShaderMaterial;


//TODO: Temporary class to test the shader material
public class ShaderMaterialBase extends PhongMaterial {

    private final ShaderMaterial material;

    public ShaderMaterialBase(ShaderMaterial material) {
        this.material = material;
    }

    public void setDirty(boolean value) {
        Reflect.on(Material.class).invokeMethod("setDirty", boolean.class).invoke(this, value);
    }

    public boolean isDirty() {
        return (boolean) Reflect.on(Material.class).invokeMethod("isDirty").invoke(this);
    }

    public ShaderMaterial getJFXShaderMaterial() {
        return this.material;
    }

    private NGPhongMaterial peer;

    public NGPhongMaterial getInternalNGMaterial() {
        if (this.peer == null) {
            this.peer = new InternalNGPhongMaterial(this.material);
        }
        return this.peer;
    }

    public void updateMaterial() {
        this.setDirty(false);
    }

    @Override
    public String toString() {
        return "ShaderMaterialBase [material=" + this.material + "]";
    }

}
