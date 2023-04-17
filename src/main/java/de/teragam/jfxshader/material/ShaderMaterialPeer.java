package de.teragam.jfxshader.material;

import java.io.InputStream;

import com.sun.prism.GraphicsPipeline;
import com.sun.prism.ps.Shader;

import de.teragam.jfxshader.ShaderDeclaration;
import de.teragam.jfxshader.internal.Reflect;

public abstract class ShaderMaterialPeer<T extends ShaderMaterial> {

    public ShaderDeclaration createVertexShaderDeclaration() {
        InputStream es2Source = null;
        if (GraphicsPipeline.getPipeline().supportsShader(GraphicsPipeline.ShaderType.GLSL, GraphicsPipeline.ShaderModel.SM3)) {
            // TODO: Check whether a different class can be used here
            es2Source = Reflect.resolveClass("com.sun.prism.es2.ES2ResourceFactory").getResourceAsStream("glsl/main.vert");
        }
        return new ShaderDeclaration(null, null, es2Source, ShaderMaterialPeer.class.getResourceAsStream("/hlsl/Mtl1VS.obj"));
    }

    public abstract ShaderDeclaration createPixelShaderDeclaration();

    public abstract void updateShader(Shader vertexShader, Shader pixelShader, T material);

}
