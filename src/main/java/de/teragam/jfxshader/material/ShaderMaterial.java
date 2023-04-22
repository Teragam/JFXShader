package de.teragam.jfxshader.material;

import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.image.Image;
import javafx.scene.paint.Material;

import com.sun.javafx.beans.event.AbstractNotifyListener;
import com.sun.javafx.tk.Toolkit;

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
        return new SimpleDoubleProperty(ShaderMaterial.this, name, value) {
            @Override
            public void invalidated() {
                ShaderMaterial.this.markDirty();
            }
        };
    }

    protected IntegerProperty createMaterialIntegerProperty(int value, String name) {
        return new SimpleIntegerProperty(ShaderMaterial.this, name, value) {
            @Override
            public void invalidated() {
                ShaderMaterial.this.markDirty();
            }
        };
    }

    protected BooleanProperty createMaterialBooleanProperty(boolean value, String name) {
        return new SimpleBooleanProperty(ShaderMaterial.this, name, value) {
            @Override
            public void invalidated() {
                ShaderMaterial.this.markDirty();
            }
        };
    }

    protected <T> ObjectProperty<T> createMaterialObjectProperty(T value, String name) {
        return new SimpleObjectProperty<>(ShaderMaterial.this, name, value) {
            @Override
            public void invalidated() {
                ShaderMaterial.this.markDirty();
            }
        };
    }

    protected ObjectProperty<Image> createMaterialImageProperty(Image value, String name) {
        return new SimpleObjectProperty<>(ShaderMaterial.this, name, value) {

            private boolean needsListeners;
            private Image lastImage;

            @Override
            public void invalidated() {
                final Image image = this.get();

                if (this.needsListeners) {
                    Toolkit.getImageAccessor().getImageProperty(this.lastImage)
                            .removeListener(ShaderMaterial.this.platformImageChangeListener.getWeakListener());
                }

                this.needsListeners = image != null && (Toolkit.getImageAccessor().isAnimation(image) || image.getProgress() < 1);
                if (this.needsListeners) {
                    Toolkit.getImageAccessor().getImageProperty(image).
                            addListener(ShaderMaterial.this.platformImageChangeListener.getWeakListener());
                }

                this.lastImage = image;

                ShaderMaterial.this.markDirty();
            }
        };
    }

    private final AbstractNotifyListener platformImageChangeListener = new AbstractNotifyListener() {
        @Override
        public void invalidated(Observable valueModel) {
            ShaderMaterial.this.markDirty();
        }
    };

}
