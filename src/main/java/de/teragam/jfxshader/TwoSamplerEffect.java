package de.teragam.jfxshader;

import javafx.beans.property.ObjectProperty;
import javafx.scene.effect.Effect;

public abstract class TwoSamplerEffect extends ShaderEffect {

    protected TwoSamplerEffect() {
        super(2);
    }

    public void setPrimaryInput(Effect value) {
        this.primaryInputProperty().set(value);
    }

    public Effect getPrimaryInput() {
        return this.primaryInputProperty().get();
    }

    public ObjectProperty<Effect> primaryInputProperty() {
        return super.getInputProperty(0);
    }

    public void setSecondaryInput(Effect value) {
        this.secondaryInputProperty().set(value);
    }

    public Effect getSecondaryInput() {
        return this.secondaryInputProperty().get();
    }

    public ObjectProperty<Effect> secondaryInputProperty() {
        return super.getInputProperty(1);
    }

}
