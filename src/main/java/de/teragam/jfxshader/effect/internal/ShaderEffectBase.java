package de.teragam.jfxshader.effect.internal;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.effect.Blend;
import javafx.scene.effect.ColorInput;
import javafx.scene.effect.Effect;
import javafx.scene.effect.Lighting;

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

    private final ObjectProperty<BoundsEffectProxy> boundsEffectProxyTop;
    private final ObjectProperty<BoundsEffectProxy> boundsEffectProxyBottom;
    private final ObjectProperty<Effect> inputMirrorTop;
    private final ObjectProperty<Effect> inputMirrorBottom;

    private boolean continuousRendering;

    public ShaderEffectBase(ShaderEffect effect, int inputs) {
        if (inputs < 1 || inputs > MAX_INPUTS) {
            throw new IllegalArgumentException(String.format("Only 1 to %d inputs are supported. Requested were %d.", MAX_INPUTS, inputs));
        }
        this.effectID = UUID.randomUUID();
        this.effect = effect;
        this.peer = new InternalEffect(effect, inputs);
        EFFECT_REFLECT.setFieldValue("peer", this, this.peer);
        this.boundsEffectProxyTop = new SimpleObjectProperty<>(new BoundsEffectProxy());
        this.boundsEffectProxyBottom = new SimpleObjectProperty<>(new BoundsEffectProxy());
        this.inputMirrorTop = super.topInputProperty();
        this.inputMirrorBottom = super.bottomInputProperty();
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
        EFFECT_REFLECT.method("effectBoundsChanged").invoke(this);
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

    public InternalEffect getInternalPeer() {
        return this.peer;
    }

    public UUID getEffectID() {
        return this.effectID;
    }

    public static BaseBounds getInputBounds(BaseBounds bounds, BaseTransform tx, Node node, BoundsAccessor boundsAccessor, Effect input) {
        return EFFECT_REFLECT.<BaseBounds>method("getInputBounds").invoke(null, bounds, tx, node, boundsAccessor, input);
    }

    private void prepareBoundsCalculation(BaseBounds bounds, Node node, BoundsAccessor boundsAccessor) {
        final BaseBounds inputBounds = getInputBounds(bounds, BaseTransform.IDENTITY_TRANSFORM, node, boundsAccessor, this);
        final BaseBounds effectBounds = this.effect.getBounds(inputBounds); // Must be untransformed bounds
        this.boundsEffectProxyTop.get().setBounds(effectBounds);
        this.boundsEffectProxyBottom.get().setBounds(effectBounds);

        final Reflect<Blend> reflect = Reflect.on(Blend.class);
        reflect.setFieldValue("topInput", this, this.boundsEffectProxyTop);
        reflect.setFieldValue("bottomInput", this, this.boundsEffectProxyBottom);
    }

    private void restoreBoundsCalculation() {
        final Reflect<Blend> reflect = Reflect.on(Blend.class);
        reflect.setFieldValue("topInput", this, this.inputMirrorTop);
        reflect.setFieldValue("bottomInput", this, this.inputMirrorBottom);
    }

    public static BaseBounds getBounds(Effect effect, BaseBounds bounds, BaseTransform tx, Node node, BoundsAccessor boundsAccessor) {
        ShaderEffectBase.replaceRecursive(effect, bounds, node, boundsAccessor, false);
        final BaseBounds result = EFFECT_REFLECT.<BaseBounds>method("getBounds").invoke(effect, bounds, tx, node, boundsAccessor);
        ShaderEffectBase.replaceRecursive(effect, bounds, node, boundsAccessor, true);
        return result;
    }

    // This method iterates over the effect tree and precalculates the bounds for every ShaderEffectBase.
    // It is not possible to override the original package-private getBounds method to provide a custom bounds calculation (without bytecode manipulation).
    // To circumvent this, we can replace the parent inputs of the ShaderEffectBase with a BoundsEffectProxy that can provide precalculated bounds.
    // The bounds are then calculated by the original getBounds method and the original inputs are restored.
    private static void replaceRecursive(Effect effect, BaseBounds bounds, Node node, BoundsAccessor boundsAccessor, boolean restore) {
        if (effect == null) {
            return;
        }
        Effect topInput = null;
        Effect bottomInput = null;
        if (effect instanceof Blend) {
            final Blend blend = (Blend) effect;
            topInput = blend.getTopInput();
            bottomInput = blend.getBottomInput();
        } else if (effect instanceof Lighting) {
            final Lighting lighting = (Lighting) effect;
            topInput = lighting.getContentInput();
            bottomInput = lighting.getBumpInput();
        } else {
            final Reflect<? extends Effect> reflect = Reflect.on(effect.getClass());
            if (reflect.hasField("input")) {
                final ObjectProperty<Effect> effectProperty = reflect.getFieldValue("input", effect);
                if (effectProperty != null) {
                    topInput = effectProperty.get();
                }
            }
        }
        replaceRecursive(topInput, bounds, node, boundsAccessor, restore);
        replaceRecursive(bottomInput, bounds, node, boundsAccessor, restore);
        if (effect instanceof ShaderEffectBase) {
            final ShaderEffectBase effectBase = (ShaderEffectBase) effect;
            if (restore) {
                effectBase.restoreBoundsCalculation();
            } else {
                effectBase.prepareBoundsCalculation(bounds, node, boundsAccessor);
            }
        }
    }

    private static final class BoundsEffectProxy extends ColorInput {

        public void setBounds(BaseBounds bounds) {
            this.setX(bounds.getMinX());
            this.setY(bounds.getMinY());
            this.setWidth(bounds.getWidth());
            this.setHeight(bounds.getHeight());
        }

    }

}
