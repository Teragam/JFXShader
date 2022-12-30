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
    <version>0.8.0</version>
</dependency>
```

## Usage
Examples can be found in the [wiki](https://github.com/Teragam/JFXShader/wiki/Examples) and in the [samples](src/main/java/de/teragam/jfxshader/samples) package.

A custom shader can be applied to any JavaFX node hierarchy by using `setEffect`:
```java
StackPane pane = new StackPane(getOtherContent());

// Instead of using existing JavaFX effects like SepiaTone or ColorAdjust,
// we can apply our own effect with custom shaders:
MyCustomShaderEffect effect = new MyCustomShaderEffect(); 
pane.setEffect(effect);
// Custom parameters can be set and used in the shader:
effect.setMyCustomParameter(2.0);
```
Instructions on how to implement a custom shader effect can be found in the [wiki](https://github.com/Teragam/JFXShader/wiki/Examples).

## Limitations
This library is bound to the restrictions of the JavaFX effects system. The following limitations apply:

- Only fragment/pixel shaders are supported. The vertex shaders of effects cannot be overwritten.
- The fragment/pixel shaders only support one active render target (output texture).
- It is not possible to use OpenGL shaders on Windows.
- For DirectX, only ps_3_0 shaders are supported.
- JavaFX requires DirectX shaders to be compiled with the `fxc` compiler. The following command can be used to compile HLSL shaders: `fxc.exe /nologo /T ps_3_0 /Fo .\shader.obj .\shader.hlsl`
- On some systems (i.e. VMs), it is necessary to enable the GPU usage by setting the `prism.forceGPU` system property to `true`.
- Due to dirty region optimizations, JavaFX may apply the shader effect only to the dirty region of the node. This can lead to artifacts. To disable dirty region optimizations, set the `prism.dirtyopts` system property to `false`.
