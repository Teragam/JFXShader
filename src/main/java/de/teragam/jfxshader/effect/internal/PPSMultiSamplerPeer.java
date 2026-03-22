package de.teragam.jfxshader.effect.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.sg.prism.NodeEffectInput;
import com.sun.prism.PixelFormat;
import com.sun.prism.RTTexture;
import com.sun.prism.Texture;
import com.sun.prism.impl.BaseContext;
import com.sun.prism.impl.BaseGraphics;
import com.sun.prism.impl.VertexBuffer;
import com.sun.prism.impl.ps.BaseShaderContext;
import com.sun.prism.impl.ps.BaseShaderGraphics;
import com.sun.prism.ps.Shader;
import com.sun.prism.ps.ShaderGraphics;
import com.sun.scenario.effect.Effect;
import com.sun.scenario.effect.Filterable;
import com.sun.scenario.effect.ImageData;
import com.sun.scenario.effect.impl.EffectPeer;
import com.sun.scenario.effect.impl.PoolFilterable;
import com.sun.scenario.effect.impl.prism.PrDrawable;
import com.sun.scenario.effect.impl.prism.PrTexture;
import com.sun.scenario.effect.impl.prism.ps.PPSDrawable;
import com.sun.scenario.effect.impl.prism.ps.PPSRenderer;
import com.sun.scenario.effect.impl.state.RenderState;

import de.teragam.jfxshader.ImagePoolPolicy;
import de.teragam.jfxshader.JFXShader;
import de.teragam.jfxshader.ShaderController;
import de.teragam.jfxshader.ShaderDeclaration;
import de.teragam.jfxshader.effect.InternalEffect;
import de.teragam.jfxshader.effect.ShaderEffect;
import de.teragam.jfxshader.effect.ShaderEffectPeer;
import de.teragam.jfxshader.effect.ShaderEffectPeerConfig;
import de.teragam.jfxshader.exception.ShaderException;
import de.teragam.jfxshader.exception.TextureCreationException;
import de.teragam.jfxshader.util.Reflect;

public abstract class PPSMultiSamplerPeer<T extends RenderState, S extends ShaderEffect> extends EffectPeer<T> {

    private static final EnumMap<PixelFormat, PolicyBasedImagePool> FORMAT_IMAGE_POOL_MAP = new EnumMap<>(PixelFormat.class);

    private final PeerAccessor<T> peerAccessor;
    private final ShaderEffectPeer<S> parentPeer;
    private JFXShader shader;
    private PPSDrawable drawable;
    private BaseTransform transform;
    private Rectangle outputClip;
    private boolean invalidateShader;

    private final ArrayList<float[]> textureCoords;
    private final ShaderEffectPeerConfig config;

    private final int checkTextureOpMask;

