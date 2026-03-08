module de.teragam.jfxshader {
    requires javafx.base;
    requires javafx.graphics;
    requires java.instrument;

    exports de.teragam.jfxshader;
    exports de.teragam.jfxshader.effect;
    exports de.teragam.jfxshader.material;
    exports de.teragam.jfxshader.exception;
    exports de.teragam.jfxshader.renderstate;
    exports de.teragam.jfxshader.samples.effects;
    exports de.teragam.jfxshader.samples.materials;
    exports de.teragam.jfxshader.misc;
}
