package de.teragam.jfxshader.material.internal;

import com.sun.prism.Graphics;
import com.sun.prism.Material;
import com.sun.prism.MeshView;

import de.teragam.jfxshader.internal.MeshRendererHelper;

public class ShaderMeshView implements MeshView {

    private final ShaderBaseMesh mesh;
    private InternalBasePhongMaterial material;
    private int cullingMode;
    private boolean wireframe;

    public ShaderMeshView(ShaderBaseMesh mesh) {
        this.mesh = mesh;
    }

    @Override
    public void setCullingMode(int mode) {
        this.cullingMode = mode;
    }

    @Override
    public void setMaterial(Material material) {
        this.material = (InternalBasePhongMaterial) material;
    }

    @Override
    public void setWireframe(boolean wireframe) {
        this.wireframe = wireframe;
    }

    @Override
    public void setAmbientLight(float r, float g, float b) {
        // Not needed
    }

    @Override
    public void setLight(int index, float x, float y, float z, float r, float g, float b, float w, float ca, float la, float qa, float isAttenuated,
                         float maxRange, float dirX, float dirY, float dirZ, float innerAngle, float outerAngle, float falloff) {
        // Not needed
    }

    @Override
    public void render(Graphics g) {
        MeshRendererHelper.renderMeshView(this, g);
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void dispose() {
        this.material = null;
    }

    public ShaderBaseMesh getMesh() {
        return this.mesh;
    }

    public int getCullingMode() {
        return this.cullingMode;
    }

    public boolean isWireframe() {
        return this.wireframe;
    }

    public InternalBasePhongMaterial getMaterial() {
        return this.material;
    }

}
