package de.teragam.jfxshader.material.internal.d3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javafx.scene.image.Image;

import com.sun.javafx.geom.transform.GeneralTransform3D;
import com.sun.prism.Graphics;
import com.sun.prism.Texture;
import com.sun.prism.impl.ps.BaseShaderContext;

import de.teragam.jfxshader.MaterialController;
import de.teragam.jfxshader.exception.ShaderException;
import de.teragam.jfxshader.material.internal.AbstractShaderMaterialPeerRenderer;
import de.teragam.jfxshader.material.internal.ShaderBaseMesh;
import de.teragam.jfxshader.material.internal.ShaderMeshView;
import de.teragam.jfxshader.util.Reflect;

public class D3DShaderMaterialPeerRenderer extends AbstractShaderMaterialPeerRenderer {

    /**
     * Size of a vertex in bytes.
     * struct PRISM_VERTEX_3D {
     * float x, y, z;
     * float tu, tv;
     * float nx, ny, nz, nw;
     * };
     */
    private static final int PRIMITIVE_VERTEX_SIZE = 4 * 3 + 4 * 2 + 4 * 4;

    @Override
    public void render(Graphics g, ShaderMeshView meshView, BaseShaderContext context, Map<Integer, Image> imageIndexMap) {
        final ShaderBaseMesh mesh = (ShaderBaseMesh) meshView.getMesh();

        final Direct3DDevice9 device = MaterialController.getD3DDevice(g.getResourceFactory());
        device.setFVF(D3D9Types.D3DFVF_XYZ | (2 << D3D9Types.D3DFVF_TEXCOUNT_SHIFT) | D3D9Types.D3DFVF_TEXCOORDSIZE4(1));
        if (device.getResultCode() != 0) {
            throw new ShaderException("Failed to set FVF");
        }
        final List<Texture> textures = new ArrayList<>();
        imageIndexMap.forEach((index, image) -> {
            final Texture texture = this.getTexture(context, image, false);
            device.setTexture(index, getD3DTextureHandle(texture));
            if (texture != null) {
                textures.add(texture);
            }
        });

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

        textures.forEach(Texture::unlock);
    }

    @Override
    protected float[] convertMatrix(GeneralTransform3D src) {
        final float[] rawMatrix = new float[16];
        for (int i = 0; i < rawMatrix.length; i++) {
            rawMatrix[i] = (float) src.get(i);
        }
        return rawMatrix;
    }

    private static long getD3DTextureHandle(Texture texture) {
        if (texture == null) {
            return 0;
        }
        return Reflect.on("com.sun.prism.d3d.D3DTexture").<Long>method("getNativeTextureObject").invoke(texture);
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

}
