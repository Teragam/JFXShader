package de.teragam.jfxshader.material.internal;

import com.sun.prism.Image;
import com.sun.prism.ResourceFactory;
import com.sun.prism.Texture;
import com.sun.prism.TextureMap;
import com.sun.prism.impl.BasePhongMaterial;

import de.teragam.jfxshader.material.ShaderMaterial;

//TODO: Temporary implementation to test the shader material
public class InternalBasePhongMaterial extends BasePhongMaterial {
    static int count = 0;
    private TextureMap maps[] = new TextureMap[MAX_MAP_TYPE];

    private final ResourceFactory factory;

    private final ShaderMaterial material;

    public InternalBasePhongMaterial(ResourceFactory factory, ShaderMaterial material) {
        super(() -> {});
        this.factory = factory;
        count++;
        this.material = material;
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

    private Texture setupTexture(TextureMap map, boolean useMipmap) {
        Image image = map.getImage();
        Texture texture = (image == null) ? null : factory.getCachedTexture(image, Texture.WrapMode.REPEAT, useMipmap);
        return texture;
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
