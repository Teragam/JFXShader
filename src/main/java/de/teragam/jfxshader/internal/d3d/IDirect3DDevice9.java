package de.teragam.jfxshader.internal.d3d;

public class IDirect3DDevice9 {

    private final long handle;
    private int resultCode;

    public IDirect3DDevice9(long handle) {
        this.handle = handle;
    }

    public long getHandle() {
        return this.handle;
    }

    /**
     * @return the HRESULT code of the last operation.
     */
    public int getResultCode() {
        return this.resultCode;
    }

    public native void setFVF(int fvf);

    public native void setVertexShader(long pShader);

    public native long createVertexShader(byte[] pFunction);

    public native void setVertexShaderConstantF(int startRegister, float[] pConstantData, int vector4fCount);

    public native void setVertexShaderConstantI(int startRegister, int[] pConstantData, int vector4iCount);

    public native void setPixelShader(long pShader);

    public native void setPixelShaderConstantF(int startRegister, float[] pConstantData, int vector4fCount);

    public native void setTexture(int stage, long pTexture);

    public native void setRenderState(int state, int value);

    public native int getRenderState(int state);

    public native void setStreamSource(int streamNumber, long pStreamData, int offsetInBytes, int stride);

    public native void setIndices(long pIndexData);

    public native void drawIndexedPrimitive(int primitiveType, int baseVertexIndex, int minVertexIndex, int numVertices, int startIndex, int primCount);

    public native void releaseResource(long pResource);

    public native long createVertexBuffer(int length, int usage, int fvf, int pool, long pSharedHandle);

    public native long createIndexBuffer(int length, int usage, int format, int pool, long pSharedHandle);

    public native void uploadVertexBufferData(long pVertexBuffer, float[] vertexBuffer, int vertexBufferLength);

    public native void uploadIndexBufferDataInt(long pIndexBuffer, int[] indexBufferInt, int indexBufferLength);

    public native void uploadIndexBufferDataShort(long pIndexBuffer, short[] indexBufferShort, int indexBufferLength);

}
