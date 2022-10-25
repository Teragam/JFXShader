package com.sun.scenario.effect.impl.prism.ps;

import java.lang.reflect.Field;

import com.sun.prism.ps.Shader;
import com.sun.scenario.effect.Effect;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.ImageData;
import com.sun.scenario.effect.impl.EffectPeer;
import com.sun.scenario.effect.impl.Renderer;
import com.sun.scenario.effect.impl.state.RenderState;

import de.teragam.jfxshader.ShaderCreationException;

public abstract class PPSMultiSamplerPeer<T extends RenderState> extends PPSEffectPeer<T> {

    private final PPSOneSamplerPeer<T> oneSamplerPeer;
    private final PPSTwoSamplerPeer twoSamplerPeer;
    private final Field effectPeerDestBoundsField;

    protected PPSMultiSamplerPeer(FilterContext fctx, Renderer r, String shaderName) {
        super(fctx, r, shaderName);
        this.oneSamplerPeer = new PPSOneSamplerPeer<T>(fctx, r, shaderName) {
            @Override
            protected boolean isSamplerLinear(int i) {
                return PPSMultiSamplerPeer.this.isSamplerLinear(i);
            }

            @Override
            protected Shader createShader() {
                return PPSMultiSamplerPeer.this.createShader();
            }

            @Override
            protected void updateShader(Shader shader) {
                PPSMultiSamplerPeer.this.updateShader(shader);
            }
        };
        this.twoSamplerPeer = new PPSTwoSamplerPeer(fctx, r, shaderName) {
            @Override
            protected boolean isSamplerLinear(int i) {
                return PPSMultiSamplerPeer.this.isSamplerLinear(i);
            }

            @Override
            protected Shader createShader() {
                return PPSMultiSamplerPeer.this.createShader();
            }

            @Override
            protected void updateShader(Shader shader) {
                PPSMultiSamplerPeer.this.updateShader(shader);
            }
        };
        try {
            this.effectPeerDestBoundsField = EffectPeer.class.getDeclaredField("destBounds");
            this.effectPeerDestBoundsField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new ShaderCreationException("Could not get bounds field", e);
        }
    }

    @Override
    protected void setEffect(Effect effect) {
        super.setEffect(effect);
    }

    @Override
    public void dispose() {
        this.oneSamplerPeer.dispose();
        this.twoSamplerPeer.dispose();
    }

    @Override
    ImageData filterImpl(ImageData... inputs) {
        if (inputs.length == 1) {
            try {
                this.effectPeerDestBoundsField.set(this.oneSamplerPeer, this.getDestBounds());
            } catch (IllegalAccessException e) {
                throw new ShaderCreationException("Could not set bounds", e);
            }
            return this.oneSamplerPeer.filterImpl(inputs);
        }
        if (inputs.length == 2) {
            try {
                this.effectPeerDestBoundsField.set(this.twoSamplerPeer, this.getDestBounds());
            } catch (IllegalAccessException e) {
                throw new ShaderCreationException("Could not set bounds", e);
            }
            return this.twoSamplerPeer.filterImpl(inputs);
        }
        throw new IllegalArgumentException("Invalid number of inputs for effect: " + this.getEffect());
    }

}
