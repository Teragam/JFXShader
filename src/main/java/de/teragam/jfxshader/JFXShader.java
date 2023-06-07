package de.teragam.jfxshader;

import com.sun.prism.ps.Shader;

import de.teragam.jfxshader.util.ReflectProxy;

public interface JFXShader extends Shader, ReflectProxy {

    void setMatrix(String name, float[] buf, int vector4fCount);

}
