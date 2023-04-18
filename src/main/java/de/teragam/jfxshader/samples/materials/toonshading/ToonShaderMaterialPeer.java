package de.teragam.jfxshader.samples.materials.toonshading;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.prism.ps.Shader;
import com.sun.scenario.effect.impl.BufferUtil;

import de.teragam.jfxshader.ShaderDeclaration;
import de.teragam.jfxshader.material.ShaderMaterialPeer;

public class ToonShaderMaterialPeer extends ShaderMaterialPeer<ToonShaderMaterial> {

    @Override
    public ShaderDeclaration createPixelShaderDeclaration() {
        final Map<String, Integer> samplers = new HashMap<>();
        samplers.put("baseImg", 0);
        final Map<String, Integer> params = new HashMap<>();
        params.put("edgeWidth", 0);
        return new ShaderDeclaration(samplers, params, null, ToonShaderMaterialPeer.class.getResourceAsStream("/hlsl/Mtl1PS.obj"));
    }

    private final FloatBuffer buf = BufferUtil.newFloatBuffer(12);

    @Override
    public void updateShader(Shader vertexShader, Shader pixelShader, ToonShaderMaterial material) {
        this.buf.clear();
        this.buf.put(new float[12]);
        this.buf.rewind();
        vertexShader.setConstants("VSR_LIGHT_POS", this.buf, 0, 3);
        this.buf.rewind();
        vertexShader.setConstants("VSR_LIGHT_DIRS", this.buf, 0, 3);

        final float[] transform = new float[12];
        final BaseTransform baseTransform = this.getTransform();
        transform[0] = (float) baseTransform.getMxx();
        transform[1] = (float) baseTransform.getMxy();
        transform[2] = (float) baseTransform.getMxz();
        transform[3] = (float) baseTransform.getMxt();
        transform[4] = (float) baseTransform.getMyx();
        transform[5] = (float) baseTransform.getMyy();
        transform[6] = (float) baseTransform.getMyz();
        transform[7] = (float) baseTransform.getMyt();
        transform[8] = (float) baseTransform.getMzx();
        transform[9] = (float) baseTransform.getMzy();
        transform[10] = (float) baseTransform.getMzz();
        transform[11] = (float) baseTransform.getMzt();
        this.buf.clear();
        this.buf.put(transform);
        this.buf.rewind();
        vertexShader.setConstants("VSR_LIGHT_COLOR", this.buf, 0, 3);

        pixelShader.setConstant("edgeWidth", (float) material.getEdgeWidth());
    }

}
