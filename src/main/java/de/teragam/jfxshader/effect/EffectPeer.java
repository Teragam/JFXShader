package de.teragam.jfxshader.effect;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.sun.prism.PixelFormat;
import com.sun.prism.Texture;

import de.teragam.jfxshader.ImagePoolPolicy;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EffectPeer {

    /**
     * @return The unique name of the peer.
     */
    String value();

    /**
     * @return The pixel format of the target texture.
     * Defaults to {@link PixelFormat#INT_ARGB_PRE}.
     */
    PixelFormat targetFormat() default PixelFormat.INT_ARGB_PRE;

    /**
     * @return The wrap mode of the target texture.
     * Defaults to {@link Texture.WrapMode#CLAMP_TO_ZERO}.
     */
    Texture.WrapMode targetWrapMode() default Texture.WrapMode.CLAMP_TO_ZERO;

    /**
     * Enables mipmaps for the target texture.
     * Defaults to {@code false}.
     * <p>
     * <b>Note:</b> For Opengl, due to the use of deprecated functions, mipmaps are only generated if the
     * version of OpenGL is below 3.1.
     *
     * @return Whether mipmaps should be generated for the target texture.
     */
    boolean targetMipmaps() default false;

    /**
     * @return The image pool policy for the target texture.
     * Defaults to {@link ImagePoolPolicy#LENIENT}.
     * @see ImagePoolPolicy
     */
    ImagePoolPolicy targetPoolPolicy() default ImagePoolPolicy.LENIENT;

    /**
     * @return Whether the peer is a singleton and should be reused for all instances of the effect.
     * Defaults to {@code true}.
     */
    boolean singleton() default true;
}
