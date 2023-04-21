package de.teragam.jfxshader.material;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.IntegerPropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.scene.paint.Material;

import de.teragam.jfxshader.MaterialController;
import de.teragam.jfxshader.material.internal.ShaderMaterialBase;

public abstract class ShaderMaterial {

    private final ShaderMaterialBase materialBase;

    protected ShaderMaterial() {
        MaterialController.ensure3DAccessorInjection();
        this.materialBase = new ShaderMaterialBase(this);
    }

    public Material getFXMaterial() {
        return this.materialBase;
    }

    private void markDirty() {
        this.materialBase.setDirty(true);
    }

    protected DoubleProperty createMaterialDoubleProperty(double value, String name) {
        return new DoublePropertyBase(value) {

            @Override
            public void invalidated() {
                ShaderMaterial.this.markDirty();
            }

            @Override
            public Object getBean() {
                return ShaderMaterial.this;
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }

    protected IntegerProperty createMaterialIntegerProperty(int value, String name) {
        return new IntegerPropertyBase(value) {

            @Override
            public void invalidated() {
                ShaderMaterial.this.markDirty();
            }

            @Override
            public Object getBean() {
                return ShaderMaterial.this;
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }

    protected BooleanProperty createMaterialBooleanProperty(boolean value, String name) {
        return new BooleanPropertyBase(value) {

            @Override
            public void invalidated() {
                ShaderMaterial.this.markDirty();
            }

            @Override
            public Object getBean() {
                return ShaderMaterial.this;
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }

    protected <T> ObjectProperty<T> createMaterialObjectProperty(T value, String name) {
        return new ObjectPropertyBase<>(value) {

            @Override
            public void invalidated() {
                ShaderMaterial.this.markDirty();
            }

            @Override
            public Object getBean() {
                return ShaderMaterial.this;
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }

}
