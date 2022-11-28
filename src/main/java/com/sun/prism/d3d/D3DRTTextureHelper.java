package com.sun.prism.d3d;

import com.sun.prism.PixelFormat;
import com.sun.prism.Texture;
import com.sun.prism.impl.BaseResourceFactory;
import com.sun.prism.impl.PrismSettings;

import de.teragam.jfxshader.internal.ReflectiveTextureHelper;
import de.teragam.jfxshader.internal.TextureCreationException;

public class D3DRTTextureHelper {

    private D3DRTTextureHelper() {}

    public static D3DRTTexture createD3DRTTexture(BaseResourceFactory factory, PixelFormat format, Texture.WrapMode wrapMode, int width, int height,
                                                  boolean useMipmap) {
        if (!(factory instanceof D3DResourceFactory)) {
            throw new TextureCreationException("Factory is not a D3DResourceFactory");
        }
        if (factory.isDisposed()) {
            throw new TextureCreationException("Failed to create texture: The factory is disposed.");
        }
        if (!D3DVramPool.instance.prepareForAllocation(D3DVramPool.instance.estimateTextureSize(width, height, format))) {
            throw new TextureCreationException("Failed to create texture: Not enough VRAM.");
        }

        int createWidth = width;
        int createHeight = height;
        if (PrismSettings.forcePow2) {
            createWidth = D3DResourceFactory.nextPowerOfTwo(createWidth, Integer.MAX_VALUE);
            createHeight = D3DResourceFactory.nextPowerOfTwo(createHeight, Integer.MAX_VALUE);
        }

        final D3DContext context = ((D3DResourceFactory) factory).getContext();
        final long pResource = D3DResourceFactory.nCreateTexture(context.getContextHandle(), format.ordinal(),
                Texture.Usage.DEFAULT.ordinal(), true, createWidth, createHeight, 0, useMipmap);

        if (pResource == 0L) {
            throw new TextureCreationException("Failed to create texture.");
        }

        final int textureWidth = D3DResourceFactory.nGetTextureWidth(pResource);
        final int textureHeight = D3DResourceFactory.nGetTextureHeight(pResource);
        final D3DRTTexture rtt = ReflectiveTextureHelper.getInstance().allocateInstance(D3DRTTexture.class);
        final D3DTextureResource resource = new D3DTextureResource(new D3DTextureData(context, pResource, true, textureWidth, textureHeight, format, 0));
        ReflectiveTextureHelper.getInstance().fillTexture(rtt, resource, PixelFormat.INT_ARGB_PRE, wrapMode, textureWidth, textureHeight, 0, 0, width, height,
                textureWidth, textureHeight, useMipmap);
        rtt.setOpaque(false);
        rtt.createGraphics().clear();
        return rtt;
    }

}
