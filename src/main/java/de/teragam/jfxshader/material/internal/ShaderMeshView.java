package de.teragam.jfxshader.material.internal;

import com.sun.prism.Graphics;
import com.sun.prism.Material;
import com.sun.prism.MeshView;
import com.sun.prism.impl.BaseContext;
import com.sun.prism.impl.BaseGraphics;

import de.teragam.jfxshader.internal.MaterialController;
import de.teragam.jfxshader.internal.Reflect;

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
        this.material.lockTextureMaps();
        if (g instanceof MeshProxyHelper.GraphicsHelper) {
            final Graphics rawGraphics = ((MeshProxyHelper.GraphicsHelper) g).getRawGraphics();
            final BaseContext context = Reflect.on(BaseGraphics.class).getFieldValue("context", rawGraphics);
            Reflect.on(context.getClass()).invokeMethod("renderMeshView", long.class, Graphics.class).invoke(context, 0, g);
            MaterialController.getPeer(this.getMaterial().getShaderMaterial()).filter(rawGraphics, this);
        }
        this.material.unlockTextureMaps();
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
