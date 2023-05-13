# JFXShader Changelog

All notable changes to this project will be documented here.

## v1.1.0 - 2023-05-13

### Added

- Added Java module support.
    - The library can now be used with modular and non-modular JavaFX applications.
    - For modular JavaFX applications, `JFXShaderModule.setup()` must be called before using any other of the library's
      classes.
- Added more sample effects:
    - [Pixelate](src/main/java/de/teragam/jfxshader/samples/effects/pixelate/Pixelate.java)
    - [ZoomRadialBlur](src/main/java/de/teragam/jfxshader/samples/effects/zoomradialblur/ZoomRadialBlur.java)

### Changed

- To support Java modules, ShaderEffects now have `getFXEffect()` to return the compatible JavaFX `Effect` that should
  be used. (Analogue to `getFXMaterial()` for ShaderMaterials.)

## v1.0.0 - 2023-04-30

### Added

- Added support for custom 3D shader materials.
    - Applicable to any `Shape3D` by using `setMaterial`.
    - Custom parameters and textures can be set and used in the shader.
    - Pixel shaders as well as vertex shaders are supported.
- Added [FresnelMaterial](src/main/java/de/teragam/jfxshader/samples/materials/fresnel/FresnelMaterial.java) as an example for a custom 3D shader material.

### Changed

- Restructured classes and packages.
- Reduced reliability on internal JavaFX package-private classes for future modularity support.

## v0.8.0 - 2022-12-30

### Added

- Shaders can now use up to 16 bound textures simultaneously.
- Some refactoring and QoL improvements to ease the development of
  custom [IEffectRenderer](src/main/java/de/teragam/jfxshader/IEffectRenderer.java).
- The generated texture coordinates for the shader input textures can now be accessed in
  the [ShaderEffectPeer](src/main/java/de/teragam/jfxshader/ShaderEffectPeer.java). This allows the shaders to
  compensate for the varying texture coordinates caused by JavaFX's internal texture pooling.
- The `ImagePoolPolicy.QUANTIZED` option has been added to allow better VRAM usage while ensuring that the target
  texture dimensions do not fluctuate between frames.
- Added option to invert the mask in
  the [BlendShapes](src/main/java/de/teragam/jfxshader/samples/effects/blendshapes/BlendShapesEffectPeer.java) example.

### Fixed

- Fixed an issue that did not allow the use of more than 2 textures in a shader.

## v0.7.0 - 2022-11-28

### Added

- Added support to allow shaders to render to a texture with a custom pixel format. Previously, shaders could only
  render to a texture with the pixel format *INT_ARGB_PRE*.
- The output pixel format can now be specified in the EffectPeer interface along with other options, such as the wrap
  mode and whether mipmaps should be generated.
  Note: For OpenGL, JavaFX uses a deprecated method to generate mipmaps. Therefore, mipmaps are only generated if the
  version of OpenGL is below 3.1.
- Shaders can now use up to 4 texture samplers. Previously, shaders could only use 2 textures. Unfortunately, any
  textures above 2 do not have dedicated texture coordinates, due to limitations of JavaFX.
- Added support for shaders that use their previous output as an input texture. An example of this will be added in a
  future release.
- Added basic support for DPI scaling. JavaFX accomplishes this by applying a scale transform. However, this does not
  work for shaders that rely on positional information, such as the *Lighting* Effect. For now, the shaders can load the
  DPI scale as a parameter and compensate for it accordingly (see
  the [BlendShapes](src/main/java/de/teragam/jfxshader/samples/effects/blendshapes/BlendShapesEffectPeer.java)
  EffectPeer for an
  example).

### Changed

- The Constructor of ShaderEffectPeer now takes a ShaderEffectPeerConfig instead of a FilterContext.

## v0.5.0 - 2022-10-25

### Added

- Initial release of the JFXShader library.
