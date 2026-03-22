package de.teragam.jfxshader.effect;

import java.util.Optional;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.prism.PixelFormat;
import com.sun.prism.Texture;
import com.sun.scenario.effect.Effect;
import com.sun.scenario.effect.ImageData;
import com.sun.scenario.effect.impl.EffectPeer;
import com.sun.scenario.effect.impl.prism.PrDrawable;
import com.sun.scenario.effect.impl.prism.ps.PPSDrawable;
import com.sun.scenario.effect.impl.state.RenderState;

import de.teragam.jfxshader.ImagePoolPolicy;
import de.teragam.jfxshader.JFXShader;
import de.teragam.jfxshader.ShaderDeclaration;
import de.teragam.jfxshader.effect.internal.PPSMultiSamplerPeer;
import de.teragam.jfxshader.effect.internal.PeerAccessor;

/**
 * Facade for a JavaFX prism effect peer that renders a {@link ShaderEffect} using a custom shader.
 * <p>
 * Implementations only need to describe and update the shader while this class delegates render state,
 * texture handling and effect lifecycle to an internal peer.
 *
 * @param <T> concrete effect type rendered by this peer
 */
public abstract class ShaderEffectPeer<T extends ShaderEffect> {

    private final PPSMultiSamplerPeer<RenderState, T> internalPeer;
    private final PeerAccessor<RenderState> accessor;

    /**
     * Creates a peer bound to the provided configuration.
     *
     * @param config peer configuration used to initialize the underlying prism peer
     */
    protected ShaderEffectPeer(ShaderEffectPeerConfig config) {
        this.internalPeer = new PPSMultiSamplerPeer<>(config, this) {
            @Override
            protected ShaderDeclaration createShaderDeclaration() {
                return ShaderEffectPeer.this.createShaderDeclaration();
            }

            @Override
            protected void updateShader(JFXShader shader, T effect) {
                ShaderEffectPeer.this.updateShader(shader, effect);
            }
        };
        this.accessor = this.internalPeer.getPeerAccessor();
    }

    /**
     * Returns the internal JavaFX peer used for rendering.
     *
     * @return internal peer instance
     */
    public PPSMultiSamplerPeer<RenderState, T> getFXEffectPeer() {
        return this.internalPeer;
    }

    /**
     * Creates the declaration for the shader program used by this peer.
     *
     * @return shader declaration for the current effect
     */
    protected abstract ShaderDeclaration createShaderDeclaration();

    /**
     * Updates shader constants before drawing.
     *
     * @param shader shader instance to update
     * @param effect effect instance that provides parameter values
     */
    protected abstract void updateShader(JFXShader shader, T effect);

    // Delegation of public internalPeer methods

    /**
     * Disposes resources held by the underlying peer.
     */
    public void dispose() {
        this.internalPeer.dispose();
    }

    public void setPass(int pass) {
        this.internalPeer.setPass(pass);
    }

    public ShaderEffectPeer<T> getParentPeer() {
        return this.internalPeer.getParentPeer();
    }

    public BaseTransform getTransform() {
        return this.internalPeer.getTransform();
    }

    public PPSDrawable getDrawable() {
        return this.internalPeer.getDrawable();
    }

    public Rectangle getOutputClip() {
        return this.internalPeer.getOutputClip();
    }

    /**
     * Returns normalized source texture coordinates for the given input from the most recent filter pass.
     * <p>
     * The returned array has either 4 entries (rectilinear mapping) or 8 entries (non-rectilinear mapping).
     * Index sequences such as {@code [0,1,2,3,4,5,6,7]} describe array positions, where each corner is a
     * {@code (u,v)} pair.
     * <p>
     * For the 8-entry variant, this method returns coordinates in corner order:
     * {@code [top-left, top-right, bottom-left, bottom-right]}.
     *
     * @param inputIndex zero-based input index
     * @return copy of the coordinate array for the requested input
     * @throws IndexOutOfBoundsException if no coordinates are available for {@code inputIndex}
     */
    public float[] getTextureCoords(int inputIndex) {
        return this.internalPeer.getTextureCoords(inputIndex);
    }

    public boolean isOriginUpperLeft() {
        return this.internalPeer.isOriginUpperLeft();
    }

    public ImageData filter(Effect effect, RenderState renderState, BaseTransform transform, Rectangle outputClip,
                            ImageData... inputs) {
        return this.internalPeer.filter(effect, renderState, transform, outputClip, inputs);
    }

    public PPSDrawable getCompatibleImage(int width, int height, PixelFormat format, Texture.WrapMode wrapMode, boolean mipmaps, ImagePoolPolicy poolPolicy) {
        return this.internalPeer.getCompatibleImage(width, height, format, wrapMode, mipmaps, poolPolicy);
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
        return this.internalPeer.cloneTexture(srcTexture, width, height, dstFormat);
    }

    /**
     * Queues the invalidation and disposal of the shader.
     * It will be recreated with {@link #createShaderDeclaration()} when needed.
     */
    public void invalidateShader() {
        this.internalPeer.invalidateShader();
    }

    /**
     * Returns the transformed bounds of the input effect.
     * In contrast to {@link #getInputBounds(int)}, this method returns the full bounds regardless if dirty regions are rendered.
     *
     * @param index the index of the input
     * @return the effect bounds
     */
    public BaseBounds getInputEffectBounds(int index) {
        return this.internalPeer.getInputEffectBounds(index);
    }

    /**
     * @return the NGNode, where the effect is applied to, if available.
     */
    public Optional<NGNode> getNGNode() {
        return this.internalPeer.getNGNode();
    }

    public Rectangle getResultBounds(BaseTransform transform, Rectangle outputClip, ImageData... inputDatas) {
        return this.internalPeer.getResultBounds(transform, outputClip, inputDatas);
    }

    public int getPass() {
        return this.internalPeer.getPass();
    }

    public boolean isImageDataCompatible(ImageData id) {
        return this.internalPeer.isImageDataCompatible(id);
    }

    /**
     * Returns the unique peer name used by JavaFX effect infrastructure.
     *
     * @return unique peer name
     */
    public String getUniqueName() {
        return this.internalPeer.getUniqueName();
    }

    public int getTextureCoordinates(float[] coords, float srcX, float srcY, float srcNativeWidth, float srcNativeHeight, Rectangle dstBounds,
                                     BaseTransform transform) {
        return EffectPeer.getTextureCoordinates(coords, srcX, srcY, srcNativeWidth, srcNativeHeight, dstBounds, transform);
    }

    // Delegation of protected internalPeer methods via accessor

    protected RenderState getRenderState() {return this.accessor.getRenderState();}

    protected Rectangle getInputBounds(int inputIndex) {
        return this.accessor.getInputBounds(inputIndex);
    }

    protected BaseTransform getInputTransform(int inputIndex) {
        return this.accessor.getInputTransform(inputIndex);
    }

    protected Rectangle getInputNativeBounds(int inputIndex) {
        return this.accessor.getInputNativeBounds(inputIndex);
    }

    protected float[] getSourceRegion(int inputIndex) {
        return this.accessor.getSourceRegion(inputIndex);
    }

    protected Rectangle getDestBounds() {
        return this.accessor.getDestBounds();
    }

    protected Rectangle getDestNativeBounds() {
        return this.accessor.getDestNativeBounds();
    }

    protected Object getSamplerData(int i) {
        return this.accessor.getSamplerData(i);
    }
}
