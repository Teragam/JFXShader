package com.sun.prism.es2;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.Buffer;

import com.sun.prism.PixelFormat;
import com.sun.prism.Texture;
import com.sun.prism.impl.BaseResourceFactory;
import com.sun.prism.impl.PrismSettings;
import com.sun.prism.impl.PrismTrace;

import de.teragam.jfxshader.internal.ReflectiveTextureHelper;
import de.teragam.jfxshader.internal.TextureCreationException;

public class ES2RTTextureHelper {

    private ES2RTTextureHelper() {}

    public static GLContext getGLContext(BaseResourceFactory factory) {
        if (!(factory instanceof ES2ResourceFactory)) {
            throw new TextureCreationException("Factory is not a ES2ResourceFactory");
        }
        final ES2Context context = ReflectiveHelper.getInstance().getContext(factory);
        return context.getGLContext();
    }

    public static ES2RTTexture createES2RTTexture(BaseResourceFactory factory, PixelFormat format, Texture.WrapMode wrapMode, int width, int height,
                                                  boolean useMipmap) {
        if (!(factory instanceof ES2ResourceFactory)) {
            throw new TextureCreationException("Factory is not a ES2ResourceFactory");
        }

        final ES2Context context = ReflectiveHelper.getInstance().getContext(factory);
        final GLContext glContext = context.getGLContext();
        final boolean pad;
        switch (wrapMode) {
            case CLAMP_NOT_NEEDED:
                pad = false;
                break;
            case CLAMP_TO_ZERO:
                pad = !glContext.canClampToZero();
                break;
            default:
            case CLAMP_TO_EDGE:
            case REPEAT:
                throw new IllegalArgumentException("wrap mode not supported for RT textures: " + wrapMode);
            case CLAMP_TO_EDGE_SIMULATED:
            case CLAMP_TO_ZERO_SIMULATED:
            case REPEAT_SIMULATED:
                throw new IllegalArgumentException("Cannot request simulated wrap mode: " + wrapMode);
        }

        final int contentX;
        final int contentY;
        final int paddedW;
        final int paddedH;
        if (pad) {
            contentX = 1;
            contentY = 1;
            paddedW = width + 2;
            paddedH = height + 2;
            wrapMode = wrapMode.simulatedVersion();
        } else {
            contentX = 0;
            contentY = 0;
            paddedW = width;
            paddedH = height;
        }

        final int maxSize = glContext.getMaxTextureSize();
        int texWidth;
        int texHeight;
        if (glContext.canCreateNonPowTwoTextures()) {
            texWidth = (paddedW <= maxSize) ? paddedW : 0;
            texHeight = (paddedH <= maxSize) ? paddedH : 0;
        } else {
            texWidth = ES2Texture.nextPowerOfTwo(paddedW, maxSize);
            texHeight = ES2Texture.nextPowerOfTwo(paddedH, maxSize);
        }

        if (texWidth == 0 || texHeight == 0) {
            throw new TextureCreationException(
                    "Requested texture dimensions (" + width + "x" + height + ") "
                            + "require dimensions (" + texWidth + "x" + texHeight + ") "
                            + "that exceed maximum texture size (" + maxSize + ")");
        }

        texWidth = Math.max(texWidth, PrismSettings.minRTTSize);
        texHeight = Math.max(texHeight, PrismSettings.minRTTSize);

        final long size = ES2VramPool.instance.estimateTextureSize(texWidth, texHeight, format);
        if (!ES2VramPool.instance.prepareForAllocation(size)) {
            throw new TextureCreationException("Failed to create texture: Not enough VRAM.");
        }

        glContext.setActiveTextureUnit(0);
        final int savedFBO = glContext.getBoundFBO();
        final int savedTex = glContext.getBoundTexture();

        final int nativeTexID = glContext.genAndBindTexture();
        if (nativeTexID == 0L) {
            throw new TextureCreationException("Failed to create texture.");
        }

        final boolean result = ReflectiveHelper.getInstance()
                .uploadPixels(glContext, GLContext.GL_TEXTURE_2D, null, format, texWidth, texHeight, contentX,
                        contentY, 0, 0, width, height, 0, true, useMipmap);
        if (!result) {
            throw new TextureCreationException("Failed to create texture.");
        }

        glContext.texParamsMinMax(GLContext.GL_LINEAR, useMipmap);

        final int nativeFBOID = glContext.createFBO(nativeTexID);
        if (nativeFBOID == 0) {
            glContext.deleteTexture(nativeTexID);
            throw new TextureCreationException("Failed to attach FBO to texture.");
        }

        final int padding = pad ? 2 : 0;
        final int maxContentW = texWidth - padding;
        final int maxContentH = texHeight - padding;
        final ES2RTTextureData texData = new ES2RTTextureData(context, nativeTexID, nativeFBOID, texWidth, texHeight, size);
        final ES2TextureResource<ES2RTTextureData> texRes = new ES2TextureResource<>(texData);
        final ES2RTTexture es2RTT = ReflectiveHelper.getInstance()
                .createTexture(context, texRes, format, wrapMode, texWidth, texHeight, contentX, contentY, width, height, maxContentW, maxContentH, useMipmap);

        glContext.bindFBO(savedFBO);
        glContext.setBoundTexture(savedTex);
        return es2RTT;
    }

