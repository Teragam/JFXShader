# JFXShader Changelog

All notable changes to this project will be documented here.

## v0.7.0 - 2022-11-28

### Added

- Added support to allow shaders to render to a texture with a custom pixel format. Previously, shaders could only
  render to a texture with the pixel format *INT_ARGB_PRE*.
- The output pixel format can now be specified in the EffectPeer interface along with other options, such as the wrap
  mode and whether mipmaps should be generated.
  Note: For OpenGL, JavaFX uses a deprecated method to generate mipmaps. Therefore, mipmaps are only generated if the
  version of OpenGL is below 3.1.
- Shaders now can use up to 4 texture samplers. Previously, shaders could only use 2 textures. Unfortunately, any
  textures above 2 do not have dedicated texture coordinates, due to limitations of JavaFX.
- Added support for shaders that use their previous output as an input texture. An example of this will be added in a
  future release.
- Added basic support for DPI scaling. JavaFX accomplishes this by applying a scale transform. However, this does not
  work for shaders that rely on positional information, such as the *Lighting* Effect. For now, the shaders can load the
  DPI scale as a parameter and compensate for it accordingly (see
  the [BlendShapes](src/main/java/de/teragam/jfxshader/samples/blendshapes/BlendShapesEffectPeer.java) EffectPeer for an
  example).

### Changed

- The Constructor of ShaderEffectPeer now takes a ShaderEffectPeerConfig instead of a FilterContext.

## v0.5.0 - 2022-10-25

### Added

- Initial release of the JFXShader library.
