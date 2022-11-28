# JFXShader
Allows custom effect shaders in JavaFX using OpenGL (GLSL) or DirectX (HLSL).


## Maven
How to include JFXShader in a Maven project:

Add the following repository to the pom.xml:
```xml
<repositories>
    <repository>
        <id>github</id>
        <name>GitHub Teragam Repository</name>
        <url>https://maven.pkg.github.com/Teragam/JFXShader</url>
    </repository>
</repositories>
```
Then, add the following dependency:
```xml
<dependency>
    <groupId>de.teragam</groupId>
    <artifactId>jfxshader</artifactId>
    <version>0.7.0</version>
</dependency>
```

## Usage
Samples can be found in the [samples](src/main/java/de/teragam/jfxshader/samples) package.

Custom effect shaders consist of two components: The [ShaderEffect](src/main/java/de/teragam/jfxshader/ShaderEffect.java) and the [ShaderEffectPeer](src/main/java/de/teragam/jfxshader/ShaderEffectPeer.java).
The ShaderEffect is the JavaFX effect that can be applied to a node via `setEffect`.
The ShaderEffectPeer provides the shader sources and updates the shader constants according to the ShaderEffect.

1. Create a new shader class that extends `de.teragam.jfxshader.ShaderEffect`. This class must be annotated with `@EffectDependencies(YourShaderEffectPeer.class)` to specify the corresponding ShaderEffectPeer.
2. Create a new shader peer class that extends `de.teragam.jfxshader.ShaderEffectPeer`. The peer class must be annotated with `@EffectPeer("UniqueEffectName")`.
3. Some examples on how to implement the ShaderEffect and ShaderEffectPeer can be found in the [samples](src/main/java/de/teragam/jfxshader/samples) package.

## Limitations
This library is bound to the restrictions of the JavaFX effects system. The following limitations apply:

- Only fragment/pixel shaders are supported. The vertex shaders of effects cannot be overwritten.
- The fragment/pixel shaders only support up to two samplers (input textures) and one output texture.
- It is not possible to use OpenGL shaders on Windows.
- For DirectX, only ps_3_0 shaders are supported.
- JavaFX requires DirectX shaders to be compiled with the `fxc` compiler. The following command can be used to compile HLSL shaders: `fxc.exe /nologo /T ps_3_0 /Fo .\shader.obj .\shader.hlsl`
- On some systems (i.e. VMs), it is necessary to enable the GPU usage by setting the `prism.forceGPU` system property to `true`.
- Due to dirty region optimizations, JavaFX may apply the shader effect only to the dirty region of the node. This can lead to artifacts. To disable dirty region optimizations, set the `prism.dirtyopts` system property to `false`.
