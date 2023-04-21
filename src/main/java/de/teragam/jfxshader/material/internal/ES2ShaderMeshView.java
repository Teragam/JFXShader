package de.teragam.jfxshader.material.internal;

import com.sun.prism.Material;
import com.sun.prism.es2.ES2ResourceFactory;
import com.sun.prism.impl.BaseMesh;
import com.sun.prism.impl.Disposer;
import com.sun.prism.impl.ps.BaseShaderContext;

import de.teragam.jfxshader.internal.Reflect;

public class ES2ShaderMeshView extends ShaderMeshView {

    private final long nativeHandle;
    private final BaseShaderContext es2Context;

    public ES2ShaderMeshView(long nativeHandle, BaseMesh mesh, Disposer.Record disposerRecord, BaseShaderContext es2Context) {
        super(mesh, disposerRecord);
        this.nativeHandle = nativeHandle;
        this.es2Context = es2Context;
    }

    public static ES2ShaderMeshView create(ES2ResourceFactory rf, BaseMesh mesh) {
        final BaseShaderContext es2Context = Reflect.on(rf.getClass()).getFieldValue("context", rf);
        final long nativeHandle = Reflect.on(es2Context.getClass()).<Long>invokeMethod("createES2MeshView").invoke(es2Context, mesh);
        return new ES2ShaderMeshView(nativeHandle, mesh, new ES2MeshViewDisposerRecord(es2Context, nativeHandle), es2Context);
    }

    @Override
    public void setCullingMode(int mode) {
        super.setCullingMode(mode);
        Reflect.on(this.es2Context.getClass()).invokeMethod("setCullingMode").invoke(this.es2Context, this.nativeHandle, mode);
    }

    @Override
    public void setWireframe(boolean wireframe) {
        super.setWireframe(wireframe);
        Reflect.on(this.es2Context.getClass()).invokeMethod("setWireframe").invoke(this.es2Context, this.nativeHandle, wireframe);
    }

    @Override
    public void setMaterial(Material material) {
        super.setMaterial(material);
        final Reflect<?> contextReflect = Reflect.on(this.es2Context.getClass());
        final Object glContext = contextReflect.getFieldValue("glContext", this.es2Context);
        Reflect.on(glContext.getClass()).invokeMethod("setMaterial")
                .invoke(this.es2Context, this.nativeHandle, ((InternalES2BasePhongMaterial) material).getNativeHandle());
    }

    public long getNativeHandle() {
        return this.nativeHandle;
    }

    public static class ES2MeshViewDisposerRecord implements Disposer.Record {

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
