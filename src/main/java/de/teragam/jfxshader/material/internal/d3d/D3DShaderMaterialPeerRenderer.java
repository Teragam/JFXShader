package de.teragam.jfxshader.material.internal.d3d;

import com.sun.javafx.geom.transform.GeneralTransform3D;

import de.teragam.jfxshader.material.internal.AbstractShaderMaterialPeerRenderer;

public class D3DShaderMaterialPeerRenderer extends AbstractShaderMaterialPeerRenderer {

    @Override
    protected float[] convertMatrix(GeneralTransform3D src) {
        final float[] rawMatrix = new float[16];
        for (int i = 0; i < rawMatrix.length; i++) {
            rawMatrix[i] = (float) src.get(i);
        }
        return rawMatrix;
    }

}
