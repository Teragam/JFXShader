package de.teragam.jfxshader;

import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.nio.FloatBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javafx.scene.Node;
import javafx.scene.effect.Effect;
import javafx.scene.image.Image;

import com.sun.glass.ui.Screen;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.BoundsAccessor;
import com.sun.javafx.tk.Toolkit;
import com.sun.javafx.util.Utils;
import com.sun.prism.GraphicsPipeline;
import com.sun.prism.PixelFormat;
import com.sun.prism.RTTexture;
import com.sun.prism.ResourceFactory;
import com.sun.prism.Texture;
import com.sun.prism.impl.BaseResourceFactory;
import com.sun.prism.impl.ps.BaseShaderContext;
import com.sun.prism.impl.ps.BaseShaderFactory;
import com.sun.prism.ps.Shader;
import com.sun.prism.ps.ShaderFactory;
import com.sun.scenario.effect.EffectHelper;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.Filterable;
import com.sun.scenario.effect.ImageData;
import com.sun.scenario.effect.impl.BufferUtil;
import com.sun.scenario.effect.impl.Renderer;
import com.sun.scenario.effect.impl.state.RenderState;

import de.teragam.jfxshader.effect.EffectDependencies;
import de.teragam.jfxshader.effect.EffectPeer;
import de.teragam.jfxshader.effect.EffectRenderer;
import de.teragam.jfxshader.effect.IEffectRenderer;
import de.teragam.jfxshader.effect.InternalEffect;
import de.teragam.jfxshader.effect.ShaderEffect;
import de.teragam.jfxshader.effect.ShaderEffectPeer;
import de.teragam.jfxshader.effect.ShaderEffectPeerConfig;
import de.teragam.jfxshader.effect.internal.PPSMultiSamplerPeer;
import de.teragam.jfxshader.effect.internal.FilterableTexture;
import de.teragam.jfxshader.effect.internal.ShaderEffectBase;
import de.teragam.jfxshader.effect.internal.d3d.D3DRTTextureHelper;
import de.teragam.jfxshader.effect.internal.es2.ES2RTTextureHelper;
import de.teragam.jfxshader.exception.ShaderCreationException;
import de.teragam.jfxshader.exception.ShaderException;
import de.teragam.jfxshader.material.internal.d3d.D3DVertexShader;
import de.teragam.jfxshader.material.internal.d3d.Direct3DDevice9;
import de.teragam.jfxshader.util.Reflect;

public final class ShaderController {

    public static final int MAX_BOUND_TEXTURES = 16;

    private static final Map<Class<? extends ShaderEffect>, IEffectRenderer> EFFECT_RENDERER_MAP = Collections.synchronizedMap(new HashMap<>());
    private static final FloatBuffer tmpBuf = BufferUtil.newFloatBuffer(16);

    private ShaderController() {}

    public static void injectEffectAccessor() {
        Utils.forceInit(Effect.class);
        final EffectHelper.EffectAccessor accessor = Reflect.on(EffectHelper.class).getFieldValue("effectAccessor", null);
        if (Proxy.isProxyClass(accessor.getClass())) {
            return;
        }
        final Object proxy = Proxy.newProxyInstance(accessor.getClass().getClassLoader(), accessor.getClass().getInterfaces(), (p, method, args) -> {
            if ("copy".equals(method.getName()) && args[0] instanceof ShaderEffectBase) {
                return ((ShaderEffectBase) args[0]).getJFXShaderEffect().copy().getFXEffect();
            } else if ("getBounds".equals(method.getName())) {
                return ShaderEffectBase.getBounds((Effect) args[0], (BaseBounds) args[1], (BaseTransform) args[2], (Node) args[3], (BoundsAccessor) args[4]);
            } else {
                return method.invoke(accessor, args);
            }
        });
        Reflect.on(EffectHelper.class).setFieldValue("effectAccessor", null, proxy);
    }

