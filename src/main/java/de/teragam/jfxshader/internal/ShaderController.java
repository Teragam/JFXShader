package de.teragam.jfxshader.internal;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javafx.scene.effect.ShaderEffectBase;

import com.sun.glass.ui.Screen;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.prism.GraphicsPipeline;
import com.sun.prism.PixelFormat;
import com.sun.prism.RTTexture;
import com.sun.prism.ResourceFactory;
import com.sun.prism.Texture;
import com.sun.prism.es2.ES2ResourceFactory;
import com.sun.prism.es2.ES2Shader;
import com.sun.prism.impl.BaseResourceFactory;
import com.sun.prism.impl.ps.BaseShaderContext;
import com.sun.prism.impl.ps.BaseShaderFactory;
import com.sun.prism.ps.Shader;
import com.sun.prism.ps.ShaderFactory;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.ImageData;
import com.sun.scenario.effect.impl.Renderer;
import com.sun.scenario.effect.impl.state.RenderState;

import de.teragam.jfxshader.EffectDependencies;
import de.teragam.jfxshader.EffectPeer;
import de.teragam.jfxshader.EffectRenderer;
import de.teragam.jfxshader.IEffectRenderer;
import de.teragam.jfxshader.ShaderDeclaration;
import de.teragam.jfxshader.ShaderEffect;
import de.teragam.jfxshader.ShaderEffectPeer;
import de.teragam.jfxshader.internal.d3d.D3DRTTextureHelper;
import de.teragam.jfxshader.internal.d3d.D3DVertexShader;
import de.teragam.jfxshader.internal.d3d.IDirect3DDevice9;
import de.teragam.jfxshader.internal.es2.ES2RTTextureHelper;

public final class ShaderController {

    public static final int MAX_BOUND_TEXTURES = 16;

    private static final Map<Class<? extends ShaderEffectBase>, IEffectRenderer> EFFECT_RENDERER_MAP = Collections.synchronizedMap(new HashMap<>());

    private ShaderController() {}

    public static void register(FilterContext fctx, ShaderEffectBase effect) {
        final Renderer renderer = Renderer.getRenderer(Objects.requireNonNull(fctx, "FilterContext cannot be null"));
        try {
            final Map<String, com.sun.scenario.effect.impl.EffectPeer<?>> peerCache = Reflect.on(Renderer.class).getFieldValue("peerCache", renderer);
            final List<Class<? extends ShaderEffectPeer<?>>> dependencies = ShaderController.getPeerDependencies(effect.getClass());
            for (final Class<? extends ShaderEffectPeer<?>> peer : dependencies) {
                final EffectPeer peerConfig = ShaderController.getPeerConfig(peer);
                if (!peerCache.containsKey(peerConfig.value())) {
                    final ShaderEffectPeerConfig peerConfigInstance = new ShaderEffectPeerConfig(fctx, renderer, peerConfig.value(), peerConfig.targetFormat(),
                            peerConfig.targetWrapMode(), peerConfig.targetMipmaps(), peerConfig.targetPoolPolicy());
                    peerCache.put(peerConfig.value(), Reflect.on(peer).createInstance(ShaderEffectPeerConfig.class).create(peerConfigInstance));
                }
            }
        } catch (ShaderException e) {
            throw new ShaderCreationException("Could not inject custom shaders", e);
        }
    }

    public static Shader createShader(FilterContext fctx, ShaderDeclaration shaderDeclaration) {
        Objects.requireNonNull(shaderDeclaration, "ShaderDeclaration cannot be null");
        final InputStream shaderSource = ShaderController.isHLSLSupported() ? shaderDeclaration.d3dSource() : shaderDeclaration.es2Source();
        final ShaderFactory factory = ShaderController.getBaseShaderFactory(fctx);
        return factory.createShader(shaderSource, shaderDeclaration.samplers(), shaderDeclaration.params(),
                shaderDeclaration.samplers().keySet().size() - 1, true, false);
    }

