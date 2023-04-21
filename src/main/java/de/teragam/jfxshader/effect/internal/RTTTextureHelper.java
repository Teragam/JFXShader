package de.teragam.jfxshader.effect.internal;

import com.sun.prism.PixelFormat;
import com.sun.prism.Texture;
import com.sun.prism.impl.BaseTexture;
import com.sun.prism.impl.ManagedResource;

import de.teragam.jfxshader.exception.ShaderException;
import de.teragam.jfxshader.util.Reflect;

public class RTTTextureHelper {

    protected RTTTextureHelper() {}

    protected static <T extends ManagedResource<?>> void fillTexture(BaseTexture<T> texture, T resource, PixelFormat format, Texture.WrapMode wrapMode,
                                                                  int physicalWidth, int physicalHeight, int contentX, int contentY,
                                                                  int contentWidth, int contentHeight, int maxContentWidth, int maxContentHeight,
                                                                  boolean useMipmap) {
        try {
            final Reflect<?> reflect = Reflect.on(BaseTexture.class);
            reflect.setFieldValue("resource", texture, resource);
            reflect.setFieldValue("format", texture, format);
            reflect.setFieldValue("wrapMode", texture, wrapMode);
            reflect.setFieldValue("physicalWidth", texture, physicalWidth);
            reflect.setFieldValue("physicalHeight", texture, physicalHeight);
            reflect.setFieldValue("contentX", texture, contentX);
            reflect.setFieldValue("contentY", texture, contentY);
            reflect.setFieldValue("contentWidth", texture, contentWidth);
            reflect.setFieldValue("contentHeight", texture, contentHeight);
            reflect.setFieldValue("maxContentWidth", texture, maxContentWidth);
            reflect.setFieldValue("maxContentHeight", texture, maxContentHeight);
            reflect.setFieldValue("useMipmap", texture, useMipmap);
            texture.setLinearFiltering(true);
        } catch (ShaderException e) {
            throw new ShaderException("Could not fill texture", e);
        }
    }
}
