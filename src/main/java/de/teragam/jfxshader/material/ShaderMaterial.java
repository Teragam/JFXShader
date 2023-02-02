package de.teragam.jfxshader.material;

import javafx.scene.paint.PhongMaterial;

import de.teragam.jfxshader.internal.ShaderController;

public class ShaderMaterial extends PhongMaterial {

    public ShaderMaterial() {
        ShaderController.ensure3DAccessorInjection();
    }
}