    public static Shader createVertexShader(FilterContext fctx, ShaderDeclaration shaderDeclaration) {
        Objects.requireNonNull(shaderDeclaration, "ShaderDeclaration cannot be null");
        final ShaderFactory factory = ShaderController.getBaseShaderFactory(fctx);
        if (ShaderController.isHLSLSupported()) {
            final IDirect3DDevice9 device = MaterialController.getD3DDevice(factory);
            return new D3DVertexShader(device, D3DVertexShader.init(device, shaderDeclaration.d3dSource()), shaderDeclaration.params());
        }
        throw new ShaderCreationException("Standalone vertex shaders are not supported on OpenGL ES 2.0");
    }

    public static ES2Shader createES2ShaderProgram(FilterContext fctx, ShaderDeclaration vertexShaderDeclaration, ShaderDeclaration pixelShaderDeclaration,
                                                   Map<String, Integer> attributes) {
        Objects.requireNonNull(vertexShaderDeclaration, "VertexShaderDeclaration cannot be null");
        Objects.requireNonNull(pixelShaderDeclaration, "PixelShaderDeclaration cannot be null");
        Objects.requireNonNull(attributes, "Attributes cannot be null");
        final ShaderFactory factory = ShaderController.getBaseShaderFactory(fctx);
        if (ShaderController.isGLSLSupported()) {
            final BaseShaderContext es2Context = Reflect.on(ES2ResourceFactory.class).getFieldValue("context", factory);
            final String vertexShader = Reflect.on("com.sun.prism.es2.ES2Shader").<String>invokeMethod("readStreamIntoString")
                    .invoke(null, vertexShaderDeclaration.es2Source());
            return Reflect.on("com.sun.prism.es2.ES2Shader").<ES2Shader>invokeMethod("createFromSource")
                    .invoke(null, es2Context, vertexShader, pixelShaderDeclaration.es2Source(), pixelShaderDeclaration.samplers(), attributes, 1, false);
        } else {
            throw new ShaderCreationException("ES2 shader programs are not supported on DirectX 9.0");
        }
    }

    static IEffectRenderer getEffectRenderer(ShaderEffectBase effect) {
        if (ShaderController.EFFECT_RENDERER_MAP.containsKey(Objects.requireNonNull(effect, "Effect cannot be null").getClass())) {
            return ShaderController.EFFECT_RENDERER_MAP.get(effect.getClass());
        }
        final Class<? extends IEffectRenderer> rendererClass = effect.getClass().getAnnotation(EffectRenderer.class).value();
        try {
            final IEffectRenderer effectRenderer = Reflect.on(rendererClass).createInstance().create();
            ShaderController.EFFECT_RENDERER_MAP.put(effect.getClass(), effectRenderer);
            return effectRenderer;
        } catch (ShaderException e) {
            throw new ShaderCreationException("Could not create EffectRenderer", e);
        }
    }

    static List<Class<? extends ShaderEffectPeer<?>>> getPeerDependencies(Class<? extends ShaderEffectBase> effect) {
        if (Objects.requireNonNull(effect, "Effect cannot be null").isAnnotationPresent(EffectDependencies.class)) {
            return List.of(effect.getAnnotation(EffectDependencies.class).value());
        } else {
            throw new IllegalArgumentException(String.format("%s is not annotated with %s", effect, EffectDependencies.class));
        }
    }

    private static EffectPeer getPeerConfig(Class<? extends ShaderEffectPeer<?>> peer) {
        if (Objects.requireNonNull(peer, "Peer cannot be null").isAnnotationPresent(EffectPeer.class)) {
            return peer.getAnnotation(EffectPeer.class);
        } else {
            throw new IllegalArgumentException(String.format("%s is not annotated with %s", peer, EffectPeer.class));
        }
    }

    public static void ensureTextureCapacity(FilterContext fctx, BaseShaderContext shaderContext) {
        try {
            final BaseShaderContext.State state = Reflect.on(BaseShaderContext.class).getFieldValue("state", shaderContext);
            final Texture[] lastTextures = Reflect.on(BaseShaderContext.State.class).getFieldValue("lastTextures", state);
            if (lastTextures.length < MAX_BOUND_TEXTURES) {
                final Texture[] newTextures = new Texture[ShaderController.MAX_BOUND_TEXTURES];
                System.arraycopy(lastTextures, 0, newTextures, 0, lastTextures.length);
                Reflect.on(BaseShaderContext.State.class).setFieldValue("lastTextures", state, newTextures);
            }

            final BaseResourceFactory factory = ShaderController.getBaseShaderFactory(fctx);
            if (ShaderController.isGLSLSupported()) {
                final Object glContext = ES2RTTextureHelper.getGLContext(factory);
                final Reflect<?> glContextReflect = Reflect.on("com.sun.prism.es2.GLContext");
                final int[] boundTextures = glContextReflect.getFieldValue("boundTextures", glContext);
                if (boundTextures.length < MAX_BOUND_TEXTURES) {
                    final int[] newBoundTextures = new int[ShaderController.MAX_BOUND_TEXTURES];
                    System.arraycopy(boundTextures, 0, newBoundTextures, 0, boundTextures.length);
                    glContextReflect.setFieldValue("boundTextures", glContext, newBoundTextures);
                }
            }
        } catch (ShaderException e) {
            throw new ShaderCreationException("Could not ensure texture capacity", e);
        }
    }

