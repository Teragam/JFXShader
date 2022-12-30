package de.teragam.jfxshader;

/**
 * Defines the texture selection policy for the image pool.
 */
public enum ImagePoolPolicy {

    /**
     * The texture pool only returns textures that are exactly the same size as the requested texture.
     * If no texture is available, a new texture is created.
     * This policy will result in larger VRAM usage because more textures are created.
     * <p>
     * This policy cannot be applied if one of the following conditions apply:
     * <ul>
     *     <li>The rendering pipeline is OpenGL ES 2.0 (OpenGL 2.0 is still supported)</li>
     *     <li>The setting <i>prism.noclamptozero</i> is set to true</li>
     *     <li>The setting <i>prism.forcepowerof2</i> is set to true</li>
     *     <li>Both of <i>GL_ARB_texture_non_power_of_two</i> and <i>GL_OES_texture_npot</i> are not supported.</li>
     * </ul>
     */
    EXACT(1, false),

    /**
     * The texture pool will return textures that are at least as large as the requested texture.
     * In contrast to {@link #LENIENT}, this policy assures that the texture dimensions will not fluctuate between frames, as the pool will only return
     * textures with dimensions that exactly match a quantized version of the requested dimensions.
     */
    QUANTIZED(32, false),

    /**
     * The texture pool will return textures that are at least as large as the requested texture.
     * The texture dimensions may fluctuate between frames because the pool will select textures that loosely match the requested dimensions.
     * This may cause problems for shaders that rely on the texture size being consistent between multiple frames.
     * This policy will result in smaller VRAM usage because more textures are reused and fewer textures are created.
     * JavaFX uses this policy for its own textures.
     */
    LENIENT(32, true);

    private final int quantization;
    private final boolean approximateMatch;

    ImagePoolPolicy(int quantization, boolean approximateMatch) {
        this.quantization = quantization;
        this.approximateMatch = approximateMatch;
    }

    public int getQuantization() {
        return this.quantization;
    }

    public boolean isApproximateMatch() {
        return this.approximateMatch;
    }
}
