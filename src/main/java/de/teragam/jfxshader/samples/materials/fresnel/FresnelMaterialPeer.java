package de.teragam.jfxshader.samples.materials.fresnel;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import com.sun.javafx.geom.Vec3d;
import com.sun.prism.es2.ES2Shader;
import com.sun.prism.ps.Shader;
import com.sun.scenario.effect.impl.BufferUtil;

import de.teragam.jfxshader.ShaderController;
import de.teragam.jfxshader.ShaderDeclaration;
import de.teragam.jfxshader.material.ShaderMaterialPeer;

public class FresnelMaterialPeer extends ShaderMaterialPeer<FresnelMaterial> {

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
    public ShaderDeclaration createPixelShaderDeclaration() {
        final Map<String, Integer> samplers = new HashMap<>();
        samplers.put("diffuseImage", 0);
        final Map<String, Integer> params = new HashMap<>();
        params.put("color", 0);
        params.put("strength", 1);
        return new ShaderDeclaration(samplers, params, FresnelMaterialPeer.class.getResourceAsStream("/samples/materials/fresnel/fresnel.frag"),
                FresnelMaterialPeer.class.getResourceAsStream("/samples/materials/fresnel/fresnel.ps.obj"));
    }

    private final FloatBuffer buf = BufferUtil.newFloatBuffer(16);

    @Override
    public void updateShader(Shader vertexShader, Shader pixelShader, FresnelMaterial material) {
        final Vec3d camPos = this.getCamera().getPositionInWorld(null);
        vertexShader.setConstant("camPos", (float) camPos.x, (float) camPos.y, (float) camPos.z);

        if (ShaderController.isGLSLSupported()) {
            ((ES2Shader) vertexShader).setMatrix("viewProjectionMatrix", this.getViewProjectionMatrix());
            ((ES2Shader) vertexShader).setMatrix("worldMatrix", this.getWorldMatrix());
        } else {
            final float[] viewProjectionMatrix = this.getViewProjectionMatrix();
            this.buf.clear();
            this.buf.put(viewProjectionMatrix);
            this.buf.rewind();
            vertexShader.setConstants("viewProjectionMatrix", this.buf, 0, 4);

            final float[] worldMatrix = this.getWorldMatrix();
            this.buf.clear();
            this.buf.put(worldMatrix);
            this.buf.rewind();
            vertexShader.setConstants("worldMatrix", this.buf, 0, 4);
        }

        this.setSamplerImage(material.getDiffuseImage(), 0);

        pixelShader.setConstant("strength", (float) material.getPower());
        pixelShader.setConstant("color", (float) material.getGlowColor().getRed(), (float) material.getGlowColor().getGreen(),
                (float) material.getGlowColor().getBlue(), (float) material.getGlowColor().getOpacity());
    }

}
