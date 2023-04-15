package de.teragam.jfxshader.material.internal;

import com.sun.prism.Graphics;
import com.sun.prism.Material;
import com.sun.prism.Mesh;
import com.sun.prism.MeshView;

import de.teragam.jfxshader.internal.MeshRendererHelper;

public class ShaderMeshView implements MeshView {

    private final Mesh mesh;
    private Material material;
    private int cullingMode;
    private boolean wireframe;

    public ShaderMeshView(Mesh mesh) {
        this.mesh = mesh;
    }

    @Override
    public void setCullingMode(int mode) {
        this.cullingMode = mode;
    }

    @Override
    public void setMaterial(Material material) {
        this.material = material;
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

    public Mesh getMesh() {
        return this.mesh;
    }

    public int getCullingMode() {
        return this.cullingMode;
    }

    public boolean isWireframe() {
        return this.wireframe;
    }
}
