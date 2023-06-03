package de.teragam.jfxshader.samples.effects.proxyshadereffect;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javafx.application.Platform;

import com.sun.prism.ps.Shader;

import de.teragam.jfxshader.ShaderDeclaration;
import de.teragam.jfxshader.effect.EffectDependencies;
import de.teragam.jfxshader.effect.EffectPeer;
import de.teragam.jfxshader.effect.ShaderEffectPeer;
import de.teragam.jfxshader.effect.ShaderEffectPeerConfig;
import de.teragam.jfxshader.effect.TwoSamplerEffect;

@EffectDependencies(ProxyShaderEffect.ProxyShaderEffectPeer.class)
public class ProxyShaderEffect extends TwoSamplerEffect {

    private final AtomicReference<Supplier<ShaderDeclaration>> declarationSupplier;
    private final AtomicBoolean invalidateDeclaration;
    private final AtomicReference<BiConsumer<Shader, ShaderEffectPeer<ProxyShaderEffect>>> shaderConsumer;

    public ProxyShaderEffect() {
        this(null);
    }

    public ProxyShaderEffect(Supplier<ShaderDeclaration> declarationSupplier) {
        this.declarationSupplier = new AtomicReference<>(declarationSupplier);
        this.invalidateDeclaration = new AtomicBoolean();
        this.shaderConsumer = new AtomicReference<>();
    }

    public void setShaderDeclarationSupplier(Supplier<ShaderDeclaration> declarationSupplier) {
        this.declarationSupplier.set(declarationSupplier);
    }

    public void invalidateShaderDeclaration() {
        this.invalidateDeclaration.set(true);
        Platform.runLater(this::markDirty);
    }

    public void setShaderConsumer(BiConsumer<Shader, ShaderEffectPeer<ProxyShaderEffect>> shaderConsumer) {
        this.shaderConsumer.set(shaderConsumer);
    }

    @Override
    public void setContinuousRendering(boolean continuousRendering) {
        super.setContinuousRendering(continuousRendering);
    }

    @EffectPeer(value = "ProxyShaderEffect", singleton = false)
    protected class ProxyShaderEffectPeer extends ShaderEffectPeer<ProxyShaderEffect> {

        protected ProxyShaderEffectPeer(ShaderEffectPeerConfig config) {
            super(config);
        }

        @Override
        protected ShaderDeclaration createShaderDeclaration() {
            return Objects.requireNonNull(ProxyShaderEffect.this.declarationSupplier.get(), "The ShaderDeclaration supplier cannot be null").get();
        }

        @Override
        protected void updateShader(Shader shader, ProxyShaderEffect effect) {
            if (ProxyShaderEffect.this.invalidateDeclaration.getAndSet(false)) {
                this.invalidateShader();
            }
            final BiConsumer<Shader, ShaderEffectPeer<ProxyShaderEffect>> consumer = ProxyShaderEffect.this.shaderConsumer.get();
            if (consumer != null) {
                consumer.accept(shader, this);
            }
        }
    }

}
