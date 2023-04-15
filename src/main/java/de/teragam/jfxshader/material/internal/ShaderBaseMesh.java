package de.teragam.jfxshader.material.internal;

import com.sun.prism.ResourceFactory;
import com.sun.prism.impl.BaseMesh;

import de.teragam.jfxshader.internal.ShaderController;

public class ShaderBaseMesh extends BaseMesh {

    private static int count = 0;

    private final BaseMeshHelper meshHelper;

    protected ShaderBaseMesh(BaseMeshHelper meshHelper) {
        super(meshHelper);
        this.meshHelper = meshHelper;
        count++;
    }

    public BaseMeshHelper getMeshHelper() {
        return this.meshHelper;
    }

    public static ShaderBaseMesh create(ResourceFactory resourceFactory) {
        return new ShaderBaseMesh(new D3DBaseMeshHelper(ShaderController.getD3DDevice(resourceFactory)));
    }

    @Override
    public boolean buildNativeGeometry(float[] vertexBuffer, int vertexBufferLength, int[] indexBufferInt, int indexBufferLength) {
        return this.meshHelper.buildNativeGeometry(vertexBuffer, vertexBufferLength, indexBufferInt, indexBufferLength);
    }

    @Override
    public boolean buildNativeGeometry(float[] vertexBuffer, int vertexBufferLength, short[] indexBufferShort, int indexBufferLength) {
        return this.meshHelper.buildNativeGeometry(vertexBuffer, vertexBufferLength, indexBufferShort, indexBufferLength);
    }

    @Override
    public int getCount() {
        return ShaderBaseMesh.count;
    }

    @Override
    public void dispose() {
        this.disposerRecord.dispose();
        ShaderBaseMesh.count--;
    }

}