    public static void register(FilterContext fctx, ShaderEffect effect) {
        final Renderer renderer = Renderer.getRenderer(Objects.requireNonNull(fctx, "FilterContext cannot be null"));
        ShaderController.validatePipeline(fctx);
        try {
            final Map<String, com.sun.scenario.effect.impl.EffectPeer<? super RenderState>> peerCache = Reflect.on(Renderer.class)
                    .getFieldValue("peerCache", renderer);
            final List<Class<? extends ShaderEffectPeer<?>>> dependencies = ShaderController.getPeerDependencies(effect.getClass());
            for (final Class<? extends ShaderEffectPeer<?>> peer : dependencies) {
                final EffectPeer peerConfig = ShaderController.getPeerConfig(peer);
                final String peerName = peerConfig.singleton() ? peerConfig.value() : String.format("%s-%s", peerConfig.value(),
                        ((ShaderEffectBase) effect.getFXEffect()).getEffectID());
                if (!peerCache.containsKey(peerName)) {
                    final ShaderEffectPeerConfig peerConfigInstance = new ShaderEffectPeerConfig(fctx, renderer, peerName, peerConfig.targetFormat(),
                            peerConfig.targetWrapMode(), peerConfig.targetMipmaps(), peerConfig.targetPoolPolicy());
                    if (Reflect.on(peer).hasConstructor(effect.getClass(), ShaderEffectPeerConfig.class)) {
                        if (peerConfig.singleton()) {
                            throw new ShaderCreationException("The ShaderEffect instance cannot be provided to a singleton ShaderEffectPeer");
                        } else {
                            peerCache.put(peerName, Reflect.on(peer).constructor(effect.getClass(), ShaderEffectPeerConfig.class)
                                    .create(effect, peerConfigInstance).getFXEffectPeer());
                        }
                    } else {
                        peerCache.put(peerName, Reflect.on(peer).constructor(ShaderEffectPeerConfig.class).create(peerConfigInstance).getFXEffectPeer());
                    }
                }
                ((ShaderEffectBase) effect.getFXEffect()).getPeerMap().putIfAbsent(peerName, peerCache.get(peerName));
            }
        } catch (ShaderException e) {
            throw new ShaderCreationException("Could not inject custom shaders", e);
        }
    }

    public static JFXShader createShader(FilterContext fctx, ShaderDeclaration shaderDeclaration, String shaderName) {
        Objects.requireNonNull(shaderDeclaration, "ShaderDeclaration cannot be null");
        final InputStream shaderSource = ShaderController.isHLSLSupported() ? shaderDeclaration.d3dSource() : shaderDeclaration.es2Source();
        final ShaderFactory factory = ShaderController.getBaseShaderFactory(fctx);
        final Reflect<? extends ShaderFactory> factoryReflect = Reflect.on(factory.getClass());
        final Shader shader;
        if (factoryReflect.hasMethod("createShader", String.class, InputStream.class, Map.class, Map.class, int.class, boolean.class, boolean.class)) {
            // JavaFX 24 and above
            shader = factoryReflect.<Shader>method("createShader", String.class, InputStream.class, Map.class, Map.class, int.class, boolean.class,
                    boolean.class).invoke(factory, shaderName, shaderSource, shaderDeclaration.samplers(), shaderDeclaration.params(),
                    shaderDeclaration.samplers().size() - 1, true, false);
        } else {
            // JavaFX 23 and below
            shader = factoryReflect.<Shader>method("createShader", InputStream.class, Map.class, Map.class, int.class, boolean.class, boolean.class)
                    .invoke(factory, shaderSource, shaderDeclaration.samplers(), shaderDeclaration.params(),
                            shaderDeclaration.samplers().size() - 1, true, false);
        }
        if (shader == null) {
            return null;
        }
        final Reflect<? extends Shader> shaderReflect = Reflect.on(shader.getClass());
        return Reflect.createProxy(shader, JFXShader.class, (proxy, method, args) -> {
            if ("setMatrix".equals(method.getName())) {
                if (ShaderController.isGLSLSupported()) {
                    return shaderReflect.method(method.getName(), String.class, float[].class)
                            .invoke(shader, args[0], args[1]);
                } else {
                    tmpBuf.clear();
                    tmpBuf.put((float[]) args[1]);
                    tmpBuf.rewind();
                    shader.setConstants((String) args[0], tmpBuf, 0, (int) args[2]);
                    return null;
                }
            }
            return shaderReflect.method(method.getName(), method.getParameterTypes()).invoke(shader, args);
        });
    }

    public static JFXShader createVertexShader(FilterContext fctx, ShaderDeclaration shaderDeclaration) {
        Objects.requireNonNull(shaderDeclaration, "ShaderDeclaration cannot be null");
        final ShaderFactory factory = ShaderController.getBaseShaderFactory(fctx);
        if (ShaderController.isHLSLSupported()) {
            final Direct3DDevice9 device = MaterialController.getD3DDevice(factory);
            return new D3DVertexShader(device, D3DVertexShader.init(device, shaderDeclaration.d3dSource()), shaderDeclaration.params());
        }
        throw new ShaderCreationException("Standalone vertex shaders are not supported on OpenGL ES 2.0");
    }

