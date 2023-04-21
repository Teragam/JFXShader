package de.teragam.jfxshader.material.internal;

import com.sun.prism.Graphics;
import com.sun.prism.Material;
import com.sun.prism.impl.BaseGraphics;
import com.sun.prism.impl.BaseMesh;
import com.sun.prism.impl.BaseMeshView;
import com.sun.prism.impl.Disposer;
import com.sun.prism.impl.ps.BaseShaderContext;

import de.teragam.jfxshader.MaterialController;
import de.teragam.jfxshader.util.Reflect;

public class ShaderMeshView extends BaseMeshView {

    private final BaseMesh mesh;
    private InternalBasePhongMaterial material;
    private int cullingMode;
    private boolean wireframe;

    public ShaderMeshView(BaseMesh mesh, Disposer.Record disposerRecord) {
        super(disposerRecord);
        this.mesh = mesh;
    }

    public static ShaderMeshView create(BaseMesh mesh) {
        return new ShaderMeshView(mesh, () -> {});
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
            final BaseShaderContext context = Reflect.on(BaseGraphics.class).getFieldValue("context", rawGraphics);
            MaterialController.getPeer(this.getMaterial().getShaderMaterial()).filter(rawGraphics, this, context);
        }
        this.material.unlockTextureMaps();
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void dispose() {
        if (this.mesh != null) {
            this.mesh.dispose();
        }
        this.material = null;
    }

    public BaseMesh getMesh() {
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
