package de.teragam.jfxshader;

import com.sun.prism.ps.Shader;

public interface JFXShader extends Shader {

    void setMatrix(String name, float[] buf, int vector4fCount);

}
