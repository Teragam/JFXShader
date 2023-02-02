package de.teragam.jfxshader.internal;

import com.sun.prism.PixelFormat;
import com.sun.prism.Texture;
import com.sun.prism.impl.BaseTexture;
import com.sun.prism.impl.ManagedResource;

public class ReflectionTextureHelper {

    private static ReflectionTextureHelper instance;

    private final Object unsafe;
    private final ReflectionHelper.MethodInvocationWrapper<?> allocateInstanceMethod;

    private ReflectionTextureHelper() {
        try {
            final Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            this.unsafe = ReflectionHelper.getFieldValue(unsafeClass, "theUnsafe", null);
            this.allocateInstanceMethod = ReflectionHelper.invokeMethod(unsafeClass, "allocateInstance", Class.class);
        } catch (ReflectiveOperationException e) {
            throw new ShaderException("Could not initialize ReflectiveTextureHelper", e);
        }
    }

    public static ReflectionTextureHelper getInstance() {
        if (ReflectionTextureHelper.instance == null) {
            ReflectionTextureHelper.instance = new ReflectionTextureHelper();
        }
        return ReflectionTextureHelper.instance;
    }

    public <T> T allocateInstance(Class<T> clazz) {
        return (T) this.allocateInstanceMethod.invoke(this.unsafe, clazz);
    }

    public <T extends ManagedResource<?>> void fillTexture(BaseTexture<T> texture, T resource, PixelFormat format, Texture.WrapMode wrapMode,
                                                           int physicalWidth, int physicalHeight, int contentX, int contentY,
                                                           int contentWidth, int contentHeight, int maxContentWidth, int maxContentHeight, boolean useMipmap) {
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
