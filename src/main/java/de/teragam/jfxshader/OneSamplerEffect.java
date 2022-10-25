package de.teragam.jfxshader;

import javafx.beans.property.ObjectProperty;
import javafx.scene.effect.Effect;

public abstract class OneSamplerEffect extends ShaderEffect {
    protected OneSamplerEffect() {
        super(1);
    }

    public void setInput(Effect value) {
        this.inputProperty().set(value);
    }

    public Effect getInput() {
        return this.inputProperty().get();
    }

    public ObjectProperty<Effect> inputProperty() {
        return super.getInputProperty(0);
    }
}
