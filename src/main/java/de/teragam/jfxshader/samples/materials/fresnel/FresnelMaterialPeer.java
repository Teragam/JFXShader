package de.teragam.jfxshader.samples.materials.fresnel;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.prism.ps.Shader;
import com.sun.scenario.effect.impl.BufferUtil;

import de.teragam.jfxshader.ShaderDeclaration;
import de.teragam.jfxshader.material.ShaderMaterialPeer;

public class FresnelMaterialPeer extends ShaderMaterialPeer<FresnelMaterial> {

    @Override
    public ShaderDeclaration createVertexShaderDeclaration() {
        final Map<String, Integer> params = new HashMap<>();
        params.put("VSR_WORLDMATRIX", 35);
        return new ShaderDeclaration(null, params, ShaderMaterialPeer.class.getResourceAsStream("/samples/materials/fresnel/fresnel.vert"),
                ShaderMaterialPeer.class.getResourceAsStream("/samples/materials/fresnel/fresnel.vs.obj"));
    }

    @Override
    public ShaderDeclaration createPixelShaderDeclaration() {
        final Map<String, Integer> samplers = new HashMap<>();
        samplers.put("diffuseImage", 0);
        final Map<String, Integer> params = new HashMap<>();
        params.put("color", 0);
        params.put("strength", 1);
        return new ShaderDeclaration(samplers, params, FresnelMaterialPeer.class.getResourceAsStream("/samples/materials/fresnel/fresnel.frag"),
                FresnelMaterialPeer.class.getResourceAsStream("/samples/materials/fresnel/fresnel.ps.obj"));
    }

    private final FloatBuffer buf = BufferUtil.newFloatBuffer(12);

    @Override
    public void updateShader(Shader vertexShader, Shader pixelShader, FresnelMaterial material) {
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
        vertexShader.setConstants("VSR_WORLDMATRIX", this.buf, 0, 3);

        this.setTexture(material.getDiffuseImage(), 0);

        pixelShader.setConstant("strength", (float) material.getPower());
        pixelShader.setConstant("color", (float) material.getGlowColor().getRed(), (float) material.getGlowColor().getGreen(),
                (float) material.getGlowColor().getBlue(), (float) material.getGlowColor().getOpacity());
    }

}
