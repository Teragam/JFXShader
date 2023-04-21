package de.teragam.jfxshader.effect.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.prism.PixelFormat;
import com.sun.prism.RTTexture;
import com.sun.prism.Texture;
import com.sun.prism.impl.BaseContext;
import com.sun.prism.impl.BaseGraphics;
import com.sun.prism.impl.VertexBuffer;
import com.sun.prism.impl.ps.BaseShaderContext;
import com.sun.prism.impl.ps.BaseShaderGraphics;
import com.sun.prism.paint.Paint;
import com.sun.prism.ps.Shader;
import com.sun.prism.ps.ShaderGraphics;
import com.sun.scenario.effect.Effect;
import com.sun.scenario.effect.ImageData;
import com.sun.scenario.effect.impl.EffectPeer;
import com.sun.scenario.effect.impl.PoolFilterable;
import com.sun.scenario.effect.impl.prism.PrDrawable;
import com.sun.scenario.effect.impl.prism.ps.PPSDrawable;
import com.sun.scenario.effect.impl.prism.ps.PPSRenderer;
import com.sun.scenario.effect.impl.state.RenderState;

import de.teragam.jfxshader.ImagePoolPolicy;
import de.teragam.jfxshader.ShaderController;
import de.teragam.jfxshader.ShaderDeclaration;
import de.teragam.jfxshader.effect.ShaderEffect;
import de.teragam.jfxshader.exception.ShaderException;
import de.teragam.jfxshader.exception.TextureCreationException;
import de.teragam.jfxshader.util.Reflect;

public abstract class PPSMultiSamplerPeer<T extends RenderState, S extends ShaderEffect> extends EffectPeer<T> {

    private static final EnumMap<PixelFormat, PolicyBasedImagePool> FORMAT_IMAGE_POOL_MAP = new EnumMap<>(PixelFormat.class);

    private Shader shader;
    private PPSDrawable drawable;
    private BaseTransform transform;
    private Rectangle outputClip;

    private final ArrayList<float[]> textureCoords;
    private final ShaderEffectPeerConfig config;

    private final int checkTextureOpMask;

    protected PPSMultiSamplerPeer(ShaderEffectPeerConfig options) {
        super(options.getFilterContext(), options.getRenderer(), options.getShaderName());
        this.textureCoords = new ArrayList<>();
        this.config = Objects.requireNonNull(options, "ShaderEffectPeerConfig must not be null");
        this.checkTextureOpMask = Reflect.on(BaseShaderContext.class).getFieldValue("CHECK_TEXTURE_OP_MASK", null);
    }

    @Override
    public void dispose() {
        if (this.shader != null) {
            this.shader.dispose();
        }
    }

    private Shader createShader() {
        return ShaderController.createShader(super.getFilterContext(), this.createShaderDeclaration());
    }

    private void updateShader(Shader shader) {
        this.updateShader(shader, (S) ((InternalEffect) super.getEffect()).getEffect());
    }

    protected abstract ShaderDeclaration createShaderDeclaration();

    protected abstract void updateShader(Shader shader, S effect);

    protected BaseTransform getTransform() {
        return this.transform;
    }

    protected void setTransform(BaseTransform transform) {
        this.transform = transform;
    }

    protected PPSDrawable getDrawable() {
        return this.drawable;
    }

    protected void setOutputClip(Rectangle outputClip) {
        this.outputClip = outputClip;
    }

    protected Rectangle getOutputClip() {
        return this.outputClip;
    }

    protected float[] getTextureCoords(int inputIndex) {
        return Arrays.copyOf(this.textureCoords.get(inputIndex), this.textureCoords.get(inputIndex).length);
    }

    @Override
    protected final PPSRenderer getRenderer() {
        return (PPSRenderer) super.getRenderer();
    }

    @Override
    public final ImageData filter(final Effect effect, final T renderState, final BaseTransform transform, final Rectangle outputClip,
                                  final ImageData... inputs) {
        this.setEffect(effect);
        this.setRenderState(renderState);
        this.setTransform(transform);
        this.setOutputClip(outputClip);
        this.setDestBounds(this.getResultBounds(transform, outputClip, inputs));
        return this.filterImpl(inputs);
    }

