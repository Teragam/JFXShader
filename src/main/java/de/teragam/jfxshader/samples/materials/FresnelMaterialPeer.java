package de.teragam.jfxshader.samples.materials;

import java.util.HashMap;
import java.util.Map;

import com.sun.javafx.geom.Vec3d;

import de.teragam.jfxshader.JFXShader;
import de.teragam.jfxshader.ShaderDeclaration;
import de.teragam.jfxshader.material.ShaderMaterialPeer;

public class FresnelMaterialPeer extends ShaderMaterialPeer<FresnelMaterial> {

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

    @Override
    public ShaderDeclaration createVertexShaderDeclaration() {
        final Map<String, Integer> params = new HashMap<>();
        params.put("viewProjectionMatrix", 0);
        params.put("camPos", 4);
        params.put("worldMatrix", 35);
        return new ShaderDeclaration(null, params, ShaderMaterialPeer.class.getResourceAsStream("/samples/materials/fresnel/fresnel.vert"),
                ShaderMaterialPeer.class.getResourceAsStream("/samples/materials/fresnel/fresnel.vs.obj"));
    }

    @Override
    public void updateShader(JFXShader vertexShader, JFXShader pixelShader, FresnelMaterial material) {
        final Vec3d camPos = this.getCamera().getPositionInWorld(null);
        vertexShader.setConstant("camPos", (float) camPos.x, (float) camPos.y, (float) camPos.z);
        vertexShader.setMatrix("viewProjectionMatrix", this.getViewProjectionMatrix(), 4);
        vertexShader.setMatrix("worldMatrix", this.getWorldMatrix(), 4);

        this.setSamplerImage(material.getDiffuseImage(), 0);

        pixelShader.setConstant("strength", (float) material.getPower());
        pixelShader.setConstant("color", (float) material.getGlowColor().getRed(), (float) material.getGlowColor().getGreen(),
                (float) material.getGlowColor().getBlue(), (float) material.getGlowColor().getOpacity());
    }

}
