package de.teragam.jfxshader.internal;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import javafx.scene.effect.ShaderEffectBase;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Shape3D;

import com.sun.glass.ui.Screen;
import com.sun.glass.utils.NativeLibLoader;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.shape.BoxHelper;
import com.sun.javafx.scene.shape.CylinderHelper;
import com.sun.javafx.scene.shape.MeshViewHelper;
import com.sun.javafx.scene.shape.Shape3DHelper;
import com.sun.javafx.scene.shape.SphereHelper;
import com.sun.javafx.sg.prism.NGShape3D;
import com.sun.javafx.util.Utils;
import com.sun.prism.GraphicsPipeline;
import com.sun.prism.PixelFormat;
import com.sun.prism.RTTexture;
import com.sun.prism.ResourceFactory;
import com.sun.prism.Texture;
import com.sun.prism.d3d.D3DRTTextureHelper;
import com.sun.prism.es2.ES2RTTextureHelper;
import com.sun.prism.impl.BaseResourceFactory;
import com.sun.prism.impl.ps.BaseShaderContext;
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
import de.teragam.jfxshader.internal.d3d.IDirect3DDevice9;
import de.teragam.jfxshader.material.internal.InternalNGBox;
import de.teragam.jfxshader.material.internal.InternalNGCylinder;
import de.teragam.jfxshader.material.internal.InternalNGMeshView;
import de.teragam.jfxshader.material.internal.InternalNGSphere;

public final class ShaderController {

    public static final int MAX_BOUND_TEXTURES = 16;

    private static final Map<Class<? extends ShaderEffectBase>, IEffectRenderer> EFFECT_RENDERER_MAP = Collections.synchronizedMap(new HashMap<>());
    private static final Map<Long, IDirect3DDevice9> D3D_DEVICE_MAP = new HashMap<>();
    private static Field boundTexturesField;

    private ShaderController() {}

