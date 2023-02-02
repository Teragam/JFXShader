package de.teragam.jfxshader.internal;

public class IDirect3DDevice9Wrapper {

    private final long pDevice;

    public IDirect3DDevice9Wrapper(long pDevice) {
        this.pDevice = pDevice;
    }

    public long getDevice() {
        return this.pDevice;
    }

    public native int setFVF(int fvf);
    public native int setVertexShader(long pShader);
    public native long createVertexShader(byte[] pFunction);
    public native int setVertexShaderConstantF(int startRegister, float[] pConstantData, int vector4fCount);
    public native int setPixelShader(long pShader);
    public native int setPixelShaderConstantF(int startRegister, float[] pConstantData, int vector4fCount);
    public native int setTexture(int stage, long pTexture);
    public native int setRenderState(int state, int value);
    public native int setStreamSource(int streamNumber, long pStreamData, int offsetInBytes, int stride);
    public native int setIndices(long pIndexData);
    public native int drawIndexedPrimitive(int primitiveType, int baseVertexIndex, int minVertexIndex, int numVertices, int startIndex, int primCount);

}