    public static JFXShader createES2ShaderProgram(FilterContext fctx, ShaderDeclaration vertexShaderDeclaration, ShaderDeclaration pixelShaderDeclaration,
                                                   Map<String, Integer> attributes) {
        Objects.requireNonNull(vertexShaderDeclaration, "VertexShaderDeclaration cannot be null");
        Objects.requireNonNull(pixelShaderDeclaration, "PixelShaderDeclaration cannot be null");
        Objects.requireNonNull(attributes, "Attributes cannot be null");
        final ShaderFactory factory = ShaderController.getBaseShaderFactory(fctx);
        if (ShaderController.isGLSLSupported()) {
            final BaseShaderContext es2Context = Reflect.on("com.sun.prism.es2.ES2ResourceFactory").getFieldValue("context", factory);
            final String vertexShader = Reflect.on("com.sun.prism.es2.ES2Shader").<String>method("readStreamIntoString")
                    .invoke(null, Objects.requireNonNull(vertexShaderDeclaration.es2Source(), "ES2 vertex shader source cannot be null"));
            final Shader es2Shader = Reflect.on("com.sun.prism.es2.ES2Shader")
                    .<Shader>method("createFromSource", Reflect.resolveClass("com.sun.prism.es2.ES2Context"),
                            String.class, InputStream.class, Map.class, Map.class, int.class, boolean.class)
                    .invoke(null, es2Context, vertexShader,
                            Objects.requireNonNull(pixelShaderDeclaration.es2Source(), "ES2 pixel shader source cannot be null"),
                            Objects.requireNonNull(pixelShaderDeclaration.samplers(), "ES2 pixel shader samplers cannot be null"), attributes, 1, true);
            final Reflect<? extends Shader> shaderReflect = Reflect.on(es2Shader.getClass());
            return Reflect.createProxy(es2Shader, JFXShader.class, (proxy, method, args) -> {
                if ("setMatrix".equals(method.getName())) {
                    return shaderReflect.method("setMatrix", String.class, float[].class)
                            .invoke(es2Shader, args[0], args[1]);
                }
                return shaderReflect.method(method.getName(), method.getParameterTypes()).invoke(es2Shader, args);
            });
        } else {
            throw new ShaderCreationException("ES2 shader programs are not supported on DirectX 9.0");
        }
    }

    public static IEffectRenderer getEffectRenderer(ShaderEffect effect) {
        if (ShaderController.EFFECT_RENDERER_MAP.containsKey(Objects.requireNonNull(effect, "Effect cannot be null").getClass())) {
            return ShaderController.EFFECT_RENDERER_MAP.get(effect.getClass());
        }
        final Class<? extends IEffectRenderer> rendererClass = effect.getClass().getAnnotation(EffectRenderer.class).value();
        try {
            final IEffectRenderer effectRenderer = Reflect.on(rendererClass).constructor().create();
            ShaderController.EFFECT_RENDERER_MAP.put(effect.getClass(), effectRenderer);
            return effectRenderer;
        } catch (ShaderException e) {
            throw new ShaderCreationException("Could not create EffectRenderer", e);
        }
    }

    public static List<Class<? extends ShaderEffectPeer<?>>> getPeerDependencies(Class<? extends ShaderEffect> effect) {
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

    public static ImageData createImageData(FilterContext fctx, Texture texture, Rectangle bounds, BaseTransform transform) {
        return new ImageData(fctx, new FilterableTexture(texture), bounds, transform);
    }

    public static ImageData createImageData(FilterContext fctx, Image image, BaseTransform transform) {
        if (image == null) {
            final Filterable f = com.sun.scenario.effect.Effect.getCompatibleImage(fctx, 1, 1);
            return new ImageData(fctx, f, new Rectangle(1, 1));
        }
        final com.sun.prism.Image prismImage = (com.sun.prism.Image) Toolkit.getImageAccessor().getPlatformImage(image);
        if (prismImage == null) {
            final Filterable f = com.sun.scenario.effect.Effect.getCompatibleImage(fctx, 1, 1);
            return new ImageData(fctx, f, new Rectangle(1, 1));
        }
        final BaseResourceFactory factory = ShaderController.getBaseShaderFactory(fctx);
        final Texture texture = factory.getCachedTexture(prismImage, Texture.WrapMode.CLAMP_TO_EDGE);
        return new ImageData(fctx, new FilterableTexture(texture), new Rectangle((int) image.getWidth(), (int) image.getHeight()), transform);
    }

    public static <T extends ShaderEffect> ShaderEffectPeer<T> getPeerInstance(Class<? extends ShaderEffectPeer<T>> peerClass, FilterContext fctx) {
        final com.sun.scenario.effect.impl.EffectPeer<?> peer = Renderer.getRenderer(fctx)
                .getPeerInstance(fctx, ShaderController.getPeerConfig(peerClass).value(), -1);
        if (peer instanceof PPSMultiSamplerPeer) {
            return (ShaderEffectPeer<T>) ((PPSMultiSamplerPeer<?, ?>) peer).getParentPeer();
        } else {
            throw new ShaderCreationException(String.format("Peer %s is not a PPSMultiSamplerPeer", peer));
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
