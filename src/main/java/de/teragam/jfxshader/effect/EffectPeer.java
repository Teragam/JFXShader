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
     * The unique name of the peer.
     */
    String value();

    /**
     * The pixel format of the target texture.
     * Defaults to {@link PixelFormat#INT_ARGB_PRE}.
     */
    PixelFormat targetFormat() default PixelFormat.INT_ARGB_PRE;

    /**
     * The wrap mode of the target texture.
     * Defaults to {@link Texture.WrapMode#CLAMP_TO_ZERO}.
     */
    Texture.WrapMode targetWrapMode() default Texture.WrapMode.CLAMP_TO_ZERO;

    /**
     * Enables mipmaps for the target texture.
     * Defaults to {@code false}.
     * <p>
     * <b>Note:</b> For Opengl, due to the use of deprecated functions, mipmaps are only generated if the
     * version of OpenGL is below 3.1.
     */
    boolean targetMipmaps() default false;

    /**
     * The image pool policy for the target texture.
     * Defaults to {@link ImagePoolPolicy#LENIENT}.
     *
     * @see ImagePoolPolicy
     */
    ImagePoolPolicy targetPoolPolicy() default ImagePoolPolicy.LENIENT;
}
