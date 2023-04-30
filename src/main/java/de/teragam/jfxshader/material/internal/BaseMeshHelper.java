package de.teragam.jfxshader.material.internal;

import com.sun.prism.impl.Disposer;

public interface BaseMeshHelper extends Disposer.Record {
    boolean buildNativeGeometry(float[] vertexBuffer, int vertexBufferLength, int[] indexBufferInt, int indexBufferLength);

    boolean buildNativeGeometry(float[] vertexBuffer, int vertexBufferLength, short[] indexBufferShort, int indexBufferLength);
}
