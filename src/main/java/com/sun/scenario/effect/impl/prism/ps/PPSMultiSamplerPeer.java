package com.sun.scenario.effect.impl.prism.ps;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.prism.PixelFormat;
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
import com.sun.scenario.effect.Filterable;
import com.sun.scenario.effect.ImageData;
import com.sun.scenario.effect.InternalCoreEffectBase;
import com.sun.scenario.effect.impl.EffectPeer;
import com.sun.scenario.effect.impl.PolicyBasedImagePool;
import com.sun.scenario.effect.impl.PoolFilterable;
import com.sun.scenario.effect.impl.prism.PrTexture;
import com.sun.scenario.effect.impl.state.RenderState;

import de.teragam.jfxshader.ImagePoolPolicy;
import de.teragam.jfxshader.ShaderDeclaration;
import de.teragam.jfxshader.ShaderEffect;
import de.teragam.jfxshader.internal.ShaderController;
import de.teragam.jfxshader.internal.ShaderEffectPeerConfig;
import de.teragam.jfxshader.internal.ShaderException;
import de.teragam.jfxshader.internal.TextureCreationException;

public abstract class PPSMultiSamplerPeer<T extends RenderState, S extends ShaderEffect> extends EffectPeer<T> {

    private static final EnumMap<PixelFormat, PolicyBasedImagePool> FORMAT_IMAGE_POOL_MAP = new EnumMap<>(PixelFormat.class);

    private Shader shader;
    private PPSDrawable drawable;
    private BaseTransform transform;

    private final ShaderEffectPeerConfig config;

    private final Field contextField;
    private final Method validateTextureOpMethod;
    private final Method checkStateMethod;
    private final int checkTextureOpMask;
    private final Method setTextureMethod;
    private final Method updatePerVertexColorMethod;
    private final Method validateMethod;

    protected PPSMultiSamplerPeer(ShaderEffectPeerConfig options) {
        super(options.getFilterContext(), options.getRenderer(), options.getShaderName());
        this.config = Objects.requireNonNull(options, "ShaderEffectPeerConfig must not be null");

        try {
            this.contextField = BaseGraphics.class.getDeclaredField("context");
            this.validateTextureOpMethod = BaseShaderContext.class.getDeclaredMethod("validateTextureOp", BaseShaderGraphics.class, BaseTransform.class,
                    Texture[].class, PixelFormat.class);
            this.checkStateMethod = BaseShaderContext.class.getDeclaredMethod("checkState", BaseShaderGraphics.class, int.class, BaseTransform.class,
                    Shader.class);
            this.setTextureMethod = BaseShaderContext.class.getDeclaredMethod("setTexture", int.class, Texture.class);
            this.updatePerVertexColorMethod = BaseShaderContext.class.getDeclaredMethod("updatePerVertexColor", Paint.class, float.class);
            this.validateMethod = PPSRenderer.class.getDeclaredMethod("validate");

            this.contextField.setAccessible(true);
            this.validateTextureOpMethod.setAccessible(true);
            this.checkStateMethod.setAccessible(true);
            this.setTextureMethod.setAccessible(true);
            this.updatePerVertexColorMethod.setAccessible(true);
            this.validateMethod.setAccessible(true);

            final Field checkTextureOpMaskField = BaseShaderContext.class.getDeclaredField("CHECK_TEXTURE_OP_MASK");
            checkTextureOpMaskField.setAccessible(true);
            this.checkTextureOpMask = (int) checkTextureOpMaskField.get(null);
        } catch (ReflectiveOperationException e) {
            throw new ShaderException("Failed to initialize PPSMultiSamplerPeer", e);
        }
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
        this.updateShader(shader, (S) ((InternalCoreEffectBase) super.getEffect()).getEffect());
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
        this.setDestBounds(this.getResultBounds(transform, outputClip, inputs));
        return this.filterImpl(inputs);
    }

