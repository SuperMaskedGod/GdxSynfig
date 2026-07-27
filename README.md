# GdxSynfig

A [Synfig Studio](https://www.synfig.org/) animation runtime for [libGDX](https://libgdx.com/).

Load and play `.sif` Synfig animations natively in your libGDX project, the sifAnimation acts as if its a image and is seamless.

**Platforms:** Android · iOS · Windows · Linux · macOS · GWT/HTML

**Synfig version:** `1.4.5-2024.05.19`

```java
dependencies {
  api 'com.github.SuperMaskedGod:GdxSynfig:1.3'
}
```
 `Into your core build.gradle, Latest`

---

## Features

- 🔊 Sounds, including delayed sound triggers
- 🖼️ Images
- 🎚️ Opacity, Alpha overflow (you can brighten animation by pushing its alpha over 1)
- 📍 Origin
- 🔄 Transformations
- 👁️ Active layer toggling
- 🔀 Switch groups / layers
- 📦 Groups / layers
- 🔁 Time offset, Time loop Layer, Reversing animation, SeekTo, Stroboscope Time Layer, Free Time Layer
- 📈 Interpolations
- 📄 `.sif and .sifz` file parsing

### Not currently supported

- Any layer type or shape beyond the above (no region/outline/gradient rendering, etc.)

> ⚠️ **Performance tip:** `.sif` is XML, and parsing gets noticeably slower as animations grow larger, you can use sifz which is a gzip compressed version of sif but it is not compatable with gwt. It's strongly recommended to serialize parsed `.sif` data into a binary format or use sifz before shipping, this is **more than twice as fast to load** and produces a **smaller file size** than parsing raw XML at runtime.

---

## Getting Started

### Parsing an animation

```java
SifCanvas canvas = SifParser.parse(Gdx.files.internal("animations/hero_idle.sif"));
```

`SifParser.parse` accepts either a `FileHandle` or an `InputStream`, and returns a `SifCanvas`, the parsed representation of your Synfig animation.

### Loading assets

Before playback, load the animation's dependent assets (images, sounds):

```java
Array<String> loadedAssets = SifAnimation.loadAssets(canvas, imagePath, audioPath, assetManager);
```

This is a **static** call, you don't need an active `SifAnimation` instance to load assets for a canvas. It returns an `Array<String>` listing every asset it loaded, and assets can also be pulled from an existing `AssetManager` if you're managing loading yourself.

### Creating and playing an animation

```java
SifAnimation animation = new SifAnimation(canvas);
animation.play();
```

Animations **do not play automatically** on creation, you must call `play()` explicitly.

### Stopping / skipping to the end

```java
animation.seekTo(animation.getDuration());
```

There's no dedicated "skipToEnd or Finish" call, seek to the animation's duration to end it immediately.


---

## Swapping Textures at Runtime

You can hot-swap any texture used by a playing animation, useful for skins, damage states, or dynamic content:

```java
animation.swapTexture(index, newTexture);
```

---

## Quick Reference

```java
//Parse
SifCanvas canvas = SifParser.parse(Gdx.files.internal("anim.sif or sifz"));

//Load assets (static, no instance needed)
Array<String> assets = SifAnimation.loadAssets(canvas, imagePath, audioPath, assetManager);

//Create + play
SifAnimation animation = new SifAnimation(canvas);
animation.play();

//Sets size, animations are automatically compatible with Tables
animation.setSize(targetWidth, targetHeight);

//Skip to end
animation.seekTo(animation.getDuration());

//Swap a texture
animation.swapTexture(0, myTexture);
```