    private static class ReflectiveHelper {

        private static ReflectiveHelper instance;

        private final Field contextField;
        private final Field factoryContextField;
        private final Method uploadPixelsMethod;

        public ReflectiveHelper() {
            try {
                this.contextField = ES2Texture.class.getDeclaredField("context");
                this.contextField.setAccessible(true);
                this.factoryContextField = ES2ResourceFactory.class.getDeclaredField("context");
                this.factoryContextField.setAccessible(true);
                this.uploadPixelsMethod = ES2Texture.class.getDeclaredMethod("uploadPixels", GLContext.class, int.class, Buffer.class, PixelFormat.class,
                        int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, boolean.class, boolean.class);
                this.uploadPixelsMethod.setAccessible(true);
            } catch (ReflectiveOperationException e) {
                throw new TextureCreationException("Cannot create RTT texture", e);
            }
        }

        public static ReflectiveHelper getInstance() {
            if (ReflectiveHelper.instance == null) {
                ReflectiveHelper.instance = new ReflectiveHelper();
            }
            return ReflectiveHelper.instance;
        }

        public ES2Context getContext(BaseResourceFactory factory) {
            try {
                return (ES2Context) this.factoryContextField.get(factory);
            } catch (ReflectiveOperationException e) {
                throw new TextureCreationException("Could not get context", e);
            }
        }

        public boolean uploadPixels(GLContext glCtx, int target, Buffer pixels, PixelFormat format, int texw, int texh, int dstx, int dsty, int srcx, int srcy,
                                    int srcw, int srch, int srcscan, boolean create, boolean useMipmap) {
            try {
                return (boolean) this.uploadPixelsMethod.invoke(null, glCtx, target, pixels, format, texw, texh, dstx, dsty, srcx, srcy, srcw, srch, srcscan,
                        create, useMipmap);
            } catch (ReflectiveOperationException e) {
                throw new TextureCreationException("Could not upload pixels", e);
            }
        }

        private ES2RTTexture createTexture(ES2Context context, ES2TextureResource<ES2RTTextureData> resource, PixelFormat format, Texture.WrapMode wrapMode,
                                           int physicalWidth, int physicalHeight, int contentX, int contentY, int contentWidth, int contentHeight,
                                           int maxContentWidth, int maxContentHeight, boolean useMipmap) {
            try {
                final ES2RTTexture texture = ReflectiveTextureHelper.getInstance().allocateInstance(ES2RTTexture.class);
                texture.setOpaque(false);
                this.contextField.set(texture, context);
                ReflectiveTextureHelper.getInstance().fillTexture(texture, resource, PixelFormat.INT_ARGB_PRE, wrapMode, physicalWidth, physicalHeight,
                        contentX, contentY, contentWidth, contentHeight, maxContentWidth, maxContentHeight, useMipmap);
                PrismTrace.rttCreated(resource.getResource().getFboID(),
                        physicalWidth, physicalHeight,
                        format.getBytesPerPixelUnit());
                return texture;
            } catch (ReflectiveOperationException e) {
                throw new TextureCreationException("Cannot create RTT texture", e);
            }
        }
    }
}