    public static RTTexture createRTTexture(FilterContext fctx, PixelFormat formatHint, Texture.WrapMode wrapMode, int width, int height, boolean useMipmap) {
        final BaseResourceFactory factory = ShaderController.getBaseShaderFactory(fctx);
        if (ShaderController.isGLSLSupported()) {
            return ES2RTTextureHelper.createES2RTTexture(factory, formatHint, wrapMode, width, height, useMipmap);
        }
        return D3DRTTextureHelper.createD3DRTTexture(factory, formatHint, wrapMode, width, height, useMipmap);
    }

    public static <T extends ShaderEffect> ShaderEffectPeer<T> getPeerInstance(Class<? extends ShaderEffectPeer<T>> peerClass, FilterContext fctx) {
        final com.sun.scenario.effect.impl.EffectPeer<?> peer = Renderer.getRenderer(fctx)
                .getPeerInstance(fctx, ShaderController.getPeerConfig(peerClass).value(), -1);
        if (peer instanceof ShaderEffectPeer) {
            return (ShaderEffectPeer<T>) peer;
        } else {
            throw new ShaderCreationException(String.format("Peer %s is not a ShaderEffectPeer", peer));
        }
    }

    public static ImageData renderPeer(String peerName, InternalEffect effect, FilterContext fctx, BaseTransform transform, Rectangle outputClip,
                                       RenderState rstate, ImageData... inputs) {
        return Renderer.getRenderer(fctx).getPeerInstance(fctx, Objects.requireNonNull(peerName, "Peer name cannot be null"), -1)
                .filter(effect, rstate, transform, outputClip, inputs);
    }

    public static boolean isHLSLSupported() {
        final GraphicsPipeline pipe = GraphicsPipeline.getPipeline();
        return pipe != null && pipe.supportsShader(GraphicsPipeline.ShaderType.HLSL, GraphicsPipeline.ShaderModel.SM3);
    }

    public static boolean isGLSLSupported() {
        final GraphicsPipeline pipe = GraphicsPipeline.getPipeline();
        return pipe != null && pipe.supportsShader(GraphicsPipeline.ShaderType.GLSL, GraphicsPipeline.ShaderModel.SM3);
    }

    private static void validatePipeline(FilterContext fctx) {
        final Object ref = Objects.requireNonNull(fctx, "FilterContext cannot be null").getReferent();
        if (!(ref instanceof Screen)) {
            throw new ShaderCreationException("Invalid FilterContext");
        }
        final GraphicsPipeline pipe = GraphicsPipeline.getPipeline();
        if (pipe == null) {
            throw new ShaderCreationException("GraphicsPipeline is not initialized");
        }
        if (!ShaderController.isGLSLSupported() && !ShaderController.isHLSLSupported()) {
            throw new ShaderCreationException(String.format("Unsupported GraphicsPipeline (%s): Shaders are not supported", pipe.getClass().getSimpleName()));
        }
    }

    private static BaseShaderFactory getBaseShaderFactory(FilterContext fctx) {
        ShaderController.validatePipeline(fctx);
        final ResourceFactory factory = GraphicsPipeline.getPipeline().getResourceFactory((Screen) fctx.getReferent());
        if (factory instanceof BaseShaderFactory) {
            return (BaseShaderFactory) factory;
        } else {
            throw new ShaderCreationException(String.format("Unsupported ResourceFactory (%s): Shaders are not supported", factory.getClass().getSimpleName()));
        }
    }

}
