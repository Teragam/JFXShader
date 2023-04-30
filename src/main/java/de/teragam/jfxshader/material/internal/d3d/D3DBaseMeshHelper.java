package de.teragam.jfxshader.material.internal.d3d;

import de.teragam.jfxshader.material.internal.BaseMeshHelper;

public final class D3DBaseMeshHelper implements BaseMeshHelper {

    private long vertexBufferHandle;
    private long indexBufferHandle;
    private long numVertices;
    private long numIndices;
    private final IDirect3DDevice9 device;

    public D3DBaseMeshHelper(IDirect3DDevice9 device) {
        this.device = device;
    }

    private static final int PRIMITIVE_VERTEX_SIZE = 36;

    @Override
    public boolean buildNativeGeometry(float[] vertexBuffer, int vertexBufferLength, int[] indexBufferInt, int indexBufferLength) {
        if (!this.buildVertexBuffer(vertexBuffer, vertexBufferLength)) {
            return false;
        }

        final int indexSize = indexBufferLength * 4;
        if (this.numIndices != indexBufferLength) {
            this.device.releaseResource(this.indexBufferHandle);
            this.indexBufferHandle = this.device.createIndexBuffer(indexSize, D3D9Types.D3DUSAGE_WRITEONLY, D3D9Types.D3DFMT_INDEX32,
                    D3D9Types.D3DPOOL_DEFAULT, 0);
            this.numIndices = indexBufferLength;
            if (this.device.getResultCode() != 0 || this.indexBufferHandle == 0) {
                return false;
            }
        }
        this.device.uploadIndexBufferDataInt(this.indexBufferHandle, indexBufferInt, indexSize);

        return this.device.getResultCode() == 0;
    }

    @Override
    public boolean buildNativeGeometry(float[] vertexBuffer, int vertexBufferLength, short[] indexBufferShort, int indexBufferLength) {
        if (!this.buildVertexBuffer(vertexBuffer, vertexBufferLength)) {
            return false;
        }

        final int indexSize = indexBufferLength * 4;
        if (this.numIndices != indexBufferLength) {
            this.device.releaseResource(this.indexBufferHandle);
            this.indexBufferHandle = this.device.createIndexBuffer(indexSize, D3D9Types.D3DUSAGE_WRITEONLY, D3D9Types.D3DFMT_INDEX16,
                    D3D9Types.D3DPOOL_DEFAULT, 0);
            this.numIndices = indexBufferLength;
            if (this.device.getResultCode() != 0 || this.indexBufferHandle == 0) {
                return false;
            }
        }
        this.device.uploadIndexBufferDataShort(this.indexBufferHandle, indexBufferShort, indexSize);

        return this.device.getResultCode() == 0;
    }

    @Override
    public void dispose() {
        this.device.releaseResource(this.vertexBufferHandle);
        if (this.device.getResultCode() == 0) {
            this.vertexBufferHandle = 0;
        }
        this.device.releaseResource(this.indexBufferHandle);
        if (this.device.getResultCode() == 0) {
            this.indexBufferHandle = 0;
        }
    }

    private boolean buildVertexBuffer(float[] vertexBuffer, int vertexBufferLength) {
        final int size = vertexBufferLength * 4;
        final int vbCount = vertexBufferLength / PRIMITIVE_VERTEX_SIZE;
        if (this.numVertices != vbCount) {
            this.device.releaseResource(this.vertexBufferHandle);
            final int fvf = D3D9Types.D3DFVF_XYZ | (2 << D3D9Types.D3DFVF_TEXCOUNT_SHIFT) | D3D9Types.D3DFVF_TEXCOORDSIZE4(1);
            this.vertexBufferHandle = this.device.createVertexBuffer(size, D3D9Types.D3DUSAGE_WRITEONLY, fvf,
                    D3D9Types.D3DPOOL_DEFAULT, 0);
            this.numVertices = vbCount;
            if (this.device.getResultCode() != 0 || this.vertexBufferHandle == 0) {
                return false;
            }
        }
        this.device.uploadVertexBufferData(this.vertexBufferHandle, vertexBuffer, size);
        return this.device.getResultCode() == 0;
    }

    public long getVertexBufferHandle() {
        return this.vertexBufferHandle;
    }

    public long getIndexBufferHandle() {
        return this.indexBufferHandle;
    }

    public long getNumVertices() {
        return this.numVertices;
    }

    public long getNumIndices() {
        return this.numIndices;
    }
}
