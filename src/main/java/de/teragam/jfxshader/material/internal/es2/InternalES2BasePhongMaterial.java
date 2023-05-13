package de.teragam.jfxshader.material.internal.es2;

import com.sun.prism.ResourceFactory;
import com.sun.prism.impl.Disposer;
import com.sun.prism.impl.ps.BaseShaderContext;

import de.teragam.jfxshader.exception.ShaderException;
import de.teragam.jfxshader.material.ShaderMaterial;
import de.teragam.jfxshader.material.internal.InternalBasePhongMaterial;
import de.teragam.jfxshader.util.Reflect;

public class InternalES2BasePhongMaterial extends InternalBasePhongMaterial {

    private final long nativeHandle;

    public InternalES2BasePhongMaterial(long nativeHandle, ShaderMaterial material, Disposer.Record disposerRecord) {
        super(material, disposerRecord);
        this.nativeHandle = nativeHandle;
    }

    public static InternalES2BasePhongMaterial create(ResourceFactory rf, ShaderMaterial material) {
        if (!Reflect.resolveClass("com.sun.prism.es2.ES2ResourceFactory").isInstance(rf)) {
            throw new ShaderException("Factory is not a ES2ResourceFactory");
        }
        final BaseShaderContext es2Context = Reflect.on(rf.getClass()).getFieldValue("context", rf);
        final long nativeHandle = Reflect.on(es2Context.getClass()).<Long>method("createES2PhongMaterial").invoke(es2Context);
        return new InternalES2BasePhongMaterial(nativeHandle, material, () -> {
            if (nativeHandle != 0L) {
                Reflect.on(es2Context.getClass()).method("releaseES2PhongMaterial").invoke(es2Context, nativeHandle);
            }
        });
    }

    public long getNativeHandle() {
        return this.nativeHandle;
    }

}
