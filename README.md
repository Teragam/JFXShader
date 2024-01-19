[![Maven Central](https://img.shields.io/maven-central/v/de.teragam/jfxshader.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22de.teragam%22%20AND%20a:%22jfxshader%22)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

# JFXShader

Allows custom effect shaders in JavaFX using OpenGL (GLSL) or DirectX (HLSL).

## Usage

Examples can be found in the [wiki](https://github.com/Teragam/JFXShader/wiki) and in the [samples](src/main/java/de/teragam/jfxshader/samples) package.

### 2D shader effects

A custom shader can be applied to any JavaFX node hierarchy by using `setEffect`:
```java
StackPane pane = new StackPane(getOtherContent());

// Instead of using existing JavaFX effects like SepiaTone or ColorAdjust,
// we can apply our own effect with custom shaders:
MyCustomShaderEffect effect = new MyCustomShaderEffect(); 
pane.setEffect(effect.getFXEffect());
// Custom parameters can be set and used in the shader:
effect.setMyCustomParameter(2.0);
```

Instructions on how to implement a custom shader effect can be found in
the [wiki](https://github.com/Teragam/JFXShader/wiki/Examples).

### 3D shader materials

A custom material can be applied to any Shape3D by using `setMaterial`:
```java
Sphere sphere = new Sphere(100);

// Instead of being limited to PhongMaterial,
// we can apply our own material with custom shaders:
MyCustomShaderMaterial material = new MyCustomShaderMaterial();
sphere.setMaterial(material.getFXMaterial());
// Custom parameters or textures can be set and used in the shader:
material.setMyCustomParameter(3.0);
material.setMyCustomTexture(new Image("myCustomTexture.png"));
```

## Maven

To include JFXShader in a Maven project, add the following dependency to the pom.xml:

```xml
<dependency>
    <groupId>de.teragam</groupId>
    <artifactId>jfxshader</artifactId>
    <version>1.3.1</version>
</dependency>
```

## Requirements

- Java 11 or higher
- JavaFX version 18 or higher

## Limitations

This library is bound to the restrictions of the JavaFX effects system. The following limitations apply:

- Only fragment/pixel shaders are supported. The vertex shaders of 2D effects cannot be overwritten. However, custom vertex shaders can be used for 3D materials.
- The fragment/pixel shaders only support one active render target (output texture).
- To use OpenGL shaders on Windows, a custom JavaFX build with OpenGL support is required. The official JavaFX builds only support DirectX shaders on Windows.
- For DirectX, only ps_3_0 shaders are supported.
- JavaFX requires DirectX shaders to be compiled with the `fxc` compiler. The following command can be used to compile
  HLSL shaders: `fxc.exe /nologo /T ps_3_0 /Fo .\shader.obj .\shader.hlsl`
- On some systems (i.e. VMs), it is necessary to enable the GPU usage by setting the `prism.forceGPU` system property
  to `true`.
- Due to dirty region optimizations, JavaFX may apply the shader effect only to the dirty region of the node. This can
  lead to artifacts. To disable dirty region optimizations, set the `prism.dirtyopts` system property to `false`.
