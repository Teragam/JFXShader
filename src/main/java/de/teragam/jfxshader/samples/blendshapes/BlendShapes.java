package de.teragam.jfxshader.samples.blendshapes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.beans.property.ObjectProperty;

import de.teragam.jfxshader.EffectDependencies;
import de.teragam.jfxshader.TwoSamplerEffect;

@EffectDependencies(BlendShapesEffectPeer.class)
public class BlendShapes extends TwoSamplerEffect {

    private final List<ObjectProperty<BlendShape>> shapes;

    public BlendShapes() {
        this.shapes = new ArrayList<>();
    }

    public ObjectProperty<BlendShape> createShapeProperty() {
        final ObjectProperty<BlendShape> property = super.createEffectObjectProperty(null, "shape");
        this.shapes.add(property);
        return property;
    }

    public List<ObjectProperty<BlendShape>> getBlendShapes() {
        return Collections.unmodifiableList(this.shapes);
    }

}
