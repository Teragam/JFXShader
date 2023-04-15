package de.teragam.jfxshader.internal;

import com.sun.prism.PixelFormat;
import com.sun.prism.Texture;
import com.sun.prism.impl.BaseTexture;
import com.sun.prism.impl.ManagedResource;

public class ReflectionTextureHelper {

    private ReflectionTextureHelper() {}

    public static <T extends ManagedResource<?>> void fillTexture(BaseTexture<T> texture, T resource, PixelFormat format, Texture.WrapMode wrapMode,
                                                                  int physicalWidth, int physicalHeight, int contentX, int contentY,
                                                                  int contentWidth, int contentHeight, int maxContentWidth, int maxContentHeight,
                                                                  boolean useMipmap) {
        try {
            ReflectionHelper.setFieldValue(BaseTexture.class, "resource", texture, resource);
            ReflectionHelper.setFieldValue(BaseTexture.class, "format", texture, format);
            ReflectionHelper.setFieldValue(BaseTexture.class, "wrapMode", texture, wrapMode);
            ReflectionHelper.setFieldValue(BaseTexture.class, "physicalWidth", texture, physicalWidth);
            ReflectionHelper.setFieldValue(BaseTexture.class, "physicalHeight", texture, physicalHeight);
            ReflectionHelper.setFieldValue(BaseTexture.class, "contentX", texture, contentX);
            ReflectionHelper.setFieldValue(BaseTexture.class, "contentY", texture, contentY);
            ReflectionHelper.setFieldValue(BaseTexture.class, "contentWidth", texture, contentWidth);
            ReflectionHelper.setFieldValue(BaseTexture.class, "contentHeight", texture, contentHeight);
            ReflectionHelper.setFieldValue(BaseTexture.class, "maxContentWidth", texture, maxContentWidth);
            ReflectionHelper.setFieldValue(BaseTexture.class, "maxContentHeight", texture, maxContentHeight);
            ReflectionHelper.setFieldValue(BaseTexture.class, "useMipmap", texture, useMipmap);
            texture.setLinearFiltering(true);
        } catch (ShaderException e) {
            throw new ShaderException("Could not fill texture", e);
        }
    }
}
