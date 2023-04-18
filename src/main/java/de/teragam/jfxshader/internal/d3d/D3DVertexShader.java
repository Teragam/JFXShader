package de.teragam.jfxshader.internal.d3d;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Map;
import java.util.Optional;

import com.sun.prism.impl.Disposer;
import com.sun.prism.ps.Shader;

import de.teragam.jfxshader.internal.ShaderException;

public class D3DVertexShader implements Shader {

    private final IDirect3DDevice9 device;
    private final long nativeHandle;
    private final Map<String, Integer> registers;

    private boolean valid;

    public D3DVertexShader(IDirect3DDevice9 device, long nativeHandle, Map<String, Integer> registers) {
        this.device = device;
        this.nativeHandle = nativeHandle;
        this.registers = registers;
        this.valid = nativeHandle != 0;
        if (this.valid) {
            Disposer.addRecord(this, this::dispose);
        }
    }

    public static long init(IDirect3DDevice9 device, InputStream source) {
        try (source) {
            final long nativeHandle = device.createVertexShader(source.readAllBytes());
            if (nativeHandle == 0) {
                throw new ShaderException("Failed to create vertex shader");
            }
            return nativeHandle;
        } catch (IOException e) {
            throw new ShaderException("Failed to read vertex shader source", e);
        }
    }

    @Override
    public void enable() {
        this.device.setVertexShader(this.nativeHandle);
        this.validate();
    }

    @Override
    public void disable() {
        this.device.setVertexShader(0);
        this.validate();
    }

    @Override
    public boolean isValid() {
        return this.valid;
    }

    @Override
    public void setConstant(String name, int i0) {
        this.setConstants(name, i0);
    }

    @Override
    public void setConstant(String name, int i0, int i1) {
        this.setConstants(name, i0, i1);
    }

    @Override
    public void setConstant(String name, int i0, int i1, int i2) {
        this.setConstants(name, i0, i1, i2);
    }

    @Override
    public void setConstant(String name, int i0, int i1, int i2, int i3) {
        this.setConstants(name, i0, i1, i2, i3);
    }

    @Override
    public void setConstants(String name, IntBuffer buf, int off, int count) {
        final int[] data = new int[count * 4];
        buf.get(data, off * 4, count * 4);
        this.device.setVertexShaderConstantI(this.getRegister(name), data, count);
        this.validate();
    }

    private void setConstants(String name, int... data) {
        if (data.length > 0) {
            this.device.setVertexShaderConstantI(this.getRegister(name), data, Math.max(1, data.length / 4));
            this.validate();
        }
    }

    @Override
    public void setConstant(String name, float f0) {
        this.setConstants(name, f0);
    }

    @Override
    public void setConstant(String name, float f0, float f1) {
        this.setConstants(name, f0, f1);
    }

    @Override
    public void setConstant(String name, float f0, float f1, float f2) {
        this.setConstants(name, f0, f1, f2);
    }

    @Override
    public void setConstant(String name, float f0, float f1, float f2, float f3) {
        this.setConstants(name, f0, f1, f2, f3);
    }

    @Override
    public void setConstants(String name, FloatBuffer buf, int off, int count) {
        final float[] data = new float[count * 4];
        buf.get(data, off * 4, count * 4);
        this.device.setVertexShaderConstantF(this.getRegister(name), data, count);
        this.validate();
    }

    private void setConstants(String name, float... data) {
        if (data.length > 0) {
            this.device.setVertexShaderConstantF(this.getRegister(name), data, Math.max(1, data.length / 4));
            this.validate();
        }
    }

    @Override
    public void dispose() {
        this.valid = false;
        this.device.releaseResource(this.nativeHandle);
        this.validate();
    }

    private void validate() {
        if (this.device.getResultCode() != 0) {
            this.valid = false;
            throw new ShaderException("Vertex shader operation failed");
        }
    }

    private int getRegister(String name) {
        return Optional.ofNullable(this.registers.get(name)).orElseThrow(() -> new ShaderException("Register not found for: " + name));
    }
}
