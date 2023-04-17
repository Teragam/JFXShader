package de.teragam.jfxshader.material.internal;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.image.Image;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;

import com.sun.javafx.scene.paint.MaterialHelper;
import com.sun.javafx.sg.prism.NGPhongMaterial;
import com.sun.javafx.tk.Toolkit;

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
