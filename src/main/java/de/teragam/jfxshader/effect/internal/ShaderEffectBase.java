package de.teragam.jfxshader.effect.internal;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.scene.effect.Blend;
import javafx.scene.effect.Effect;

import com.sun.javafx.effect.EffectDirtyBits;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.BoundsAccessor;
import com.sun.scenario.effect.impl.EffectPeer;
import com.sun.scenario.effect.impl.state.RenderState;

import de.teragam.jfxshader.effect.InternalEffect;
import de.teragam.jfxshader.effect.ShaderEffect;
import de.teragam.jfxshader.util.Reflect;

public class ShaderEffectBase extends Blend {

    public static final int MAX_INPUTS = 2;
    private static final Reflect<Effect> EFFECT_REFLECT = Reflect.on(Effect.class);

    private final ShaderEffect effect;
    private final InternalEffect peer;
    private final UUID effectID;

    private boolean continuousRendering;

    public ShaderEffectBase(ShaderEffect effect, int inputs) {
        if (inputs < 1 || inputs > MAX_INPUTS) {
            throw new IllegalArgumentException(String.format("Only 1 to %d inputs are supported. Requested were %d.", MAX_INPUTS, inputs));
        }
        this.effectID = UUID.randomUUID();
        this.effect = effect;
        this.peer = new InternalEffect(effect, inputs);
        EFFECT_REFLECT.setFieldValue("peer", this, this.peer);
    }

    public void setContinuousRendering(boolean continuousRendering) {
        if (continuousRendering && !this.continuousRendering) {
            EffectRenderTimer.getInstance().register(this);
        }
        this.continuousRendering = continuousRendering;
    }

    public boolean isContinuousRendering() {
        return this.continuousRendering;
    }

    public ShaderEffect getJFXShaderEffect() {
        return this.effect;
    }

    public void markDirty() {
        EFFECT_REFLECT.method("markDirty", EffectDirtyBits.class).invoke(this, EffectDirtyBits.EFFECT_DIRTY);
    }

    public void updateInputs() {
        final List<ObjectProperty<Effect>> properties = List.of(this.topInputProperty(), this.bottomInputProperty());
        for (int i = 0; i < properties.size(); i++) {
            final ObjectProperty<Effect> property = properties.get(i);
            if (property != null) {
                final Effect localInput = property.get();
                if (localInput != null) {
                    EFFECT_REFLECT.method("sync").invoke(localInput);
                }
                this.peer.setInput(i, localInput == null ? null : (com.sun.scenario.effect.Effect) EFFECT_REFLECT.method("getPeer").invoke(localInput));
            }
        }
    }

    public Map<String, EffectPeer<? super RenderState>> getPeerMap() {
        return this.peer.getPeerMap();
    }

    public UUID getEffectID() {
        return this.effectID;
    }

    public static BaseBounds getInputBounds(BaseBounds bounds, BaseTransform tx, Node node, BoundsAccessor boundsAccessor, Effect input) {
        return EFFECT_REFLECT.<BaseBounds>method("getInputBounds").invoke(null, bounds, tx, node, boundsAccessor, input);
    }

}
