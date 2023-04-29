package de.teragam.jfxshader.material.internal.es2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javafx.scene.image.Image;

import com.sun.javafx.geom.transform.GeneralTransform3D;
import com.sun.prism.Graphics;
import com.sun.prism.Texture;
import com.sun.prism.impl.ps.BaseShaderContext;

import de.teragam.jfxshader.material.internal.AbstractShaderMaterialPeerRenderer;
import de.teragam.jfxshader.material.internal.ShaderMeshView;
import de.teragam.jfxshader.util.Reflect;

public class ES2ShaderMaterialPeerRenderer extends AbstractShaderMaterialPeerRenderer {

    @Override
    public void render(Graphics g, ShaderMeshView meshView, BaseShaderContext context, Map<Integer, Image> imageIndexMap) {
        if (!(meshView instanceof ES2ShaderMeshView)) {
            throw new IllegalArgumentException("meshView must be an instance of ES2ShaderMeshView");
        }
        final List<Texture> textures = new ArrayList<>();
        imageIndexMap.forEach((index, image) -> {
            final Texture texture = this.getTexture(context, image, false);
            Reflect.on(BaseShaderContext.class).method("updateTexture").invoke(context, index, texture);
            if (texture != null) {
                textures.add(texture);
            }
        });
        final Reflect<? extends BaseShaderContext> contextReflect = Reflect.on(context.getClass());
        final Object glContext = contextReflect.getFieldValue("glContext", context);
        Reflect.on("com.sun.prism.es2.GLContext").method("renderMeshView").invoke(glContext, ((ES2ShaderMeshView) meshView).getNativeHandle());

        textures.forEach(Texture::unlock);
    }

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
