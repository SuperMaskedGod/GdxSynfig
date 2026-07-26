# GdxSynfig

A [Synfig Studio](https://www.synfig.org/) animation runtime for [libGDX](https://libgdx.com/).

Load and play `.sif` Synfig animations natively in your libGDX project, the sifAnimation acts as if its a image and is seamless.

**Platforms:** Android · iOS · Windows · Linux · macOS · GWT/HTML

**Synfig version:** `1.4.5-2024.05.19`

**dependencies {api 'com.github.SuperMaskedGod:GdxSynfig:1.1.0'}** `Into your core build.gradle`

---

## Features

- 🔊 Sounds, including delayed sound triggers
- 🖼️ Images
- 🎚️ Opacity
- 📍 Origin
- 🔄 Transformations
- 👁️ Active layer toggling
- 🔀 Switch groups / layers
- 📦 Groups / layers
- 🔁 Time loops, reversing
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

There's no dedicated "stop" call, seek to the animation's duration to end it immediately.

---

## Sizing & Scaling

| Method | Behavior |
|---|---|
| `setSize(width, height)` | Sets the **clip size**, aka the container's visible bounds. Content outside this size is **clipped/cropped**. |
| `setAnimationSize(width, height)` | **Scales** the animation to fit the given size, without clipping. **Use this one.** |
| `canvas.getWidth()` / `canvas.getHeight()` | The animation's **actual, native size**, as in Synfig. |

**Remember:** if you want to scale an animation, always scale relative to `canvas.getWidth()` / `canvas.getHeight()` or more accuratly the aspect ratio not an arbitrary target size or you'll get distortion or unwanted cropping.

```java
float scale = desiredWidth / canvas.getWidth();
animation.setAnimationSize(canvas.getWidth() * scale, canvas.getHeight() * scale);
```

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
SifCanvas canvas = SifParser.parse(Gdx.files.internal("anim.sif"));

//Load assets (static, no instance needed)
Array<String> assets = SifAnimation.loadAssets(canvas, imagePath, audioPath, assetManager);

//Create + play
SifAnimation animation = new SifAnimation(canvas);
animation.play();

//Scale correctly (no clipping)
animation.setAnimationSize(targetWidth, targetHeight);

//Skip to end
animation.seekTo(animation.getDuration());

//Swap a texture
animation.swapTexture(0, myTexture);
```
