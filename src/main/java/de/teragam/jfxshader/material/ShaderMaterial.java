package de.teragam.jfxshader.material;

import javafx.scene.paint.ShaderMaterialBase;

import de.teragam.jfxshader.internal.ShaderController;

public class ShaderMaterial extends ShaderMaterialBase {

    public ShaderMaterial() {
        ShaderController.ensure3DAccessorInjection();
    }
}
