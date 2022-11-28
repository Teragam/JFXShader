package de.teragam.jfxshader;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.sun.prism.PixelFormat;
import com.sun.prism.Texture;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EffectPeer {
    String value();

    PixelFormat targetFormat() default PixelFormat.INT_ARGB_PRE;

    Texture.WrapMode targetWrapMode() default Texture.WrapMode.CLAMP_TO_ZERO;

    boolean targetMipmaps() default false;

    ImagePoolPolicy targetPoolPolicy() default ImagePoolPolicy.LENIENT;
}
