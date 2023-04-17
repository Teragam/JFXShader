package de.teragam.jfxshader.internal.es2;

import java.nio.Buffer;

import com.sun.prism.PixelFormat;
import com.sun.prism.RTTexture;
import com.sun.prism.Texture;
import com.sun.prism.es2.ES2ResourceFactory;
import com.sun.prism.impl.BaseResourceFactory;
import com.sun.prism.impl.BaseTexture;
import com.sun.prism.impl.DisposerManagedResource;
import com.sun.prism.impl.PrismSettings;
import com.sun.prism.impl.PrismTrace;
import com.sun.prism.impl.TextureResourcePool;
import com.sun.prism.impl.ps.BaseShaderContext;

import de.teragam.jfxshader.internal.Reflect;
import de.teragam.jfxshader.internal.ReflectionTextureHelper;
import de.teragam.jfxshader.internal.TextureCreationException;

public class ES2RTTextureHelper {

    private static final int GL_TEXTURE_2D = 50;
    private static final int GL_LINEAR = 53;

    private ES2RTTextureHelper() {}

    public static Object getGLContext(BaseResourceFactory factory) {
        if (!(factory instanceof ES2ResourceFactory)) {
            throw new TextureCreationException("Factory is not a ES2ResourceFactory");
        }
        final BaseShaderContext context = ReflectionES2Helper.getInstance().getContext(factory);
        return Reflect.on(context.getClass()).getFieldValue("glContext", context);
    }

