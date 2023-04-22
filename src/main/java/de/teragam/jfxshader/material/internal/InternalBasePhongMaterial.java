package de.teragam.jfxshader.material.internal;

import com.sun.prism.TextureMap;
import com.sun.prism.impl.BasePhongMaterial;
import com.sun.prism.impl.Disposer;

import de.teragam.jfxshader.material.ShaderMaterial;

public class InternalBasePhongMaterial extends BasePhongMaterial {

    private final ShaderMaterial material;

    public InternalBasePhongMaterial(ShaderMaterial material, Disposer.Record disposerRecord) {
        super(disposerRecord);
        this.material = material;
    }

    public static InternalBasePhongMaterial create(ShaderMaterial material) {
        return new InternalBasePhongMaterial(material, () -> {});
    }

    public ShaderMaterial getShaderMaterial() {
        return this.material;
    }

    @Override
    public void setDiffuseColor(float r, float g, float b, float a) {
        // Not needed
    }

    @Override
    public void setSpecularColor(boolean set, float r, float g, float b, float a) {
        // Not needed
    }

    @Override
    public void setTextureMap(TextureMap map) {
        // Not needed
    }

    @Override
    public void lockTextureMaps() {
        // Not needed
    }

    @Override
    public void unlockTextureMaps() {
        // Not needed
    }

    @Override
    public void dispose() {
        this.disposerRecord.dispose();
    }

}
