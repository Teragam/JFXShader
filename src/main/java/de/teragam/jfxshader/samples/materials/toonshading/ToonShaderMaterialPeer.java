package de.teragam.jfxshader.samples.materials.toonshading;

import com.sun.prism.ps.Shader;

import de.teragam.jfxshader.ShaderDeclaration;
import de.teragam.jfxshader.material.ShaderMaterialPeer;

public class ToonShaderMaterialPeer extends ShaderMaterialPeer<ToonShader> {
    @Override
    public ShaderDeclaration createPixelShaderDeclaration() {
        return null;
    }

    @Override
    public void updateShader(Shader vertexShader, Shader pixelShader, ToonShader material) {

    }
}
