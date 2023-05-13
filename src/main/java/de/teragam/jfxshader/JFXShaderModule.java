package de.teragam.jfxshader;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import de.teragam.jfxshader.exception.ShaderException;
import de.teragam.jfxshader.util.Reflect;

public final class JFXShaderModule {

    private static AtomicBoolean initialized = new AtomicBoolean();

    private JFXShaderModule() {}

    public static void setup() {
        if (JFXShaderModule.initialized.getAndSet(true)) {
            return;
        }
        final Module module = JFXShaderModule.class.getModule();
        JFXShaderModule.getGraphicsPackages().forEach(pkg -> Reflect.addOpens(pkg, "javafx.graphics", module));
        JFXShaderModule.getOptionalGraphicsPackages().forEach(pkg -> {
            try {
                Reflect.addOpens(pkg, "javafx.graphics", module);
            } catch (ShaderException e) {
                // ignore
            }
        });
        JFXShaderModule.getBasePackages().forEach(pkg -> Reflect.addOpens(pkg, "javafx.base", module));
    }

    private static List<String> getGraphicsPackages() {
        return List.of("com.sun.javafx.effect", "com.sun.javafx.geom", "com.sun.javafx.geom.transform", "com.sun.javafx.scene",
                "com.sun.scenario.effect.impl.state", "com.sun.scenario.effect", "com.sun.prism", "com.sun.prism.impl", "com.sun.scenario.effect.impl.prism",
                "com.sun.prism.ps", "com.sun.glass.ui", "com.sun.glass.utils", "com.sun.javafx.scene.shape", "com.sun.javafx.sg.prism", "com.sun.javafx.util",
                "com.sun.prism.impl.ps", "com.sun.scenario.effect.impl", "com.sun.scenario.effect.impl.prism.ps", "com.sun.javafx.scene.paint",
                "com.sun.javafx.tk", "com.sun.javafx.beans.event", "javafx.scene.effect", "javafx.scene.paint");
    }

    private static List<String> getOptionalGraphicsPackages() {
        return List.of("com.sun.prism.es2", "com.sun.prism.d3d");
    }

    private static List<String> getBasePackages() {
        return List.of("com.sun.javafx");
    }

}
