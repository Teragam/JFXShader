package de.teragam.jfxshader;

import com.sun.prism.ps.Shader;

public interface ES2Shader extends Shader {

    void setMatrix(String name, float[] buf);

}