    protected ImageData filterImpl(ImageData... inputs) {
        final Rectangle dstBounds = this.getDestBounds();
        final int dstw = dstBounds.width;
        final int dsth = dstBounds.height;
        final PPSRenderer renderer = this.getRenderer();
        final PPSDrawable dst = this.getCompatibleImage(dstw, dsth, this.config.getTargetFormat(), this.config.getTargetWrapMode(),
                this.config.isTargetMipmaps(), this.config.getTargetPoolPolicy());
        this.drawable = dst;
        if (dst == null) {
            this.markLost(renderer);
            return new ImageData(this.getFilterContext(), null, dstBounds);
        }
        this.setDestNativeBounds(dst.getPhysicalWidth(), dst.getPhysicalHeight());

        final ArrayList<float[]> coords = new ArrayList<>();
        this.textureCoords.clear();
        final ArrayList<Integer> coordLength = new ArrayList<>();
        final ArrayList<Texture> textures = new ArrayList<>();
        for (int i = 0; i < Math.min(inputs.length, 2); i++) {
            final PrDrawable srcTexture = (PrDrawable) inputs[i].getUntransformedImage();
            if (srcTexture == null || srcTexture.getTextureObject() == null) {
                this.markLost(renderer);
                return new ImageData(this.getFilterContext(), dst, dstBounds);
            }
            final Rectangle srcBounds = inputs[i].getUntransformedBounds();
            final Texture prTexture = srcTexture.getTextureObject();
            final BaseTransform srcTransform = inputs[i].getTransform();
            this.setInputBounds(i, srcBounds);
            this.setInputTransform(i, srcTransform);
            this.setInputNativeBounds(i, srcTexture.getNativeBounds());

            final float[] srcRect = new float[8];
            final int srcCoords = this.getTextureCoordinates(0, srcRect, srcBounds.x, srcBounds.y, srcTexture.getPhysicalWidth(),
                    srcTexture.getPhysicalHeight(), dstBounds, srcTransform);

            final float txOff = ((float) prTexture.getContentX()) / prTexture.getPhysicalWidth();
            final float tyOff = ((float) prTexture.getContentY()) / prTexture.getPhysicalHeight();
            if (srcCoords < 8) {
                coords.add(new float[]{txOff + srcRect[0], tyOff + srcRect[1], txOff + srcRect[2], tyOff + srcRect[3], txOff + srcRect[2], tyOff + srcRect[1],
                        txOff + srcRect[0], tyOff + srcRect[3]});
                coordLength.add(4);
                this.textureCoords.add(new float[]{txOff + srcRect[0], tyOff + srcRect[1], txOff + srcRect[2], tyOff + srcRect[3]});
            } else {
                coords.add(new float[]{txOff + srcRect[0], tyOff + srcRect[1], txOff + srcRect[2], tyOff + srcRect[3], txOff + srcRect[4], tyOff + srcRect[5],
                        txOff + srcRect[6], tyOff + srcRect[7]});
                coordLength.add(8);
                this.textureCoords.add(new float[]{txOff + srcRect[0], tyOff + srcRect[1], txOff + srcRect[4], tyOff + srcRect[5],
                        txOff + srcRect[6], tyOff + srcRect[7], txOff + srcRect[2], tyOff + srcRect[3]});
            }
            textures.add(srcTexture.getTextureObject());
        }
        for (int i = 2; i < Math.min(inputs.length, ShaderController.MAX_BOUND_TEXTURES); i++) {
            final PrDrawable srcTexture = (PrDrawable) inputs[i].getUntransformedImage();
            if (srcTexture == null || srcTexture.getTextureObject() == null) {
                this.markLost(renderer);
                return new ImageData(this.getFilterContext(), dst, dstBounds);
            }
            textures.add(srcTexture.getTextureObject());
        }
        final ShaderGraphics g = dst.createGraphics();
        if (g == null) {
            this.markLost(renderer);
            return new ImageData(this.getFilterContext(), dst, dstBounds);
        }
        if (this.shader == null) {
            this.shader = this.createShader();
        }
        if (this.shader == null || !this.shader.isValid()) {
            this.markLost(renderer);
            return new ImageData(this.getFilterContext(), dst, dstBounds);
        }
        g.setExternalShader(this.shader);
        this.updateShader(this.shader);
        this.drawTextures((float) dstw, (float) dsth, textures, coords, coordLength, (BaseShaderGraphics) g);
        g.setExternalShader(null);
        return new ImageData(this.getFilterContext(), dst, dstBounds);
    }

    private void markLost(PPSRenderer renderer) {
        Reflect.on(PPSRenderer.class).method("markLost").invoke(renderer);
    }