    public static void register(FilterContext fctx, ShaderEffectBase effect) {
        final Renderer renderer = Renderer.getRenderer(Objects.requireNonNull(fctx, "FilterContext cannot be null"));
        try {
            final Map<String, com.sun.scenario.effect.impl.EffectPeer<?>> peerCache = ReflectionHelper.getFieldValue(Renderer.class, "peerCache", renderer);
            final List<Class<? extends ShaderEffectPeer<?>>> dependencies = ShaderController.getPeerDependencies(effect.getClass());
            for (final Class<? extends ShaderEffectPeer<?>> peer : dependencies) {
                final EffectPeer peerConfig = ShaderController.getPeerConfig(peer);
                if (!peerCache.containsKey(peerConfig.value())) {
                    final Constructor<? extends ShaderEffectPeer<?>> constructor = peer.getDeclaredConstructor(ShaderEffectPeerConfig.class);
                    constructor.setAccessible(true);
                    peerCache.put(peerConfig.value(), constructor.newInstance(
                            new ShaderEffectPeerConfig(fctx, renderer, peerConfig.value(), peerConfig.targetFormat(), peerConfig.targetWrapMode(),
                                    peerConfig.targetMipmaps(), peerConfig.targetPoolPolicy())));
                }
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | InstantiationException e) {
            throw new ShaderCreationException("Could not inject custom shaders", e);
        }
    }

    public static Shader createShader(FilterContext fctx, ShaderDeclaration shaderDeclaration) {
        Objects.requireNonNull(shaderDeclaration, "ShaderDeclaration cannot be null");
        final Object ref = Objects.requireNonNull(fctx, "FilterContext cannot be null").getReferent();
        final GraphicsPipeline pipe = GraphicsPipeline.getPipeline();
        if (pipe == null || !(ref instanceof Screen)) {
            return null;
        }
        final InputStream shaderSource;
        if (pipe.supportsShader(GraphicsPipeline.ShaderType.HLSL, GraphicsPipeline.ShaderModel.SM3)) {
            shaderSource = shaderDeclaration.d3dSource();
        } else if (pipe.supportsShader(GraphicsPipeline.ShaderType.GLSL, GraphicsPipeline.ShaderModel.SM3)) {
            shaderSource = shaderDeclaration.es2Source();
        } else {
            throw new ShaderCreationException(String.format("Unsupported GraphicsPipeline (%s): Shaders are not supported", pipe.getClass().getSimpleName()));
        }
        final ShaderFactory factory = (ShaderFactory) GraphicsPipeline.getPipeline().getResourceFactory((Screen) ref);
        return factory.createShader(shaderSource, shaderDeclaration.samplers(), shaderDeclaration.params(),
                shaderDeclaration.samplers().keySet().size() - 1, true, false);
    }

    static IEffectRenderer getEffectRenderer(ShaderEffectBase effect) {
        if (ShaderController.EFFECT_RENDERER_MAP.containsKey(Objects.requireNonNull(effect, "Effect cannot be null").getClass())) {
            return ShaderController.EFFECT_RENDERER_MAP.get(effect.getClass());
        }
        final Class<? extends IEffectRenderer> rendererClass = effect.getClass().getAnnotation(EffectRenderer.class).value();
        try {
            final Constructor<? extends IEffectRenderer> constructor = rendererClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            final IEffectRenderer effectRenderer = constructor.newInstance();
            ShaderController.EFFECT_RENDERER_MAP.put(effect.getClass(), effectRenderer);
            return effectRenderer;
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                 InvocationTargetException e) {
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
            final BaseShaderContext.State state = ReflectionHelper.getFieldValue(BaseShaderContext.class, "state", shaderContext);
            final Texture[] lastTextures = ReflectionHelper.getFieldValue(BaseShaderContext.State.class, "lastTextures", state);
            if (lastTextures.length < MAX_BOUND_TEXTURES) {
                final Texture[] newTextures = new Texture[ShaderController.MAX_BOUND_TEXTURES];
                System.arraycopy(lastTextures, 0, newTextures, 0, lastTextures.length);
                ReflectionHelper.setFieldValue(BaseShaderContext.State.class, "lastTextures", state, newTextures);
            }

            final Object ref = Objects.requireNonNull(fctx, "FilterContext cannot be null").getReferent();
            if (!(ref instanceof Screen)) {
                throw new ShaderCreationException("Invalid FilterContext");
            }
            final BaseResourceFactory factory = (BaseResourceFactory) GraphicsPipeline.getPipeline().getResourceFactory((Screen) ref);
            final GraphicsPipeline pipe = GraphicsPipeline.getPipeline();
            if (pipe != null && pipe.supportsShader(GraphicsPipeline.ShaderType.GLSL, GraphicsPipeline.ShaderModel.SM3)) {
                final Object glContext = ES2RTTextureHelper.getGLContext(factory);
                if (ShaderController.boundTexturesField == null) {
                    ShaderController.boundTexturesField = ReflectionHelper.getField(Class.forName("com.sun.prism.es2.GLContext"), "boundTextures");
                }
                final int[] boundTextures = (int[]) ShaderController.boundTexturesField.get(glContext);
                if (boundTextures.length < MAX_BOUND_TEXTURES) {
                    final int[] newBoundTextures = new int[ShaderController.MAX_BOUND_TEXTURES];
                    System.arraycopy(boundTextures, 0, newBoundTextures, 0, boundTextures.length);
                    ShaderController.boundTexturesField.set(glContext, newBoundTextures);
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new ShaderCreationException("Could not ensure texture capacity", e);
        }
    }

    public static RTTexture createRTTexture(FilterContext fctx, PixelFormat formatHint, Texture.WrapMode wrapMode, int width, int height, boolean useMipmap) {
        final Object ref = Objects.requireNonNull(fctx, "FilterContext cannot be null").getReferent();
        final GraphicsPipeline pipe = GraphicsPipeline.getPipeline();
        if (pipe == null || !(ref instanceof Screen)) {
            throw new ShaderCreationException("Invalid GraphicsPipeline or FilterContext");
        }
        final BaseResourceFactory factory = (BaseResourceFactory) GraphicsPipeline.getPipeline().getResourceFactory((Screen) ref);
        if (pipe.supportsShader(GraphicsPipeline.ShaderType.GLSL, GraphicsPipeline.ShaderModel.SM3)) {
            return ES2RTTextureHelper.createES2RTTexture(factory, formatHint, wrapMode, width, height, useMipmap);
        } else if (pipe.supportsShader(GraphicsPipeline.ShaderType.HLSL, GraphicsPipeline.ShaderModel.SM3)) {
            return D3DRTTextureHelper.createD3DRTTexture(factory, formatHint, wrapMode, width, height, useMipmap);
        } else {
            throw new ShaderCreationException(String.format("Unsupported GraphicsPipeline (%s): Shaders are not supported", pipe.getClass().getSimpleName()));
        }
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

    /**
     * Replaces the internal {@link Shape3DHelper} accessors with custom accessors that support custom 3D shaders.
     * To allow for custom shaders on default {@link MeshView} instances, this injection must be performed before any {@link Shape3D} is rendered.
     * Otherwise, only {@link Shape3D} instances created after this injection will support custom shaders.
     */
    public static void ensure3DAccessorInjection() {
        NativeLibLoader.loadLibrary("jfxshader");
        ShaderController.injectShape3DAccessor(MeshViewHelper.class, "meshViewAccessor", InternalNGMeshView::new);
        ShaderController.injectShape3DAccessor(SphereHelper.class, "sphereAccessor", InternalNGSphere::new);
        ShaderController.injectShape3DAccessor(BoxHelper.class, "boxAccessor", InternalNGBox::new);
        ShaderController.injectShape3DAccessor(CylinderHelper.class, "cylinderAccessor", InternalNGCylinder::new);
    }

    private static void injectShape3DAccessor(Class<? extends Shape3DHelper> helper, String accessorField, Supplier<NGShape3D> shapeClassSupplier) {
        Utils.forceInit(helper);
        final Object accessor = ReflectionHelper.getFieldValue(helper, accessorField, null);
        if (Proxy.isProxyClass(accessor.getClass())) {
            return;
        }
        final Object proxy = Proxy.newProxyInstance(accessor.getClass().getClassLoader(), accessor.getClass().getInterfaces(), (p, method, args) -> {
            if ("doCreatePeer".equals(method.getName())) {
                return shapeClassSupplier.get();
            } else {
                return method.invoke(accessor, args);
            }
        });
        ReflectionHelper.setFieldValue(helper, accessorField, null, proxy);
    }

    public static IDirect3DDevice9 getD3DDevice(ResourceFactory resourceFactory) {
        return ShaderController.D3D_DEVICE_MAP.computeIfAbsent(D3DRTTextureHelper.getDevice(resourceFactory), IDirect3DDevice9::new);
    }

}
