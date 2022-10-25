package de.teragam.jfxshader;

import java.io.InputStream;
import java.util.Map;

public final class ShaderDeclaration {
    private final Map<String, Integer> samplers;
    private final Map<String, Integer> params;
    private final InputStream es2Source;
    private final InputStream d3dSource;

    public ShaderDeclaration(Map<String, Integer> samplers, Map<String, Integer> params,
                             InputStream es2Source, InputStream d3dSource) {
        this.samplers = samplers;
        this.params = params;
        this.es2Source = es2Source;
        this.d3dSource = d3dSource;
    }

    public Map<String, Integer> samplers() {
        return this.samplers;
    }

    public Map<String, Integer> params() {
        return this.params;
    }

    public InputStream es2Source() {
        return this.es2Source;
    }

    public InputStream d3dSource() {
        return this.d3dSource;
    }

}
