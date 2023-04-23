package de.teragam.jfxshader.material;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.scene.image.Image;

import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.sg.prism.NGCamera;
import com.sun.javafx.sg.prism.NGLightBase;
import com.sun.javafx.tk.Toolkit;
import com.sun.prism.Graphics;
import com.sun.prism.MeshView;
import com.sun.prism.Texture;
import com.sun.prism.es2.ES2Shader;
import com.sun.prism.impl.ps.BaseShaderContext;
import com.sun.prism.ps.Shader;
import com.sun.scenario.effect.impl.prism.PrFilterContext;

import de.teragam.jfxshader.MaterialController;
import de.teragam.jfxshader.ShaderController;
import de.teragam.jfxshader.ShaderDeclaration;
import de.teragam.jfxshader.exception.ShaderException;
import de.teragam.jfxshader.material.internal.AbstractShaderMaterialPeerRenderer;
import de.teragam.jfxshader.material.internal.ShaderBaseMesh;
import de.teragam.jfxshader.material.internal.ShaderMeshView;
import de.teragam.jfxshader.material.internal.d3d.D3D9Types;
import de.teragam.jfxshader.material.internal.d3d.D3DBaseMeshHelper;
import de.teragam.jfxshader.material.internal.d3d.D3DShaderMaterialPeerRenderer;
import de.teragam.jfxshader.material.internal.d3d.IDirect3DDevice9;
import de.teragam.jfxshader.material.internal.es2.ES2ShaderMaterialPeerRenderer;
import de.teragam.jfxshader.material.internal.es2.ES2ShaderMeshView;
import de.teragam.jfxshader.util.Reflect;

public abstract class ShaderMaterialPeer<T extends ShaderMaterial> {

    private Shader vertexShader;
    private Shader pixelShader;
    private ES2Shader es2Shader;

    private BaseTransform transform;
    private NGLightBase[] lights;
    private NGCamera camera;

    private final Map<Integer, Image> imageIndexMap;
    private final AbstractShaderMaterialPeerRenderer peerRenderer;

    /**
     * Size of a vertex in bytes.
     * struct PRISM_VERTEX_3D {
     * float x, y, z;
     * float tu, tv;
     * float nx, ny, nz, nw;
     * };
     */
    private static final int PRIMITIVE_VERTEX_SIZE = 4 * 3 + 4 * 2 + 4 * 4;

    protected ShaderMaterialPeer() {
        this.imageIndexMap = new HashMap<>();
        if (ShaderController.isGLSLSupported()) {
            this.peerRenderer = new ES2ShaderMaterialPeerRenderer();
        } else {
            this.peerRenderer = new D3DShaderMaterialPeerRenderer();
        }
    }

    public ShaderDeclaration createVertexShaderDeclaration() {
        InputStream es2Source = null;
        if (ShaderController.isGLSLSupported()) {
            es2Source = Reflect.resolveClass("com.sun.prism.es2.ES2ResourceFactory").getResourceAsStream("glsl/main.vert");
        }
        final Map<String, Integer> params = new HashMap<>();
        params.put("viewProjectionMatrix", 0);
        params.put("VSR_LIGHT_POS", 10);
        params.put("VSR_LIGHT_DIRS", 20);
        params.put("worldMatrix", 35);
        return new ShaderDeclaration(null, params, es2Source, ShaderMaterialPeer.class.getResourceAsStream("/hlsl/Mtl1VS.obj"));
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

    public void filterD3D(Graphics g, ShaderMeshView meshView, BaseShaderContext context) {
        if (this.vertexShader == null) {
            this.vertexShader = this.createVertexShader(g);
        }
        if (this.pixelShader == null) {
            this.pixelShader = this.createPixelShader(g);
        }
        final ShaderBaseMesh mesh = (ShaderBaseMesh) meshView.getMesh();

        this.vertexShader.enable();
        this.pixelShader.enable();
        this.imageIndexMap.clear();
        this.peerRenderer.updateMatrices(g, context);
        this.updateShader(this.vertexShader, this.pixelShader, (T) meshView.getMaterial().getShaderMaterial());

        final IDirect3DDevice9 device = MaterialController.getD3DDevice(g.getResourceFactory());
        device.setFVF(D3D9Types.D3DFVF_XYZ | (2 << D3D9Types.D3DFVF_TEXCOUNT_SHIFT) | D3D9Types.D3DFVF_TEXCOORDSIZE4(1));
        if (device.getResultCode() != 0) {
            throw new ShaderException("Failed to set FVF");
        }
        final List<Texture> textures = new ArrayList<>();
        this.imageIndexMap.forEach((index, image) -> {
            final Texture texture = this.getTexture(context, image, false);
            device.setTexture(index, this.getD3DTextureHandle(texture));
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

    public void filterES2(Graphics g, ES2ShaderMeshView meshView, BaseShaderContext context) {
        if (this.es2Shader == null) {
            this.es2Shader = this.createES2ShaderProgram(g);
        }
        this.es2Shader.enable();
        this.peerRenderer.updateMatrices(g, context);
        this.imageIndexMap.clear();
        this.updateShader(this.es2Shader, this.es2Shader, (T) meshView.getMaterial().getShaderMaterial());

        final List<Texture> textures = new ArrayList<>();
        this.imageIndexMap.forEach((index, image) -> {
            final Texture texture = this.getTexture(context, image, false);
            Reflect.on(BaseShaderContext.class).method("updateTexture").invoke(context, index, texture);
            if (texture != null) {
                textures.add(texture);
            }
        });
        final Reflect<? extends BaseShaderContext> contextReflect = Reflect.on(context.getClass());
        final Object glContext = contextReflect.getFieldValue("glContext", context);
        Reflect.on("com.sun.prism.es2.GLContext").method("renderMeshView").invoke(glContext, meshView.getNativeHandle());

        textures.forEach(Texture::unlock);
    }

    private Shader createPixelShader(Graphics g) {
        return ShaderController.createShader(PrFilterContext.getInstance(g.getAssociatedScreen()), this.createPixelShaderDeclaration());
    }

    private Shader createVertexShader(Graphics g) {
        return ShaderController.createVertexShader(PrFilterContext.getInstance(g.getAssociatedScreen()), this.createVertexShaderDeclaration());
    }

    private ES2Shader createES2ShaderProgram(Graphics g) {
        return ShaderController.createES2ShaderProgram(PrFilterContext.getInstance(g.getAssociatedScreen()),
                this.createVertexShaderDeclaration(), this.createPixelShaderDeclaration(), this.getES2ShaderAttributes());
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

    private Texture getTexture(BaseShaderContext context, Image image, boolean useMipmap) {
        if (image == null) {
            return null;
        }
        final com.sun.prism.Image platformImage = (com.sun.prism.Image) Toolkit.getImageAccessor().getPlatformImage(image);
        return platformImage == null ? null : context.getResourceFactory().getCachedTexture(platformImage, Texture.WrapMode.REPEAT, useMipmap);
    }

    private long getD3DTextureHandle(Texture texture) {
        if (texture == null) {
            return 0;
        }
        return Reflect.on("com.sun.prism.d3d.D3DTexture").<Long>method("getNativeTextureObject").invoke(texture);
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

    protected abstract void updateShader(Shader vertexShader, Shader pixelShader, T material);

}
