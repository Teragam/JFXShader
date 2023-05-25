package de.teragam.jfxshader.samples.effects.blendshapes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;

import de.teragam.jfxshader.effect.EffectDependencies;
import de.teragam.jfxshader.effect.TwoSamplerEffect;

@EffectDependencies(BlendShapesEffectPeer.class)
public class BlendShapes extends TwoSamplerEffect {

    private final List<ObjectProperty<BlendShape>> shapes;
    private final BooleanProperty invertMask;

    public BlendShapes() {
        this.shapes = new ArrayList<>();
        this.invertMask = super.createEffectBooleanProperty(false, "invertMask");
    }

    public ObjectProperty<BlendShape> createShapeProperty() {
        final ObjectProperty<BlendShape> property = super.createEffectObjectProperty(null, "shape");
        this.shapes.add(property);
        return property;
    }

    public List<ObjectProperty<BlendShape>> getBlendShapes() {
        return Collections.unmodifiableList(this.shapes);
    }

    public boolean isInvertMask() {
        return this.invertMaskProperty().get();
    }

    public BooleanProperty invertMaskProperty() {
        return this.invertMask;
    }

    public void setInvertMask(boolean invertMask) {
        this.invertMaskProperty().set(invertMask);
    }
}
