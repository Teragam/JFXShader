package de.teragam.jfxshader.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.prism.Graphics;
import com.sun.prism.MeshView;
import com.sun.prism.PhongMaterial;
import com.sun.prism.impl.BaseContext;
import com.sun.prism.impl.BaseGraphics;
import com.sun.prism.ps.Shader;
import com.sun.scenario.effect.impl.prism.PrFilterContext;

import de.teragam.jfxshader.ShaderDeclaration;
import de.teragam.jfxshader.internal.d3d.D3D9Types;
import de.teragam.jfxshader.internal.d3d.IDirect3DDevice9;
import de.teragam.jfxshader.material.internal.D3DBaseMeshHelper;
import de.teragam.jfxshader.material.internal.MeshProxyHelper;
import de.teragam.jfxshader.material.internal.ShaderBaseMesh;
import de.teragam.jfxshader.material.internal.ShaderMeshView;

public final class MeshRendererHelper {

    private MeshRendererHelper() {}

    public static void renderMeshView(MeshView meshView, Graphics g) {
        final PhongMaterial material = ReflectionHelper.getFieldValue(meshView.getClass(), "material", meshView);
        material.lockTextureMaps();
        if (g instanceof MeshProxyHelper.GraphicsHelper && meshView instanceof ShaderMeshView) {
            final BaseContext context = ReflectionHelper.getFieldValue(BaseGraphics.class, "context", ((MeshProxyHelper.GraphicsHelper) g).getRawGraphics());
            ReflectionHelper.invokeMethod(context.getClass(), "renderMeshView", long.class, Graphics.class).invoke(context, 0, g);
            nativeRenderMeshView(((MeshProxyHelper.GraphicsHelper) g).getRawGraphics(), (ShaderBaseMesh) ((ShaderMeshView) meshView).getMesh(),
                    ((ShaderMeshView) meshView));
        }
        material.unlockTextureMaps();
    }

    private static long vertexShader;
    private static Shader pixelShader;

    private static long getVertexShader(Graphics g) {
        if (vertexShader == 0) {
            final IDirect3DDevice9 device = ShaderController.getD3DDevice(g.getResourceFactory());
            final InputStream shader = ShaderController.class.getResourceAsStream("/hlsl/Mtl1VS.obj");
            try {
                vertexShader = device.createVertexShader(shader.readAllBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return vertexShader;
    }

    private static Shader getPixelShader(Graphics g) {
        if (pixelShader == null) {
            final Map<String, Integer> samplers = new HashMap<>();
            samplers.put("baseImg", 0);
            final Map<String, Integer> params = new HashMap<>();
            pixelShader = ShaderController.createShader(PrFilterContext.getInstance(g.getAssociatedScreen()),
                    new ShaderDeclaration(samplers, params, null, MeshRendererHelper.class.getResourceAsStream("/hlsl/Mtl1PS.obj")));
        }
        return pixelShader;
    }

    /**
     * Size of a vertex in bytes.
     * struct PRISM_VERTEX_3D {
     * float x, y, z;
     * float tu, tv;
     * float nx, ny, nz, nw;
     * };
     */
    private static final int PRIMITIVE_VERTEX_SIZE = 4 * 3 + 4 * 2 + 4 * 4;

    private static void nativeRenderMeshView(Graphics g, ShaderBaseMesh mesh, ShaderMeshView meshView) {
        final IDirect3DDevice9 device = ShaderController.getD3DDevice(g.getResourceFactory());
        device.setFVF(D3D9Types.D3DFVF_XYZ | (2 << D3D9Types.D3DFVF_TEXCOUNT_SHIFT) | D3D9Types.D3DFVF_TEXCOORDSIZE4(1));
        if (device.getResultCode() != 0) {
            throw new ShaderException("Failed to set FVF");
        }
        device.setVertexShader(getVertexShader(g));
        if (device.getResultCode() != 0) {
            throw new ShaderException("Failed to set vertex shader");
        }
        device.setVertexShaderConstantF(10, new float[12], 3);
        if (device.getResultCode() != 0) {
            throw new ShaderException("Failed to set VSR_LIGHT_POS vertex shader constant");
        }
        device.setVertexShaderConstantF(20, new float[12], 3);
        if (device.getResultCode() != 0) {
            throw new ShaderException("Failed to set VSR_LIGHT_DIRS vertex shader constant");
        }
        final float[] transform = new float[16];
        final BaseTransform baseTransform = g.getTransformNoClone();
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
        transform[12] = 0F;
        transform[13] = 0F;
        transform[14] = 0F;
        transform[15] = 1F;

        //TODO verify if transpose is needed here: transpose(transform)
        device.setVertexShaderConstantF(35, transform, 3);
        if (device.getResultCode() != 0) {
            throw new ShaderException("Failed to set VSR_LIGHT_COLOR vertex shader constant");
        }

        final Shader shader = getPixelShader(g);
        shader.enable();

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

    private static float[] transpose(float[] transform) {
        final float[] result = new float[16];
        result[0] = transform[0];
        result[1] = transform[4];
        result[2] = transform[8];
        result[3] = transform[12];
        result[4] = transform[1];
        result[5] = transform[5];
        result[6] = transform[9];
        result[7] = transform[13];
        result[8] = transform[2];
        result[9] = transform[6];
        result[10] = transform[10];
        result[11] = transform[14];
        result[12] = transform[3];
        result[13] = transform[7];
        result[14] = transform[11];
        result[15] = transform[15];
        return result;
    }

}