    public static RTTexture createES2RTTexture(BaseResourceFactory factory, PixelFormat format, Texture.WrapMode wrapMode, int width, int height,
                                               boolean useMipmap) {
        if (!(factory instanceof ES2ResourceFactory)) {
            throw new TextureCreationException("Factory is not a ES2ResourceFactory");
        }

        final BaseShaderContext es2Context = ReflectionES2Helper.getInstance().getContext(factory);
        final Object glContext = ES2RTTextureHelper.getGLContext(factory);
        final boolean pad;
        switch (wrapMode) {
            case CLAMP_NOT_NEEDED:
                pad = false;
                break;
            case CLAMP_TO_ZERO:
                pad = !(boolean) Reflect.on(glContext.getClass()).invokeMethod("canClampToZero").invoke(glContext);
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

        final int maxSize = (int) Reflect.on(glContext.getClass()).invokeMethod("getMaxTextureSize").invoke(glContext);
        int texWidth;
        int texHeight;
        if ((boolean) Reflect.on(glContext.getClass()).invokeMethod("canCreateNonPowTwoTextures").invoke(glContext)) {
            texWidth = (paddedW <= maxSize) ? paddedW : 0;
            texHeight = (paddedH <= maxSize) ? paddedH : 0;
        } else {
            texWidth = ES2RTTextureHelper.nextPowerOfTwo(paddedW, maxSize);
            texHeight = ES2RTTextureHelper.nextPowerOfTwo(paddedH, maxSize);
        }

        if (texWidth == 0 || texHeight == 0) {
            throw new TextureCreationException(
                    "Requested texture dimensions (" + width + "x" + height + ") "
                            + "require dimensions (" + texWidth + "x" + texHeight + ") "
                            + "that exceed maximum texture size (" + maxSize + ")");
        }

        texWidth = Math.max(texWidth, PrismSettings.minRTTSize);
        texHeight = Math.max(texHeight, PrismSettings.minRTTSize);
        final TextureResourcePool<?> es2VramPool = Reflect.on("com.sun.prism.es2.ES2VramPool").getFieldValue("instance", null);
        final long size = es2VramPool.estimateTextureSize(texWidth, texHeight, format);
        if (!es2VramPool.prepareForAllocation(size)) {
            throw new TextureCreationException("Failed to create texture: Not enough VRAM.");
        }

        Reflect.on(glContext.getClass()).invokeMethod("setActiveTextureUnit", int.class).invoke(glContext, 0);
        final int savedFBO = (int) Reflect.on(glContext.getClass()).invokeMethod("getBoundFBO").invoke(glContext);
        final int savedTex = (int) Reflect.on(glContext.getClass()).invokeMethod("getBoundTexture").invoke(glContext);

        final int nativeTexID = (int) Reflect.on(glContext.getClass()).invokeMethod("genAndBindTexture").invoke(glContext);
        if (nativeTexID == 0L) {
            throw new TextureCreationException("Failed to create texture.");
        }

        final boolean result = ReflectionES2Helper.getInstance()
                .uploadPixels(glContext, GL_TEXTURE_2D, null, format, texWidth, texHeight, contentX,
                        contentY, 0, 0, width, height, 0, true, useMipmap);
        if (!result) {
            throw new TextureCreationException("Failed to create texture.");
        }

        Reflect.on(glContext.getClass()).invokeMethod("texParamsMinMax").invoke(glContext, GL_LINEAR, useMipmap);

        final int nativeFBOID = (int) Reflect.on(glContext.getClass()).invokeMethod("createFBO").invoke(glContext, nativeTexID);
        if (nativeFBOID == 0) {
            Reflect.on(glContext.getClass()).invokeMethod("deleteTexture").invoke(glContext, nativeTexID);
            throw new TextureCreationException("Failed to attach FBO to texture.");
        }

        final int padding = pad ? 2 : 0;
        final int maxContentW = texWidth - padding;
        final int maxContentH = texHeight - padding;
        final Object texData = Reflect.on("com.sun.prism.es2.ES2RTTextureData").createInstance()
                .create(es2Context, nativeTexID, nativeFBOID, texWidth, texHeight, size);
        final DisposerManagedResource<?> texRes = (DisposerManagedResource<?>) Reflect.on("com.sun.prism.es2.ES2TextureResource")
                .createInstance(texData.getClass()).create(texData);
        final RTTexture es2RTT = ReflectionES2Helper.getInstance()
                .createTexture(es2Context, texRes, format, wrapMode, texWidth, texHeight, contentX, contentY, width, height, maxContentW, maxContentH,
                        useMipmap);

        Reflect.on(glContext.getClass()).invokeMethod("bindFBO").invoke(glContext, savedFBO);
        Reflect.on(glContext.getClass()).invokeMethod("setBoundTexture").invoke(glContext, savedTex);
        return es2RTT;
    }

    private static int nextPowerOfTwo(int val, int max) {
        if (val > max) {
            return 0;
        }
        int i = 1;
        while (i < val) {
            i *= 2;
        }
        return i;
    }

    private static class ReflectionES2Helper {

        private static ReflectionES2Helper instance;

        private final Reflect.MethodInvocationWrapper<Boolean> uploadPixelsMethod;

        public ReflectionES2Helper() {
            this.uploadPixelsMethod = Reflect.on("com.sun.prism.es2.ES2Texture").invokeMethod("uploadPixels",
                    Reflect.resolveClass("com.sun.prism.es2.GLContext"), int.class, Buffer.class,
                    PixelFormat.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, boolean.class,
                    boolean.class);
        }

        public static ReflectionES2Helper getInstance() {
            if (ReflectionES2Helper.instance == null) {
                ReflectionES2Helper.instance = new ReflectionES2Helper();
            }
            return ReflectionES2Helper.instance;
        }

        public BaseShaderContext getContext(BaseResourceFactory factory) {
            return Reflect.on(ES2ResourceFactory.class).getFieldValue("context", factory);
        }

        public boolean uploadPixels(Object glCtx, int target, Buffer pixels, PixelFormat format, int texw, int texh, int dstx, int dsty, int srcx, int srcy,
                                    int srcw, int srch, int srcscan, boolean create, boolean useMipmap) {
            return this.uploadPixelsMethod.invoke(null, glCtx, target, pixels, format, texw, texh, dstx, dsty, srcx, srcy, srcw, srch, srcscan,
                    create, useMipmap);
        }

        private RTTexture createTexture(BaseShaderContext es2Context, DisposerManagedResource<?> resource, PixelFormat format, Texture.WrapMode wrapMode,
                                        int physicalWidth, int physicalHeight, int contentX, int contentY, int contentWidth, int contentHeight,
                                        int maxContentWidth, int maxContentHeight, boolean useMipmap) {
            final RTTexture texture = Reflect.<RTTexture>on("com.sun.prism.es2.ES2RTTexture").allocateInstance();
            texture.setOpaque(false);
            Reflect.on("com.sun.prism.es2.ES2Texture").setFieldValue("context", texture, es2Context);
            ReflectionTextureHelper.fillTexture((BaseTexture<? super DisposerManagedResource<?>>) texture, resource, PixelFormat.INT_ARGB_PRE, wrapMode,
                    physicalWidth, physicalHeight, contentX, contentY, contentWidth, contentHeight, maxContentWidth, maxContentHeight, useMipmap);
            PrismTrace.rttCreated(Reflect.on("com.sun.prism.es2.ES2RTTextureData").getFieldValue("fboID", resource),
                    physicalWidth, physicalHeight, format.getBytesPerPixelUnit());
            return texture;
        }
    }
}
