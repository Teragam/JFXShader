package de.teragam.jfxshader.material;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.prism.Graphics;
import com.sun.prism.GraphicsPipeline;
import com.sun.prism.ps.Shader;
import com.sun.scenario.effect.impl.prism.PrFilterContext;

import de.teragam.jfxshader.ShaderDeclaration;
import de.teragam.jfxshader.internal.MaterialController;
import de.teragam.jfxshader.internal.Reflect;
import de.teragam.jfxshader.internal.ShaderController;
import de.teragam.jfxshader.internal.ShaderException;
import de.teragam.jfxshader.internal.d3d.D3D9Types;
import de.teragam.jfxshader.internal.d3d.IDirect3DDevice9;
import de.teragam.jfxshader.material.internal.D3DBaseMeshHelper;
import de.teragam.jfxshader.material.internal.ShaderBaseMesh;
import de.teragam.jfxshader.material.internal.ShaderMeshView;

public abstract class ShaderMaterialPeer<T extends ShaderMaterial> {

    private Shader vertexShader;
    private Shader pixelShader;

    private BaseTransform transform;

    /**
     * Size of a vertex in bytes.
     * struct PRISM_VERTEX_3D {
     * float x, y, z;
     * float tu, tv;
     * float nx, ny, nz, nw;
     * };
     */
    private static final int PRIMITIVE_VERTEX_SIZE = 4 * 3 + 4 * 2 + 4 * 4;

    public ShaderDeclaration createVertexShaderDeclaration() {
        InputStream es2Source = null;
        if (GraphicsPipeline.getPipeline().supportsShader(GraphicsPipeline.ShaderType.GLSL, GraphicsPipeline.ShaderModel.SM3)) {
            // TODO: Check whether a different class can be used here
            es2Source = Reflect.resolveClass("com.sun.prism.es2.ES2ResourceFactory").getResourceAsStream("glsl/main.vert");
        }
        final Map<String, Integer> params = new HashMap<>();
        params.put("VSR_LIGHT_POS", 10);
        params.put("VSR_LIGHT_DIRS", 20);
        params.put("VSR_LIGHT_COLOR", 35);
        return new ShaderDeclaration(null, params, es2Source, ShaderMaterialPeer.class.getResourceAsStream("/hlsl/Mtl1VS.obj"));
    }

    public void filter(Graphics g, ShaderMeshView meshView) {
        if (this.vertexShader == null) {
            this.vertexShader = this.createVertexShader(g);
        }
        if (this.pixelShader == null) {
            this.pixelShader = this.createPixelShader(g);
        }
        this.transform = g.getTransformNoClone();
        final ShaderBaseMesh mesh = meshView.getMesh();

        final IDirect3DDevice9 device = MaterialController.getD3DDevice(g.getResourceFactory());
        device.setFVF(D3D9Types.D3DFVF_XYZ | (2 << D3D9Types.D3DFVF_TEXCOUNT_SHIFT) | D3D9Types.D3DFVF_TEXCOORDSIZE4(1));
        if (device.getResultCode() != 0) {
            throw new ShaderException("Failed to set FVF");
        }
        this.vertexShader.enable();
        this.pixelShader.enable();
        this.updateShader(this.vertexShader, this.pixelShader, (T) meshView.getMaterial().getShaderMaterial());

        final int cullMode = translateCullMode(meshView.getCullingMode());
        final int previousRenderState = device.getRenderState(D3D9Types.D3DRS_CULLMODE);
        if (previousRenderState != cullMode) {
            device.setRenderState(D3D9Types.D3DRS_CULLMODE, cullMode);
        }
        final int fillMode = meshView.isWireframe() ? D3D9Types.D3DFILL_WIREFRAME : D3D9Types.D3DFILL_SOLID;
        final int previousFillMode = device.getRenderState(D3D9Types.D3DRS_FILLMODE);
        if (previousFillMode != fillMode) {
            device.setRenderState(D3D9Types.D3DRS_FILLMODE, fillMode);
        }
        final D3DBaseMeshHelper meshHelper = (D3DBaseMeshHelper) mesh.getMeshHelper();
        device.setStreamSource(0, meshHelper.getVertexBufferHandle(), 0, PRIMITIVE_VERTEX_SIZE);
        device.setIndices(meshHelper.getIndexBufferHandle());
        device.drawIndexedPrimitive(D3D9Types.D3DPT_TRIANGLELIST, 0, 0, (int) meshHelper.getNumVertices(), 0,
                (int) (meshHelper.getNumIndices() / 3));

        if (previousRenderState != cullMode) {
            device.setRenderState(D3D9Types.D3DRS_CULLMODE, previousRenderState);
        }
        if (previousFillMode != fillMode) {
            device.setRenderState(D3D9Types.D3DRS_FILLMODE, previousFillMode);
        }
    }

    private Shader createPixelShader(Graphics g) {
        return ShaderController.createShader(PrFilterContext.getInstance(g.getAssociatedScreen()), this.createPixelShaderDeclaration());
    }

    private Shader createVertexShader(Graphics g) {
        return ShaderController.createVertexShader(PrFilterContext.getInstance(g.getAssociatedScreen()), this.createVertexShaderDeclaration());
    }

    private static int translateCullMode(int cullFace) {
        switch (cullFace) {
            case 0:
                return D3D9Types.D3DCULL_NONE;
            case 1:
                return D3D9Types.D3DCULL_CW;
            case 2:
                return D3D9Types.D3DCULL_CCW;
            default:
                throw new IllegalArgumentException("Unknown cull mode: " + cullFace);
        }
    }

    protected BaseTransform getTransform() {
        return this.transform;
    }

    public abstract ShaderDeclaration createPixelShaderDeclaration();

    protected abstract void updateShader(Shader vertexShader, Shader pixelShader, T material);

}