    protected ImageData filterImpl(ImageData... inputs) {
        final Rectangle dstBounds = this.getDestBounds();
        final int dstw = dstBounds.width;
        final int dsth = dstBounds.height;
        final PPSRenderer renderer = this.getRenderer();
        final PPSDrawable dst = this.getCompatibleImage(dstw, dsth);
        this.drawable = dst;
        if (dst == null) {
            renderer.markLost();
            return new ImageData(this.getFilterContext(), null, dstBounds);
        }
        this.setDestNativeBounds(dst.getPhysicalWidth(), dst.getPhysicalHeight());

        final ArrayList<float[]> coords = new ArrayList<>();
        final ArrayList<Integer> coordLength = new ArrayList<>();
        final ArrayList<Texture> textures = new ArrayList<>();
        for (int i = 0; i < Math.min(inputs.length, 2); i++) {
            final Filterable srcFilterable = inputs[i].getUntransformedImage();
            final PrTexture<? extends Texture> srcTexture = (PrTexture<? extends Texture>) srcFilterable;
            if (srcTexture == null || srcTexture.getTextureObject() == null) {
                renderer.markLost();
                return new ImageData(this.getFilterContext(), dst, dstBounds);
            }
            final Rectangle srcBounds = inputs[i].getUntransformedBounds();
            final Texture prTexture = srcTexture.getTextureObject();
            final BaseTransform srcTransform = inputs[i].getTransform();
            this.setInputBounds(i, srcBounds);
            this.setInputTransform(i, srcTransform);
            this.setInputNativeBounds(i, srcTexture.getNativeBounds());

            final float[] srcRect = new float[8];
            final int srcCoords = this.getTextureCoordinates(0, srcRect, srcBounds.x, srcBounds.y, srcFilterable.getPhysicalWidth(),
                    srcFilterable.getPhysicalHeight(), dstBounds, srcTransform);

            final float txOff = ((float) prTexture.getContentX()) / prTexture.getPhysicalWidth();
            final float tyOff = ((float) prTexture.getContentY()) / prTexture.getPhysicalHeight();
            if (srcCoords < 8) {
                coords.add(new float[]{txOff + srcRect[0], tyOff + srcRect[1], txOff + srcRect[2], tyOff + srcRect[3], txOff + srcRect[2], tyOff + srcRect[1],
                        txOff + srcRect[0], tyOff + srcRect[3]});
                coordLength.add(4);
            } else {
                coords.add(new float[]{txOff + srcRect[0], tyOff + srcRect[1], txOff + srcRect[2], tyOff + srcRect[3], txOff + srcRect[4], tyOff + srcRect[5],
                        txOff + srcRect[6], tyOff + srcRect[7]});
                coordLength.add(8);
            }
            textures.add(srcTexture.getTextureObject());
        }
        final ShaderGraphics g = dst.createGraphics();
        if (g == null) {
            renderer.markLost();
            return new ImageData(this.getFilterContext(), dst, dstBounds);
        }
        if (this.shader == null) {
            this.shader = this.createShader();
        }
        if (this.shader == null || !this.shader.isValid()) {
            renderer.markLost();
            return new ImageData(this.getFilterContext(), dst, dstBounds);
        }
        g.setExternalShader(this.shader);
        this.updateShader(this.shader);
        this.drawTextures((float) dstw, (float) dsth, textures, coords, coordLength, (BaseShaderGraphics) g);
        g.setExternalShader(null);
        return new ImageData(this.getFilterContext(), dst, dstBounds);
    }

    private void drawTextures(float dx2, float dy2, List<Texture> textures, List<float[]> coords, List<Integer> coordLength, BaseShaderGraphics g) {
        final BaseTransform xform = g.getTransformNoClone();
        if (textures.isEmpty()) {
            return;
        }
        try {
            final BaseContext context = (BaseContext) this.contextField.get(g);
            this.validateTextureOpMethod.invoke(context, g, xform, textures.toArray(new Texture[0]), this.config.getTargetFormat());
            this.checkStateMethod.invoke(context, g, this.checkTextureOpMask, xform, this.shader);
            for (int i = 0; i < Math.min(textures.size(), 4); i++) {
                this.setTextureMethod.invoke(context, i, textures.get(i));
            }
            //TODO support more than 4 textures (OpenGL requires special handling for this)
            this.updatePerVertexColorMethod.invoke(context, null, g.getExtraAlpha());
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
        } catch (ReflectiveOperationException e) {
            throw new ShaderException("Failed to draw textures", e);
        }
    }

    private PPSDrawable getCompatibleImage(int width, int height) {
        if (this.config.getTargetFormat() == PixelFormat.INT_ARGB_PRE && !this.config.isTargetMipmaps() && this.config.getTargetPoolPolicy() == ImagePoolPolicy.LENIENT) {
            return this.getRenderer().getCompatibleImage(width, height);
        } else {
            final BiFunction<Integer, Integer, PoolFilterable> imageFactory = (w, h) -> {
                if (!this.validateRenderer()) {
                    return null;
                }
                try {
                    return PPSDrawable.create(ShaderController.createRTTexture(this.getFilterContext(), this.config.getTargetFormat(),
                            this.config.getTargetWrapMode(), w, h, this.config.isTargetMipmaps()));
                } catch (TextureCreationException e) {
                    return null;
                }
            };
            if (!PPSMultiSamplerPeer.FORMAT_IMAGE_POOL_MAP.containsKey(this.config.getTargetFormat())) {
                PPSMultiSamplerPeer.FORMAT_IMAGE_POOL_MAP.put(this.config.getTargetFormat(), new PolicyBasedImagePool());
            }
            final PolicyBasedImagePool pool = PPSMultiSamplerPeer.FORMAT_IMAGE_POOL_MAP.get(this.config.getTargetFormat());
            return (PPSDrawable) pool.checkOut(this.getRenderer(), width, height, this.config.isTargetMipmaps(), imageFactory,
                    this.config.getTargetPoolPolicy());
        }
    }

    private boolean validateRenderer() {
        try {
            return (boolean) this.validateMethod.invoke(this.getRenderer());
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

}
