package de.teragam.jfxshader.material.internal;

import com.sun.prism.Graphics;
import com.sun.prism.Material;
import com.sun.prism.es2.ES2ResourceFactory;
import com.sun.prism.impl.BaseGraphics;
import com.sun.prism.impl.BaseMesh;
import com.sun.prism.impl.BaseMeshView;
import com.sun.prism.impl.Disposer;
import com.sun.prism.impl.ps.BaseShaderContext;

import de.teragam.jfxshader.internal.MaterialController;
import de.teragam.jfxshader.internal.Reflect;

public class ES2ShaderMeshView extends BaseMeshView {

    private final BaseMesh mesh;
    private final long nativeHandle;
    private InternalBasePhongMaterial material;
    private int cullingMode;
    private boolean wireframe;

    public ES2ShaderMeshView(long nativeHandle, BaseMesh mesh, Disposer.Record disposerRecord) {
        super(disposerRecord);
        this.nativeHandle = nativeHandle;
        this.mesh = mesh;
    }

    public static ES2ShaderMeshView create(ES2ResourceFactory rf, BaseMesh mesh) {
        final BaseShaderContext es2Context = Reflect.on(rf.getClass()).getFieldValue("context", rf);
        final long nativeHandle = Reflect.on(es2Context.getClass()).<Long>invokeMethod("createES2MeshView").invoke(es2Context, mesh);
        return new ES2ShaderMeshView(nativeHandle, mesh, new ES2MeshViewDisposerRecord(es2Context, nativeHandle));
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

    public long getNativeHandle() {
        return this.nativeHandle;
    }

    static class ES2MeshViewDisposerRecord implements Disposer.Record {

        private final BaseShaderContext context;
        private long nativeHandle;

        ES2MeshViewDisposerRecord(BaseShaderContext context, long nativeHandle) {
            this.context = context;
            this.nativeHandle = nativeHandle;
        }

        @Override
        public void dispose() {
            if (this.nativeHandle != 0L) {
                Reflect.on(this.context.getClass()).invokeMethod("releaseES2MeshView").invoke(this.context, this.nativeHandle);
                this.nativeHandle = 0L;
            }
        }
    }

}
