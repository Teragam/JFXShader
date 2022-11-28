package de.teragam.jfxshader.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.sun.prism.PixelFormat;
import com.sun.prism.Texture;
import com.sun.prism.impl.BaseTexture;
import com.sun.prism.impl.ManagedResource;

public class ReflectiveTextureHelper {

    private static ReflectiveTextureHelper instance;

    private final Object unsafe;
    private final Method allocateInstanceMethod;

    private final Field resourceField;
    private final Field formatField;
    private final Field wrapModeField;
    private final Field physicalWidthField;
    private final Field physicalHeightField;
    private final Field contentXField;
    private final Field contentYField;
    private final Field contentWidthField;
    private final Field contentHeightField;
    private final Field maxContentWidthField;
    private final Field maxContentHeightField;
    private final Field useMipmapField;

    private ReflectiveTextureHelper() {
        try {
            final Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            this.unsafe = setAccessible(unsafeClass.getDeclaredField("theUnsafe")).get(null);
            this.allocateInstanceMethod = unsafeClass.getDeclaredMethod("allocateInstance", Class.class);

            this.resourceField = setAccessible(BaseTexture.class.getDeclaredField("resource"));
            this.formatField = setAccessible(BaseTexture.class.getDeclaredField("format"));
            this.wrapModeField = setAccessible(BaseTexture.class.getDeclaredField("wrapMode"));
            this.physicalWidthField = setAccessible(BaseTexture.class.getDeclaredField("physicalWidth"));
            this.physicalHeightField = setAccessible(BaseTexture.class.getDeclaredField("physicalHeight"));
            this.contentXField = setAccessible(BaseTexture.class.getDeclaredField("contentX"));
            this.contentYField = setAccessible(BaseTexture.class.getDeclaredField("contentY"));
            this.contentWidthField = setAccessible(BaseTexture.class.getDeclaredField("contentWidth"));
            this.contentHeightField = setAccessible(BaseTexture.class.getDeclaredField("contentHeight"));
            this.maxContentWidthField = setAccessible(BaseTexture.class.getDeclaredField("maxContentWidth"));
            this.maxContentHeightField = setAccessible(BaseTexture.class.getDeclaredField("maxContentHeight"));
            this.useMipmapField = setAccessible(BaseTexture.class.getDeclaredField("useMipmap"));
        } catch (ReflectiveOperationException e) {
            throw new ShaderException("Could not initialize ReflectiveTextureHelper", e);
        }
    }

    public static ReflectiveTextureHelper getInstance() {
        if (ReflectiveTextureHelper.instance == null) {
            ReflectiveTextureHelper.instance = new ReflectiveTextureHelper();
        }
        return ReflectiveTextureHelper.instance;
    }

    public <T> T allocateInstance(Class<T> clazz) {
        try {
            return (T) this.allocateInstanceMethod.invoke(this.unsafe, clazz);
        } catch (ReflectiveOperationException e) {
            throw new ShaderException("Could not allocate instance of " + clazz, e);
        }
    }

    public <T extends ManagedResource<?>> void fillTexture(BaseTexture<T> texture, T resource, PixelFormat format, Texture.WrapMode wrapMode,
                                                           int physicalWidth, int physicalHeight, int contentX, int contentY,
                                                           int contentWidth, int contentHeight, int maxContentWidth, int maxContentHeight, boolean useMipmap) {
        try {
            this.resourceField.set(texture, resource);
            this.formatField.set(texture, format);
            this.wrapModeField.set(texture, wrapMode);
            this.physicalWidthField.set(texture, physicalWidth);
            this.physicalHeightField.set(texture, physicalHeight);
            this.contentXField.set(texture, contentX);
            this.contentYField.set(texture, contentY);
            this.contentWidthField.set(texture, contentWidth);
            this.contentHeightField.set(texture, contentHeight);
            this.maxContentWidthField.set(texture, maxContentWidth);
            this.maxContentHeightField.set(texture, maxContentHeight);
            this.useMipmapField.set(texture, useMipmap);
            texture.setLinearFiltering(true);
        } catch (ReflectiveOperationException e) {
            throw new ShaderException("Could not fill texture", e);
        }
    }

    private static Field setAccessible(Field field) {
        field.setAccessible(true);
        return field;
    }

}