    /**
     * Creates a new multi-sampler peer instance.
     *
     * @param options    configuration for this peer, including the filter context, renderer and shader name.
     * @param parentPeer the {@link ShaderEffectPeer} that owns or uses this multi-sampler peer. The parent peer
     *                   represents the higher-level effect peer associated with the {@link ShaderEffect} and is
     *                   used by this instance to access shared configuration and state. The lifecycle of the
     *                   parent peer is managed externally; this class only keeps a reference to it and does not
     *                   dispose or otherwise manage the {@code parentPeer} instance.
     */
    protected PPSMultiSamplerPeer(ShaderEffectPeerConfig options, ShaderEffectPeer<S> parentPeer) {
        super(options.getFilterContext(), options.getRenderer(), options.getShaderName());
        this.peerAccessor = Reflect.createProxy(this, EffectPeer.class, PeerAccessor.class);
        this.parentPeer = parentPeer;
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

    public ShaderEffectPeer<S> getParentPeer() {
        return this.parentPeer;
    }

    private JFXShader createShader() {
        return ShaderController.createShader(super.getFilterContext(), this.createShaderDeclaration(), super.getUniqueName());
    }

    protected abstract ShaderDeclaration createShaderDeclaration();

    protected abstract void updateShader(JFXShader shader, S effect);

    public BaseTransform getTransform() {
        return this.transform;
    }

    protected void setTransform(BaseTransform transform) {
        this.transform = transform;
    }

    public PPSDrawable getDrawable() {
        return this.drawable;
    }

    protected void setOutputClip(Rectangle outputClip) {
        this.outputClip = outputClip;
    }

    public Rectangle getOutputClip() {
        return this.outputClip;
    }

    /**
     * Returns normalized source texture coordinates for one input as computed during the most recent
     * {@link #filter} pass.
     * <p>
     * Internally, {@link #filterImpl} delegates to {@link EffectPeer#getTextureCoordinates}
     * and then applies {@code contentX/contentY} offsets so coordinates reference the content region inside
     * pooled textures (not the full physical backing texture).
     * <p>
     * The returned array contains either 4 or 8 floats.
     * Index sequences like {@code [0,1,2,3,4,5,6,7]} refer to array positions, not literal coordinate values.
     * Each corner uses a pair of indices ({@code u,v}).
     * <p>
     * Destination corners use JavaFX's naming:
     * {@code (dx1,dy1)=top-left}, {@code (dx2,dy1)=top-right},
     * {@code (dx1,dy2)=bottom-left}, {@code (dx2,dy2)=bottom-right}.
     * <p>
     * Mapping details:
     * <ul>
     *     <li>
     *         4 floats for rectilinear mapping: {@code [u0, v0, u1, v1]}.
     *         Corner mapping is:
     *         <pre>
     *             dx1,dy1 -> ret[0], ret[1]
     *             dx2,dy1 -> ret[2], ret[1]
     *             dx1,dy2 -> ret[0], ret[3]
     *             dx2,dy2 -> ret[2], ret[3]
     *         </pre>
     *     </li>
     *     <li>
     *         8 floats for non-rectilinear mapping.
     *         Raw output from {@link EffectPeer#getTextureCoordinates} is stored in {@code srcRect[0..7]} and maps corners as:
     *         <pre>
     *             dx1,dy1 -> srcRect[0], srcRect[1]
     *             dx2,dy1 -> srcRect[4], srcRect[5]
     *             dx1,dy2 -> srcRect[6], srcRect[7]
     *             dx2,dy2 -> srcRect[2], srcRect[3]
     *         </pre>
     *         This method returns the 8 values in corner order
     *         {@code [top-left, top-right, bottom-left, bottom-right]} by reordering
     *         raw indices {@code [0,1,2,3,4,5,6,7]} into returned indices {@code [0,1,4,5,6,7,2,3]}.
     *     </li>
     * </ul>
     * Coordinates are stored only for inputs processed in the current pass; this implementation populates
     * entries for at most the first two inputs.
     * <p>
     * A defensive copy is returned, so caller modifications do not affect internal peer state.
     *
     * @param inputIndex zero-based input index
     * @return copy of the normalized coordinate array for the requested input
     * @throws IndexOutOfBoundsException if coordinates are unavailable for {@code inputIndex}, for example
     *                                   before the first successful filter pass or when the index exceeds
     *                                   the number of processed inputs
     */
    public float[] getTextureCoords(int inputIndex) {
        return Arrays.copyOf(this.textureCoords.get(inputIndex), this.textureCoords.get(inputIndex).length);
    }

    @Override
    public boolean isOriginUpperLeft() {
        return super.isOriginUpperLeft();
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
            final Filterable srcFilterable = inputs[i].getUntransformedImage();
            final PrTexture<?> srcTexture = (PrTexture<?>) inputs[i].getUntransformedImage();
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
            final int srcCoords = this.getTextureCoordinates(0, srcRect, srcBounds.x, srcBounds.y, srcFilterable.getPhysicalWidth(),
                    srcFilterable.getPhysicalHeight(), dstBounds, srcTransform);

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

        if (this.invalidateShader && this.shader != null) {
            this.shader.dispose();
            this.shader = null;
            this.invalidateShader = false;
        }
        if (this.shader == null) {
            this.shader = this.createShader();
        }
        if (this.shader == null || !this.shader.isValid()) {
            this.markLost(renderer);
            return new ImageData(this.getFilterContext(), dst, dstBounds);
        }
        g.setExternalShader((Shader) this.shader.getObject());
        try {
            this.updateShader(this.shader, (S) ((InternalEffect) super.getEffect()).getEffect());
            this.drawTextures((float) dstw, (float) dsth, textures, coords, coordLength, (BaseShaderGraphics) g);
        } finally {
            g.setExternalShader(null);
        }

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
        Reflect.on(BaseShaderContext.class).method("checkState").invoke(context, g, this.checkTextureOpMask, xform, this.shader.getObject());
        for (int i = 0; i < Math.min(textures.size(), ShaderController.MAX_BOUND_TEXTURES); i++) {
            Reflect.on(BaseShaderContext.class).method("setTexture").invoke(context, i, textures.get(i));
        }
        Reflect.on(BaseShaderContext.class).method("updatePerVertexColor").invoke(context, null, g.getExtraAlpha());
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

    /**
     * Queues the invalidation and disposal of the shader.
     * It will be recreated with {@link PPSMultiSamplerPeer#createShaderDeclaration()} when needed.
     */
    public void invalidateShader() {
        if (this.shader != null) {
            this.invalidateShader = true;
        }
    }

    /**
     * @return the NGNode, where the effect is applied to, if available.
     */
    public Optional<NGNode> getNGNode() {
        return Optional.ofNullable(((InternalEffect) super.getEffect()).getDefaultInput()).filter(NodeEffectInput.class::isInstance)
                .map(NodeEffectInput.class::cast).map(NodeEffectInput::getNode);
    }

    /**
     * Returns the transformed bounds of the input effect.
     * In contrast to {@link #getInputBounds(int)}, this method returns the full bounds regardless if dirty regions are rendered.
     *
     * @param index the index of the input
     * @return the effect bounds
     */
    public BaseBounds getInputEffectBounds(int index) {
        final InternalEffect effect = (InternalEffect) super.getEffect();
        return effect.getDefaultedInput(index, effect.getDefaultInput()).getBounds(this.getTransform(), effect.getDefaultInput());
    }

    private boolean validateRenderer() {
        try {
            return (boolean) Reflect.on(PPSRenderer.class).method("validate").invoke(this.getRenderer());
        } catch (ShaderException ignored) {
            return false;
        }
    }

    public PeerAccessor<T> getPeerAccessor() {
        return this.peerAccessor;
    }

}
