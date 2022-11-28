package de.teragam.jfxshader.internal;

import com.sun.prism.PixelFormat;
import com.sun.prism.Texture;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.impl.Renderer;

import de.teragam.jfxshader.ImagePoolPolicy;

public final class ShaderEffectPeerConfig {

    private final FilterContext filterContext;
    private final Renderer renderer;
    private final String shaderName;
    private final PixelFormat targetFormat;
    private final Texture.WrapMode targetWrapMode;
    private final boolean targetMipmaps;
    private final ImagePoolPolicy targetPoolPolicy;

    public ShaderEffectPeerConfig(FilterContext filterContext, Renderer renderer, String shaderName, PixelFormat targetFormat, Texture.WrapMode targetWrapMode,
                                  boolean targetMipmaps, ImagePoolPolicy targetPoolPolicy) {
        this.filterContext = filterContext;
        this.renderer = renderer;
        this.shaderName = shaderName;
        this.targetFormat = targetFormat;
        this.targetWrapMode = targetWrapMode;
        this.targetMipmaps = targetMipmaps;
        this.targetPoolPolicy = targetPoolPolicy;
    }

    public FilterContext getFilterContext() {
        return this.filterContext;
    }

    public Renderer getRenderer() {
        return this.renderer;
    }

    public String getShaderName() {
        return this.shaderName;
    }

    public PixelFormat getTargetFormat() {
        return this.targetFormat;
    }

    public Texture.WrapMode getTargetWrapMode() {
        return this.targetWrapMode;
    }

    public boolean isTargetMipmaps() {
        return this.targetMipmaps;
    }

    public ImagePoolPolicy getTargetPoolPolicy() {
        return this.targetPoolPolicy;
    }
}
