package javafx.scene.effect;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.IntegerPropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.scene.Node;

import com.sun.javafx.effect.EffectDirtyBits;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.BoundsAccessor;
import com.sun.scenario.effect.InternalCoreEffectBase;
import com.sun.scenario.effect.impl.state.RenderState;

import de.teragam.jfxshader.DefaultEffectRenderer;
import de.teragam.jfxshader.EffectRenderer;

@EffectRenderer(DefaultEffectRenderer.class)
public class ShaderEffectBase extends Effect {

    public static final int MAX_INPUTS = 2;

    private final List<ObjectProperty<Effect>> inputProperties;

    @Override
    protected InternalCoreEffectBase createPeer() {
        return new InternalCoreEffectBase(this, this.inputProperties.size());
    }

    protected ShaderEffectBase(int inputs) {
        if (inputs < 1 || inputs > ShaderEffectBase.MAX_INPUTS) {
            throw new IllegalArgumentException(String.format("Only 1 to %d inputs are supported. Requested were %d.", ShaderEffectBase.MAX_INPUTS, inputs));
        }
        this.inputProperties = new ArrayList<>(inputs);
        for (int i = 0; i < inputs; i++) {
            this.inputProperties.add(null);
        }
    }

    protected ObjectProperty<Effect> getInputProperty(int index) {
        if (this.inputProperties.get(index) == null) {
            this.inputProperties.set(index, new EffectInputProperty(String.format("input-%d", index)));
        }
        return this.inputProperties.get(index);
    }

    public RenderState getRenderState() {
        return RenderState.RenderSpaceRenderState;
    }

    @Override
    protected boolean checkChainContains(Effect e) {
        for (final ObjectProperty<Effect> property : this.inputProperties) {
            if (property == null) {
                continue;
            }
            final Effect effect = property.get();
            if (effect == e || (effect != null && effect.checkChainContains(e))) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void update() {
        final InternalCoreEffectBase peer = ((InternalCoreEffectBase) this.getPeer());
        final List<ObjectProperty<Effect>> properties = this.inputProperties;
        for (int i = 0; i < properties.size(); i++) {
            final ObjectProperty<Effect> property = properties.get(i);
            if (property != null) {
                final Effect localInput = property.get();
                if (localInput != null) {
                    localInput.sync();
                }
                peer.setInput(i, localInput == null ? null : localInput.getPeer());
            }
        }
    }

    public int getInputs() {
        return this.inputProperties.size();
    }

    @Override
    protected BaseBounds getBounds(BaseBounds bounds, BaseTransform tx, Node node, BoundsAccessor boundsAccessor) { //TODO verify bounds calculation
        BaseBounds baseBounds = Effect.getInputBounds(bounds, tx, node, boundsAccessor,
                this.inputProperties.get(0) == null ? null : this.inputProperties.get(0).get());
        for (int i = 1; i < this.inputProperties.size(); i++) {
            baseBounds = baseBounds.deriveWithUnion(
                    Effect.getInputBounds(bounds, tx, node, boundsAccessor, this.inputProperties.get(i) == null ? null : this.inputProperties.get(i).get()));
        }
        return baseBounds;
    }

    protected static BaseBounds getInputBounds(BaseBounds bounds, BaseTransform tx, Node node, BoundsAccessor boundsAccessor, Effect input) {
        return Effect.getInputBounds(bounds, tx, node, boundsAccessor, input);
    }

    @Override
    protected Effect copy() {
        throw new UnsupportedOperationException("No copy functionality specified");
    }

    protected void markDirty() {
        super.markDirty(EffectDirtyBits.EFFECT_DIRTY);
    }

    protected DoubleProperty createEffectDoubleProperty(double value, String name) {
        return new DoublePropertyBase(value) {

            @Override
            public void invalidated() {
                ShaderEffectBase.this.markDirty();
            }

            @Override
            public Object getBean() {
                return ShaderEffectBase.this;
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }

    protected IntegerProperty createEffectIntegerProperty(int value, String name) {
        return new IntegerPropertyBase(value) {

            @Override
            public void invalidated() {
                ShaderEffectBase.this.markDirty();
            }

            @Override
            public Object getBean() {
                return ShaderEffectBase.this;
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }

    protected BooleanProperty createEffectBooleanProperty(boolean value, String name) {
        return new BooleanPropertyBase(value) {

            @Override
            public void invalidated() {
                ShaderEffectBase.this.markDirty();
            }

            @Override
            public Object getBean() {
                return ShaderEffectBase.this;
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }

    protected <T> ObjectProperty<T> createEffectObjectProperty(T value, String name) {
        return new ObjectPropertyBase<>(value) {

            @Override
            public void invalidated() {
                ShaderEffectBase.this.markDirty();
            }

            @Override
            public Object getBean() {
                return ShaderEffectBase.this;
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }

}
