package de.teragam.jfxshader.material.internal;

import com.sun.prism.ResourceFactory;
import com.sun.prism.es2.ES2ResourceFactory;
import com.sun.prism.impl.Disposer;
import com.sun.prism.impl.ps.BaseShaderContext;

import de.teragam.jfxshader.internal.Reflect;
import de.teragam.jfxshader.material.ShaderMaterial;

public class InternalES2BasePhongMaterial extends InternalBasePhongMaterial {

    private final long nativeHandle;

    public InternalES2BasePhongMaterial(ResourceFactory factory, long nativeHandle, ShaderMaterial material, Disposer.Record disposerRecord) {
        super(factory, material, disposerRecord);
        this.nativeHandle = nativeHandle;
    }

    public static InternalES2BasePhongMaterial create(ES2ResourceFactory rf, ShaderMaterial material) {
        final BaseShaderContext es2Context = Reflect.on(rf.getClass()).getFieldValue("context", rf);
        final long nativeHandle = Reflect.on(es2Context.getClass()).<Long>method("createES2PhongMaterial").invoke(es2Context);
        return new InternalES2BasePhongMaterial(rf, nativeHandle, material, new ES2PhongMaterialDisposerRecord(es2Context, nativeHandle));
    }

    public long getNativeHandle() {
        return this.nativeHandle;
    }

    public static class ES2PhongMaterialDisposerRecord implements Disposer.Record {

        private final BaseShaderContext context;
        private long nativeHandle;

        ES2PhongMaterialDisposerRecord(BaseShaderContext context, long nativeHandle) {
            this.context = context;
            this.nativeHandle = nativeHandle;
        }

        @Override
        public void dispose() {
            if (this.nativeHandle != 0L) {
                Reflect.on(this.context.getClass()).method("releaseES2PhongMaterial").invoke(this.context, this.nativeHandle);
                this.nativeHandle = 0L;
            }
        }
    }

}