    private void drawTextures(float dx2, float dy2, List<Texture> textures, List<float[]> coords, List<Integer> coordLength, BaseShaderGraphics g) {
        final BaseTransform xform = g.getTransformNoClone();
        if (textures.isEmpty()) {
            return;
        }
        final BaseContext context = Reflect.on(BaseGraphics.class).getFieldValue("context", g);
        if (context.isDisposed() || !(context instanceof BaseShaderContext)) {
            return;
        }
        ShaderController.ensureTextureCapacity(this.getFilterContext(), (BaseShaderContext) context);
        Reflect.on(BaseShaderContext.class).method("checkState", BaseShaderGraphics.class, int.class, BaseTransform.class,
                Shader.class).invoke(context, g, this.checkTextureOpMask, xform, this.shader);
        for (int i = 0; i < Math.min(textures.size(), ShaderController.MAX_BOUND_TEXTURES); i++) {
            Reflect.on(BaseShaderContext.class).method("setTexture", int.class, Texture.class).invoke(context, i, textures.get(i));
        }
        Reflect.on(BaseShaderContext.class).method("updatePerVertexColor", Paint.class, float.class).invoke(context, null, g.getExtraAlpha());
        final VertexBuffer vb = context.getVertexBuffer();
        switch (coords.size()) {
            case 0:
                return;
            case 1:
                final float[] c = coords.get(0);
                if (coordLength.get(0) < 8) {
                    vb.addQuad(0, 0, dx2, dy2, c[0], c[1], c[2], c[3]);
                } else {
                    vb.addMappedQuad(0, 0, dx2, dy2, c[0], c[1], c[4], c[5], c[6], c[7], c[2], c[3]);
                }
                break;
            default:
                final float[] c1 = coords.get(0);
                final float[] c2 = coords.get(1);
                if (coordLength.get(0) < 8 && coordLength.get(1) < 8) {
                    vb.addQuad(0, 0, dx2, dy2, c1[0], c1[1], c1[2], c1[3], c2[0], c2[1], c2[2], c2[3]);
                } else {
                    vb.addMappedQuad(0, 0, dx2, dy2, c1[0], c1[1], c1[4], c1[5], c1[6], c1[7], c1[2], c1[3], c2[0], c2[1], c2[4], c2[5], c2[6], c2[7],
                            c2[2], c2[3]);
                }
        }
    }

    public PPSDrawable getCompatibleImage(int width, int height, PixelFormat format, Texture.WrapMode wrapMode, boolean mipmaps,
                                          ImagePoolPolicy poolPolicy) {
        if (format == PixelFormat.INT_ARGB_PRE && !mipmaps && poolPolicy == ImagePoolPolicy.LENIENT) {
            return this.getRenderer().getCompatibleImage(width, height);
        } else {
            final BiFunction<Integer, Integer, PoolFilterable> imageFactory = (w, h) -> {
                if (!this.validateRenderer()) {
                    return null;
                }
                try {
                    return (PPSDrawable) Reflect.on(PPSDrawable.class).method("create", RTTexture.class)
                            .invoke(null, ShaderController.createRTTexture(this.getFilterContext(), format, wrapMode, w, h, mipmaps));
                } catch (TextureCreationException e) {
                    return null;
                }
            };
            PPSMultiSamplerPeer.FORMAT_IMAGE_POOL_MAP.computeIfAbsent(format, f -> new PolicyBasedImagePool());
            final PolicyBasedImagePool pool = PPSMultiSamplerPeer.FORMAT_IMAGE_POOL_MAP.get(format);
            return (PPSDrawable) pool.checkOut(this.getRenderer(), width, height, mipmaps, imageFactory, poolPolicy);
        }
    }

    /**
     * Clones the given texture and returns the clone with the specified dimensions and pixel format.
     * <p>
     * In JavaFX, the actual textures that are used for rendering are often larger than the content they contain due to the use of an internal texture pool.
     * To render shader effects, the texture coordinates for the input textures are fitted to the content size, but the texture size is not.
     * This may cause problems for shaders that modify the texture coordinates and rely on the texture coordinates being in the range [0, 1] for the whole
     * input texture.
     * By cloning the texture and setting the texture size to the content size, the texture coordinates will be in the range [0, 1].
     * <p>
     * Alternatively, the calculated texture coordinates can be loaded into the shader as a uniform variable.
     *
     * @see ImagePoolPolicy
     */
    public PrDrawable cloneTexture(PrDrawable srcTexture, int width, int height, PixelFormat dstFormat) {
        final PrDrawable fittedTexture = this.getCompatibleImage(width, height, dstFormat,
                srcTexture.getTextureObject().getWrapMode(),
                srcTexture.getTextureObject().getUseMipmap(), ImagePoolPolicy.EXACT);
        if (fittedTexture == null || fittedTexture.getTextureObject() == null || !this.validateRenderer()) {
            return null;
        }
        fittedTexture.createGraphics().blit(srcTexture.getTextureObject(), null, 0, 0, width, height, 0, 0, width, height);
        return fittedTexture;
    }

    private boolean validateRenderer() {
        try {
            return (boolean) Reflect.on(PPSRenderer.class).method("validate").invoke(this.getRenderer());
        } catch (ShaderException ignored) {
            return false;
        }
    }

}
