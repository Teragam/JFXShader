package de.teragam.jfxshader;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javafx.scene.effect.ShaderEffectBase;

import com.sun.glass.ui.Screen;
import com.sun.prism.GraphicsPipeline;
import com.sun.prism.ps.Shader;
import com.sun.prism.ps.ShaderFactory;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.impl.Renderer;

public final class ShaderController {

    private static final Map<Class<? extends ShaderEffectBase>, IEffectRenderer> EFFECT_RENDERER_MAP = Collections.synchronizedMap(new HashMap<>());
    private static Field peerCacheField;

    private ShaderController() {
    }

    @SuppressWarnings("unchecked")
    public static void register(FilterContext fctx, ShaderEffectBase effect) {
        final Renderer renderer = Renderer.getRenderer(Objects.requireNonNull(fctx, "FilterContext cannot be null"));
        try {
            if (ShaderController.peerCacheField == null) {
                ShaderController.peerCacheField = Renderer.class.getDeclaredField("peerCache");
                ShaderController.peerCacheField.setAccessible(true);
            }
            final Map<String, com.sun.scenario.effect.impl.EffectPeer<?>> peerCache =
                    (Map<String, com.sun.scenario.effect.impl.EffectPeer<?>>) ShaderController.peerCacheField.get(
                            renderer);
            final List<Class<? extends ShaderEffectPeer<?>>> dependencies = ShaderController.getPeerDependencies(effect.getClass());
            for (final Class<? extends ShaderEffectPeer<?>> peer : dependencies) {
                final String peerName = ShaderController.getPeerName(peer);
                if (!peerCache.containsKey(peerName)) {
                    final Constructor<? extends ShaderEffectPeer<?>> constructor = peer.getDeclaredConstructor(FilterContext.class);
                    constructor.setAccessible(true);
                    peerCache.put(peerName, constructor.newInstance(fctx));
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException | InvocationTargetException | NoSuchMethodException |
                 InstantiationException e) {
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
            throw new ShaderCreationException("Unsupported GraphicsPipeline");
        }
        final ShaderFactory factory = (ShaderFactory) GraphicsPipeline.getPipeline().getResourceFactory((Screen) ref);
        return factory.createShader(shaderSource, shaderDeclaration.samplers(), shaderDeclaration.params(),
                shaderDeclaration.samplers().keySet().size() - 1, true, false);
    }

    public static IEffectRenderer getEffectRenderer(ShaderEffectBase effect) {
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

    private static String getPeerName(Class<? extends ShaderEffectPeer<?>> peer) {
        if (Objects.requireNonNull(peer, "Peer cannot be null").isAnnotationPresent(EffectPeer.class)) {
            return peer.getAnnotation(EffectPeer.class).value();
        } else {
            throw new IllegalArgumentException(String.format("%s is not annotated with %s", peer, EffectPeer.class));
        }
    }

}
