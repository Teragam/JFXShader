package de.teragam.jfxshader;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import de.teragam.jfxshader.util.ReflectProxy;

/**
 * The main interface for interacting with shaders.
 * <p>
 * Implementations wrap backend-specific shaders (for example Direct3D or OpenGL) and expose a
 * shared API for resource lifecycle and shader constant updates.
 * This class originates from {@link com.sun.prism.ps.Shader} but was separately defined to improve the
 * usability with the Java module system by avoiding exposing internal APIs to users of this library.
 */
public interface JFXShader extends ReflectProxy {

    /**
     * Releases all native and backend-specific resources associated with this shader.
     */
    void dispose();

    /**
     * Activates this shader for subsequent rendering operations.
     */
    void enable();

    /**
     * Deactivates this shader so subsequent rendering no longer uses it.
     */
    void disable();

    /**
     * Checks whether this shader is still usable.
     *
     * @return {@code true} if this shader is valid and can be used, otherwise {@code false}
     */
    boolean isValid();

    /**
     * Sets a scalar integer constant.
     *
     * @param name shader constant name
     * @param i0   first component
     */
    void setConstant(String name, int i0);

    /**
     * Sets a 2-component integer constant.
     *
     * @param name shader constant name
     * @param i0   first component
     * @param i1   second component
     */
    void setConstant(String name, int i0, int i1);

    /**
     * Sets a 3-component integer constant.
     *
     * @param name shader constant name
     * @param i0   first component
     * @param i1   second component
     * @param i2   third component
     */
    void setConstant(String name, int i0, int i1, int i2);

    /**
     * Sets a 4-component integer constant.
     *
     * @param name shader constant name
     * @param i0   first component
     * @param i1   second component
     * @param i2   third component
     * @param i3   fourth component
     */
    void setConstant(String name, int i0, int i1, int i2, int i3);

    /**
     * Sets one or more integer constant vectors from a buffer.
     *
     * @param name  shader constant name
     * @param buf   source buffer
     * @param off   start offset in vector elements
     * @param count number of vector elements to upload
     */
    void setConstants(String name, IntBuffer buf, int off, int count);

    /**
     * Sets a scalar float constant.
     *
     * @param name shader constant name
     * @param f0   first component
     */
    void setConstant(String name, float f0);

    /**
     * Sets a 2-component float constant.
     *
     * @param name shader constant name
     * @param f0   first component
     * @param f1   second component
     */
    void setConstant(String name, float f0, float f1);

    /**
     * Sets a 3-component float constant.
     *
     * @param name shader constant name
     * @param f0   first component
     * @param f1   second component
     * @param f2   third component
     */
    void setConstant(String name, float f0, float f1, float f2);

    /**
     * Sets a 4-component float constant.
     *
     * @param name shader constant name
     * @param f0   first component
     * @param f1   second component
     * @param f2   third component
     * @param f3   fourth component
     */
    void setConstant(String name, float f0, float f1, float f2, float f3);

    /**
     * Sets one or more float constant vectors from a buffer.
     *
     * @param name  shader constant name
     * @param buf   source buffer
     * @param off   start offset in vector elements
     * @param count number of vector elements to upload
     */
    void setConstants(String name, FloatBuffer buf, int off, int count);

    /**
     * Sets a matrix constant.
     *
     * @param name          shader constant name
     * @param buf           matrix data buffer
     * @param vector4fCount number of {@code vec4}-sized elements represented by {@code buf}
     */
    void setMatrix(String name, float[] buf, int vector4fCount);
}
