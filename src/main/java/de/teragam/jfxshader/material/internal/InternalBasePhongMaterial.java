package de.teragam.jfxshader.material.internal;

import com.sun.prism.ResourceFactory;
import com.sun.prism.TextureMap;
import com.sun.prism.impl.BasePhongMaterial;
import com.sun.prism.impl.Disposer;

import de.teragam.jfxshader.material.ShaderMaterial;

//TODO: Temporary implementation to test the shader material
public class InternalBasePhongMaterial extends BasePhongMaterial {
    static int count = 0;

    private final ShaderMaterial material;

    public InternalBasePhongMaterial(ResourceFactory factory, ShaderMaterial material, Disposer.Record disposerRecord) {
        super(disposerRecord);
        count++;
        this.material = material;
    }

    public static InternalBasePhongMaterial create(ResourceFactory rf, ShaderMaterial material) {
        return new InternalBasePhongMaterial(rf, material, () -> {});
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
    }

    @Override
    public void unlockTextureMaps() {
    }

    @Override
    public void dispose() {
        disposerRecord.dispose();
        count--;
    }

}
