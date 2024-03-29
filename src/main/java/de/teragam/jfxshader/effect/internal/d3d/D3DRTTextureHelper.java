package de.teragam.jfxshader.effect.internal.d3d;

import com.sun.prism.PixelFormat;
import com.sun.prism.RTTexture;
import com.sun.prism.ResourceFactory;
import com.sun.prism.Texture;
import com.sun.prism.d3d.D3DTextureData;
import com.sun.prism.impl.BaseResourceFactory;
import com.sun.prism.impl.BaseResourcePool;
import com.sun.prism.impl.BaseTexture;
import com.sun.prism.impl.DisposerManagedResource;
import com.sun.prism.impl.PrismSettings;
import com.sun.prism.impl.ps.BaseShaderContext;

import de.teragam.jfxshader.effect.internal.RTTTextureHelper;
import de.teragam.jfxshader.exception.TextureCreationException;
import de.teragam.jfxshader.util.Reflect;

public final class D3DRTTextureHelper extends RTTTextureHelper {

    private D3DRTTextureHelper() {}

    public static RTTexture createD3DRTTexture(BaseResourceFactory factory, PixelFormat format, Texture.WrapMode wrapMode, int width, int height,
                                               boolean useMipmap) {
        final Class<?> d3DResourceFactoryClass = Reflect.resolveClass("com.sun.prism.d3d.D3DResourceFactory");
        if (!d3DResourceFactoryClass.isInstance(factory)) {
            throw new TextureCreationException("Factory is not a D3DResourceFactory");
        }
        if (factory.isDisposed()) {
            throw new TextureCreationException("Failed to create texture: The factory is disposed.");
        }
        final BaseResourcePool<D3DTextureData> d3dVramPool = Reflect.on("com.sun.prism.d3d.D3DVramPool").getFieldValue("instance", null);
        if (!d3dVramPool.prepareForAllocation(D3DRTTextureHelper.estimateTextureSize(width, height, format))) {
            throw new TextureCreationException("Failed to create texture: Not enough VRAM.");
        }

        int createWidth = width;
        int createHeight = height;
        if (PrismSettings.forcePow2) {
            createWidth = D3DRTTextureHelper.nextPowerOfTwo(createWidth);
            createHeight = D3DRTTextureHelper.nextPowerOfTwo(createHeight);
        }
        final BaseShaderContext context = Reflect.on(d3DResourceFactoryClass).getFieldValue("context", factory);
        final long contextHandle = Reflect.on("com.sun.prism.d3d.D3DContext").getFieldValue("pContext", context);
        final long pResource = (long) Reflect.on(d3DResourceFactoryClass).method("nCreateTexture")
                .invoke(null, contextHandle, format.ordinal(), Texture.Usage.DEFAULT.ordinal(), true, createWidth, createHeight, 0, useMipmap);

        if (pResource == 0L) {
            throw new TextureCreationException("Failed to create texture.");
        }

        final int textureWidth = (int) Reflect.on(d3DResourceFactoryClass).method("nGetTextureWidth").invoke(null, pResource);
        final int textureHeight = (int) Reflect.on(d3DResourceFactoryClass).method("nGetTextureHeight").invoke(null, pResource);
        final RTTexture rtt = Reflect.<RTTexture>on("com.sun.prism.d3d.D3DRTTexture").allocateInstance();
        final DisposerManagedResource<D3DTextureData> resource = Reflect.<DisposerManagedResource<D3DTextureData>>on("com.sun.prism.d3d.D3DTextureResource")
                .constructor().create(Reflect.on(D3DTextureData.class).constructor()
                        .create(context, pResource, true, textureWidth, textureHeight, format, 0));
        RTTTextureHelper.fillTexture((BaseTexture<DisposerManagedResource<D3DTextureData>>) rtt, resource, PixelFormat.INT_ARGB_PRE, wrapMode,
                textureWidth, textureHeight, 0, 0, width, height,
                textureWidth, textureHeight, useMipmap);
        rtt.setOpaque(false);
        rtt.createGraphics().clear();
        return rtt;
    }

    public static long getDevice(ResourceFactory factory) {
        final Class<?> d3DResourceFactoryClass = Reflect.resolveClass("com.sun.prism.d3d.D3DResourceFactory");
        if (!d3DResourceFactoryClass.isInstance(factory)) {
            throw new TextureCreationException("Factory is not a D3DResourceFactory");
        }
        final BaseShaderContext context = Reflect.on(d3DResourceFactoryClass).getFieldValue("context", factory);
        final long contextHandle = Reflect.on("com.sun.prism.d3d.D3DContext").getFieldValue("pContext", context);
        return (long) Reflect.on(d3DResourceFactoryClass).method("nGetDevice").invoke(null, contextHandle);
    }

    public static long estimateTextureSize(long width, long height, PixelFormat format) {
        return width * height * format.getBytesPerPixelUnit();
    }

    private static int nextPowerOfTwo(int val) {
        int i = 1;
        while (i < val) {
            i *= 2;
        }
        return i;
    }

}
