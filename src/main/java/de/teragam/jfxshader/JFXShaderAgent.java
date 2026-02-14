package de.teragam.jfxshader;

import java.lang.instrument.Instrumentation;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class JFXShaderAgent {

    private JFXShaderAgent() {}

    public static void premain(String agentArgs, Instrumentation inst) {
        JFXShaderAgent.processModule(inst, JFXShaderAgent.class.getModule());

        if (agentArgs != null) {
            for (final String moduleName : List.of(agentArgs.split(","))) {
                final Optional<Module> additionalModule = ModuleLayer.boot().findModule(moduleName);
                additionalModule.ifPresent(module -> JFXShaderAgent.processModule(inst, module));
            }
        }
    }

    private static void processModule(Instrumentation inst, Module targetModule) {
        final Optional<Module> graphicsModule = ModuleLayer.boot().findModule("javafx.graphics");
        graphicsModule.ifPresent(module -> {
            JFXShaderAgent.openPackages(inst, module, JFXShaderModule.getGraphicsPackages(), targetModule);
            JFXShaderAgent.openPackages(inst, module, JFXShaderModule.getOptionalGraphicsPackages(), targetModule);
        });
        final Optional<Module> baseModule = ModuleLayer.boot().findModule("javafx.base");
        baseModule.ifPresent(module -> JFXShaderAgent.openPackages(inst, module, JFXShaderModule.getBasePackages(), targetModule));
    }

    private static void openPackages(Instrumentation inst, Module module, Set<String> packages, Module targetModule) {
        final Set<Module> targetModules = Set.of(targetModule);
        final Map<String, Set<Module>> extraOpens = module.getPackages().stream()
                .filter(packages::contains)
                .collect(Collectors.toMap(
                        pkg -> pkg,
                        pkg -> targetModules
                ));
        inst.redefineModule(module, Set.of(), Map.of(), extraOpens, Set.of(), Map.of());
    }
}
