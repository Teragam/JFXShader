package de.teragam.jfxshader;

/**
 * Defines the texture selection policy for the image pool.
 */
public enum ImagePoolPolicy {

    /**
     * The texture pool only returns textures that are exactly the same size as the requested texture.
     * If no texture is available, a new texture is created.
     * This policy will result in larger VRAM usage because more textures are created.
     */
    EXACT,

    /**
     * The texture pool will return textures that are at least as large as the requested texture.
     * This may cause problems for shaders that rely on the texture size being consistent between multiple frames.
     * This policy will result in smaller VRAM usage because more textures are reused and fewer textures are created.
     * JavaFX uses this policy for its own textures.
     */
    LENIENT
}
