package de.teragam.jfxshader.material.internal.es2;

import com.sun.javafx.geom.transform.GeneralTransform3D;

import de.teragam.jfxshader.material.internal.AbstractShaderMaterialPeerRenderer;

public class ES2ShaderMaterialPeerRenderer extends AbstractShaderMaterialPeerRenderer {

    @Override
    protected float[] convertMatrix(GeneralTransform3D src) {
        final float[] rawMatrix = new float[16];
        rawMatrix[0] = (float) src.get(0); // Scale X
        rawMatrix[1] = (float) src.get(4); // Shear Y
        rawMatrix[2] = (float) src.get(8);
        rawMatrix[3] = (float) src.get(12);
        rawMatrix[4] = (float) src.get(1); // Shear X
        rawMatrix[5] = (float) src.get(5); // Scale Y
        rawMatrix[6] = (float) src.get(9);
        rawMatrix[7] = (float) src.get(13);
        rawMatrix[8] = (float) src.get(2);
        rawMatrix[9] = (float) src.get(6);
        rawMatrix[10] = (float) src.get(10);
        rawMatrix[11] = (float) src.get(14);
        rawMatrix[12] = (float) src.get(3);  // Translate X
        rawMatrix[13] = (float) src.get(7);  // Translate Y
        rawMatrix[14] = (float) src.get(11);
        rawMatrix[15] = (float) src.get(15);
        return rawMatrix;
    }

}
