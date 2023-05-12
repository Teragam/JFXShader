package de.teragam.jfxshader.effect.internal;

import java.util.List;

import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.scene.effect.Blend;
import javafx.scene.effect.Effect;

import com.sun.javafx.effect.EffectDirtyBits;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.BoundsAccessor;

import de.teragam.jfxshader.effect.ShaderEffect;
import de.teragam.jfxshader.util.Reflect;

public class ShaderEffectBase extends Blend {

    public static final int MAX_INPUTS = 2;
    private static final Reflect<Effect> EFFECT_REFLECT = Reflect.on(Effect.class);

    private final ShaderEffect effect;
    private final InternalEffect peer;

    public ShaderEffectBase(ShaderEffect effect, int inputs) {
        if (inputs < 1 || inputs > MAX_INPUTS) {
            throw new IllegalArgumentException(String.format("Only 1 to %d inputs are supported. Requested were %d.", MAX_INPUTS, inputs));
        }

        this.effect = effect;
        this.peer = new InternalEffect(effect, inputs);
        EFFECT_REFLECT.setFieldValue("peer", this, this.peer);
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

    public static BaseBounds getInputBounds(BaseBounds bounds, BaseTransform tx, Node node, BoundsAccessor boundsAccessor, Effect input) {
        return EFFECT_REFLECT.<BaseBounds>method("getInputBounds").invoke(null, bounds, tx, node, boundsAccessor, input);
    }

}
