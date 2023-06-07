package de.teragam.jfxshader.material;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.image.Image;

import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.sg.prism.NGCamera;
import com.sun.javafx.sg.prism.NGLightBase;
import com.sun.prism.Graphics;
import com.sun.prism.MeshView;
import com.sun.prism.impl.ps.BaseShaderContext;
import com.sun.scenario.effect.impl.prism.PrFilterContext;

import de.teragam.jfxshader.JFXShader;
import de.teragam.jfxshader.ShaderController;
import de.teragam.jfxshader.ShaderDeclaration;
import de.teragam.jfxshader.material.internal.AbstractShaderMaterialPeerRenderer;
import de.teragam.jfxshader.material.internal.ShaderMeshView;
import de.teragam.jfxshader.material.internal.d3d.D3DShaderMaterialPeerRenderer;
import de.teragam.jfxshader.material.internal.es2.ES2ShaderMaterialPeerRenderer;
import de.teragam.jfxshader.material.internal.es2.ES2ShaderMeshView;

public abstract class ShaderMaterialPeer<T extends ShaderMaterial> {

    private JFXShader vertexShader;
    private JFXShader pixelShader;
    private JFXShader es2Shader;

    private BaseTransform transform;
    private NGLightBase[] lights;
    private NGCamera camera;

    private final Map<Integer, Image> imageIndexMap;
    private final AbstractShaderMaterialPeerRenderer peerRenderer;

    protected ShaderMaterialPeer() {
        this.imageIndexMap = new HashMap<>();
        if (ShaderController.isGLSLSupported()) {
            this.peerRenderer = new ES2ShaderMaterialPeerRenderer();
        } else {
            this.peerRenderer = new D3DShaderMaterialPeerRenderer();
        }
    }

    public Map<String, Integer> getES2ShaderAttributes() {
        final Map<String, Integer> attributes = new HashMap<>();
        attributes.put("pos", 0);
        attributes.put("texCoords", 1);
        attributes.put("tangent", 2);
        return attributes;
    }

    protected void setSamplerImage(Image image, int index) {
        this.imageIndexMap.put(index, image);
    }

    public final void filter(Graphics g, MeshView meshView, BaseShaderContext context) {
        this.transform = g.getTransformNoClone();
        this.lights = g.getLights();
        this.camera = g.getCameraNoClone();

        if (ShaderController.isHLSLSupported()) {
            this.filterD3D(g, (ShaderMeshView) meshView, context);
        } else if (ShaderController.isGLSLSupported()) {
            this.filterES2(g, (ES2ShaderMeshView) meshView, context);
        }
    }

    protected void filterD3D(Graphics g, ShaderMeshView meshView, BaseShaderContext context) {
        if (this.vertexShader == null) {
            this.vertexShader = this.createVertexShader(g);
        }
        if (this.pixelShader == null) {
            this.pixelShader = this.createPixelShader(g);
        }
        this.vertexShader.enable();
        this.pixelShader.enable();
        this.imageIndexMap.clear();
        this.peerRenderer.updateMatrices(g, context);
        this.updateShader(this.vertexShader, this.pixelShader, (T) meshView.getMaterial().getShaderMaterial());
        this.peerRenderer.render(g, meshView, context, this.imageIndexMap);
    }

    protected void filterES2(Graphics g, ES2ShaderMeshView meshView, BaseShaderContext context) {
        if (this.es2Shader == null) {
            this.es2Shader = this.createES2ShaderProgram(g);
        }
        this.es2Shader.enable();
        this.imageIndexMap.clear();
        this.peerRenderer.updateMatrices(g, context);
        this.updateShader(this.es2Shader, this.es2Shader, (T) meshView.getMaterial().getShaderMaterial());
        this.peerRenderer.render(g, meshView, context, this.imageIndexMap);
    }

    private JFXShader createPixelShader(Graphics g) {
        return ShaderController.createShader(PrFilterContext.getInstance(g.getAssociatedScreen()), this.createPixelShaderDeclaration());
    }

    private JFXShader createVertexShader(Graphics g) {
        return ShaderController.createVertexShader(PrFilterContext.getInstance(g.getAssociatedScreen()), this.createVertexShaderDeclaration());
    }

    private JFXShader createES2ShaderProgram(Graphics g) {
        return ShaderController.createES2ShaderProgram(PrFilterContext.getInstance(g.getAssociatedScreen()),
                this.createVertexShaderDeclaration(), this.createPixelShaderDeclaration(), this.getES2ShaderAttributes());
    }

    protected BaseTransform getTransform() {
        return this.transform;
    }

    protected NGLightBase[] getLights() {
        return this.lights;
    }

    protected NGCamera getCamera() {
        return this.camera;
    }

    protected float[] getWorldMatrix() {
        return this.peerRenderer.getWorldMatrix();
    }

    protected float[] getViewProjectionMatrix() {
        return this.peerRenderer.getViewProjectionMatrix();
    }

    public abstract ShaderDeclaration createPixelShaderDeclaration();

    public abstract ShaderDeclaration createVertexShaderDeclaration();

    protected abstract void updateShader(JFXShader vertexShader, JFXShader pixelShader, T material);

}
