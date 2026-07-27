package com.sengame.gdxsynfig.synfig;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable;
import com.badlogic.gdx.utils.Array;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SifAnimation extends Container<Group> {
    private static final int MAX_GROUP_NESTING_DEPTH = 32;
    private final boolean isRoot;
    public final Array<Texture> locallyCreatedTextures;
    private final Map<String, Texture> locallyCreatedTextureMap;
    public final Array<Music> locallyCreatedMusic;
    private final AssetManager assetManagerRef;
    private final Array<MusicEvent> musicEvents = new Array<>();
    public final Array<Music> activeMusic = new Array<>();
    public final Array<SifImage> allImages = new Array<>();
    private final Texture.TextureFilter minFilter;
    private final Texture.TextureFilter magFilter;
    public float masterVolume = 1f;
    public final Group contentGroup;
    public float fps = 24f;
    public float currentTime = 0f;
    public float duration = 0f;
    public float speed = 1f;
    public boolean isPlaying = false;
    public boolean isLooping = false;
    private Param switchLayerParam;
    private boolean hasStopTarget = false;
    private float stopTargetTime = 0f;
    public int completedLoops = 0;
    public int targetLoopCount = 0;
    public boolean finished = false;
    private float baseWidth, baseHeight;
    private Runnable onPlay;
    private Runnable onPause;
    private Runnable onStop;
    private Runnable onSkip;
    private Runnable onReset;
    private Runnable onComplete;
    private Runnable onReverseComplete;
    private Runnable onFinished;
    public SifCanvas canvas;

    public void setOnPlay(Runnable r) { this.onPlay = r; }
    public void setOnPause(Runnable r) { this.onPause = r; }
    public void setOnStop(Runnable r) { this.onStop = r; }
    public void setOnSkip(Runnable r) { this.onSkip = r; }
    public void setOnReset(Runnable r) { this.onReset = r; }
    public void setOnComplete(Runnable r) { this.onComplete = r; }
    public void setOnReverseComplete(Runnable r) { this.onReverseComplete = r; }
    public void setOnFinished(Runnable r) { this.onFinished = r; }
    public boolean hasFinished() { return finished; }
    public int getLoopCount() { return completedLoops; }
    public void setLoopCount(int n) {
        this.targetLoopCount = n;
        this.isLooping = (n > 0);
        this.completedLoops = 0;
    }

    public float getMasterVolume() { return masterVolume; }
    public void setMasterVolume(float masterVolume) {
        this.masterVolume = masterVolume;
        for (MusicEvent me : musicEvents) {
            if (activeMusic.contains(me.music, true)) {
                me.music.setVolume(me.volume * masterVolume);
            }
        }
    }

    /**
     * Swaps the texture of animation at index n for one of your choosing
     * @param index
     * @param texture
     */
    public void swapTexture(int index, Texture texture) {
        if (texture != null) {
            swapTexture(index, new TextureRegion(texture));
        }
    }

    /**
     * Swaps the texture of animation at index n for one of your choosing
     * @param index
     * @param region
     */
    public void swapTexture(int index, TextureRegion region) {
        if (region == null || index < 0 || index >= allImages.size) {
            return;
        }

        SifImage imageActor = allImages.get(index);
        if (imageActor == null) {
            return;
        }

        float originalWidth = imageActor.getWidth();
        float originalHeight = imageActor.getHeight();
        float regionWidth = region.getRegionWidth();
        float regionHeight = region.getRegionHeight();

        if (regionWidth == 0 || regionHeight == 0) {
            return;
        }

        imageActor.setDrawable(new SpriteDrawable(new Sprite(region)));

        float newWidth, newHeight;
        if (regionWidth > regionHeight) {
            float scale = originalWidth / regionWidth;
            newWidth = originalWidth;
            newHeight = regionHeight * scale;
        } else {
            float scale = originalHeight / regionHeight;
            newWidth = regionWidth * scale;
            newHeight = originalHeight;
        }

        imageActor.setSize(newWidth, newHeight);
        imageActor.setOrigin(newWidth / 2f, newHeight / 2f);
    }

    public SifAnimation(SifCanvas canvas, FileHandle folderPath) {
        this(canvas, folderPath, folderPath, null, Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    public SifAnimation(SifCanvas canvas, FileHandle imagesFolder, FileHandle audioFolder) {
        this(canvas, imagesFolder, audioFolder, null, Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    public SifAnimation(SifCanvas canvas, FileHandle folderPath, Texture.TextureFilter filter) {
        this(canvas, folderPath, folderPath, null, filter, filter);
    }

    public SifAnimation(SifCanvas canvas, FileHandle imagesFolder, FileHandle audioFolder, Texture.TextureFilter filter) {
        this(canvas, imagesFolder, audioFolder, null, filter, filter);
    }

    public SifAnimation(SifCanvas canvas, FileHandle folderPath, Texture.TextureFilter minFilter, Texture.TextureFilter magFilter) {
        this(canvas, folderPath, folderPath, null, minFilter, magFilter);
    }

    public SifAnimation(SifCanvas canvas, FileHandle imagesFolder, FileHandle audioFolder, Texture.TextureFilter minFilter, Texture.TextureFilter magFilter) {
        this(canvas, imagesFolder, audioFolder, null, minFilter, magFilter);
    }

    public SifAnimation(SifCanvas canvas, FileHandle folderPath, AssetManager assetManager) {
        this(canvas, folderPath, folderPath, assetManager, null, null);
    }

    public SifAnimation(SifCanvas canvas, FileHandle imagesFolder, FileHandle audioFolder, AssetManager assetManager) {
        this(canvas, imagesFolder, audioFolder, assetManager, null, null);
    }

    public SifAnimation(SifCanvas canvas, FileHandle folderPath, AssetManager assetManager, Texture.TextureFilter filter) {
        this(canvas, folderPath, folderPath, assetManager, filter, filter);
    }

    public SifAnimation(SifCanvas canvas, FileHandle imagesFolder, FileHandle audioFolder, AssetManager assetManager, Texture.TextureFilter filter) {
        this(canvas, imagesFolder, audioFolder, assetManager, filter, filter);
    }

    public SifAnimation(SifCanvas canvas, FileHandle folderPath, AssetManager assetManager, Texture.TextureFilter minFilter, Texture.TextureFilter magFilter) {
        this(canvas, folderPath, folderPath, assetManager, minFilter, magFilter);
    }

    public SifAnimation(SifCanvas canvas, FileHandle folderPath, FileHandle audioFolderPath, AssetManager assetManager, Texture.TextureFilter minFilter, Texture.TextureFilter magFilter) {
        this.isRoot = true;
        this.locallyCreatedTextures = new Array<>();
        this.locallyCreatedTextureMap = new HashMap<>();
        this.locallyCreatedMusic = new Array<>();
        this.assetManagerRef = assetManager;
        this.minFilter = minFilter;
        this.magFilter = magFilter;
        this.fps = Float.parseFloat(canvas.getFps());
        this.canvas = canvas;

        this.contentGroup = new Group();
        this.contentGroup.setTransform(true);
        setActor(contentGroup);

        setTransform(true);
        setClip(true);

        this.childItems = new Array<>();

        init(canvas, folderPath, audioFolderPath, assetManager);
    }

    private SifAnimation(Array<Texture> sharedTextures, Map<String, Texture> sharedTextureMap, Array<Music> sharedMusic, AssetManager assetManager, Array<SifImage> sharedImages, Texture.TextureFilter minFilter, Texture.TextureFilter magFilter) {
        this.isRoot = false;
        this.locallyCreatedTextures = sharedTextures;
        this.locallyCreatedTextureMap = sharedTextureMap;
        this.locallyCreatedMusic = sharedMusic;
        this.assetManagerRef = assetManager;
        this.minFilter = minFilter;
        this.magFilter = magFilter;

        this.contentGroup = new Group();
        this.contentGroup.setTransform(true);
        setActor(contentGroup);

        setTransform(true);

        this.childItems = new Array<>();
    }

    private void init(SifCanvas canvas, FileHandle folderPath, FileHandle audioFolderPath, AssetManager assetManager) {
        if (isRoot) {
            setVisible(false);
            isPlaying = false;
        }

        this.fps = SifTimeUtils.parseTime(canvas.getFps(), 1f);
        if (fps <= 0) fps = Float.parseFloat(canvas.getFps());

        this.baseWidth = Float.parseFloat(canvas.getWidth());
        this.baseHeight = Float.parseFloat(canvas.getHeight());

        setSize(baseWidth, baseHeight);
        contentGroup.setSize(baseWidth, baseHeight);
        contentGroup.setOrigin(baseWidth / 2f, baseHeight / 2f);
        setOrigin(baseWidth / 2f, baseHeight / 2f);

        fill();

        float scaleFactor = calculateScaleFromCanvas(canvas, baseWidth);
        populateActors(canvas, folderPath, audioFolderPath, assetManager, scaleFactor, baseWidth / 2f, baseHeight / 2f, 0, this.musicEvents, this.allImages);

        updateTree(0f);
    }

    /**
     * If called, shows the first frame of the animation until play is called
     */
    public void showBeforeStart() {
        setVisible(true);
        currentTime = 0f;
        updateTree(currentTime);
    }

    public void resetAnimation() {
        isPlaying = false;
        currentTime = 0f;
        completedLoops = 0;
        finished = false;
        hasStopTarget = false;
        updateTree(currentTime);
        stopAllMusic();
        resetMusicEdgeState();
        if (onReset != null) onReset.run();
    }

    public void play() {
        if (!isPlaying) {
            if (finished) {
                completedLoops = 0;
                finished = false;
                currentTime = speed > 0 ? 0f : duration;
            }
            setVisible(true);
            isPlaying = true;
            for (Music m : activeMusic) {
                if (!m.isPlaying()) m.play();
            }
            if (onPlay != null) onPlay.run();
        }
    }

    public void play(int stopFrame) {
        this.stopTargetTime = stopFrame / fps;
        this.hasStopTarget = true;
        this.speed = Math.abs(this.speed);
        play();
    }

    public void continueAnim(int frame) {
        this.stopTargetTime = frame / fps;
        this.hasStopTarget = true;
        if (!isPlaying) play();
    }

    public void continueAnim() {
        this.hasStopTarget = false;
        if (!isPlaying) play();
    }

    public void pause() {
        if (isPlaying) {
            isPlaying = false;
            for (Music m : activeMusic) m.pause();
            if (onPause != null) onPause.run();
        }
    }

    public void stop() {
        isPlaying = false;
        currentTime = 0f;
        completedLoops = 0;
        finished = false;
        hasStopTarget = false;
        updateTree(currentTime);
        stopAllMusic();
        resetMusicEdgeState();
        if (onStop != null) onStop.run();
    }

    public void restart() {
        completedLoops = 0;
        finished = false;
        hasStopTarget = false;
        currentTime = speed > 0 ? 0f : duration;
        setVisible(true);
        isPlaying = true;
        updateTree(currentTime);
        stopAllMusic();
        resetMusicEdgeState();
        if (onPlay != null) onPlay.run();
    }

    public void reverse() {
        if (finished) {
            completedLoops = 0;
            finished = false;
            currentTime = duration;
        }
        this.speed = -Math.abs(this.speed);
        hasStopTarget = false;
        setVisible(true);
        isPlaying = true;
        stopAllMusic();
        if (onPlay != null) onPlay.run();
    }

    public void playForward() {
        if (finished) {
            completedLoops = 0;
            finished = false;
            currentTime = 0f;
        }
        this.speed = Math.abs(this.speed);
        hasStopTarget = false;
        setVisible(true);
        isPlaying = true;
        for (Music m : activeMusic) {
            if (!m.isPlaying()) m.play();
        }
        if (onPlay != null) onPlay.run();
    }

    public void setSpeed(float speed) { this.speed = speed; }
    public void setLooping(boolean looping) {
        this.isLooping = looping;
        if (looping && targetLoopCount == 0) {
            targetLoopCount = -1;
        } else if (!looping) {
            targetLoopCount = 0;
        }
    }

    /**
     * Skips to a specific frame in the animation
     * @param frame
     */
    public void skipToFrame(float frame) { skipToTime(frame / fps); }

    /**
     * Skips to a specific time in the animation
     * @param timeSeconds
     */
    public void skipToTime(float timeSeconds) {
        this.currentTime = timeSeconds;
        updateTree(currentTime);
        stopAllMusic();

        for (MusicEvent me : musicEvents) {
            boolean active = isMusicEventBranchActive(me);
            float effectiveTime = me.owningGroup.applyTimeLoopChain(me, me.owningGroup.currentGroupTime);
            me.lastEffectiveTime = effectiveTime;

            if (active && effectiveTime >= me.time) {
                float offset = effectiveTime - me.time;
                me.music.setVolume(me.volume * masterVolume);
                me.music.setPosition(offset);
                if (isPlaying) me.music.play();
                else me.music.pause();
                if (!activeMusic.contains(me.music, true)) activeMusic.add(me.music);
            }
        }
        if (onSkip != null) onSkip.run();
    }

    private void stopAllMusic() {
        for (Music m : activeMusic) m.stop();
        activeMusic.clear();
    }

    float trueAmount = 1f;

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (trueAmount > 1f && batch instanceof SpriteBatch) {
            SpriteBatch spriteBatch = (SpriteBatch) batch;
            AlphaOverflowShader.begin(spriteBatch, trueAmount);
            drawInternal(batch, parentAlpha);
            AlphaOverflowShader.end(batch);
        } else {
            drawInternal(batch, parentAlpha);
        }
    }

    private void drawInternal(Batch batch, float parentAlpha) {
        Color color = getColor();
        float oldBatchColor = batch.getPackedColor();
        batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
        super.draw(batch, parentAlpha);
        batch.setPackedColor(oldBatchColor);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (!isRoot || !isPlaying) return;

        currentTime += delta * speed;

        if (hasStopTarget && ((speed > 0 && currentTime >= stopTargetTime) || (speed < 0 && currentTime <= stopTargetTime))) {
            currentTime = stopTargetTime;
            isPlaying = false;
            hasStopTarget = false;
            updateTree(currentTime);
            stopAllMusic();
            if (onComplete != null) onComplete.run();
            return;
        }

        if (speed > 0 && currentTime >= duration) {
            completedLoops++;
            boolean shouldContinue = (targetLoopCount > 0 && completedLoops < targetLoopCount) || (isLooping || targetLoopCount == -1);
            if (shouldContinue) {
                currentTime = 0f;
                if (onComplete != null) onComplete.run();
            } else {
                currentTime = duration;
                isPlaying = false;
                finished = true;
                if (onComplete != null) onComplete.run();
                if (onFinished != null) onFinished.run();
            }
            stopAllMusic();
        } else if (speed < 0 && currentTime <= 0f) {
            completedLoops++;
            boolean shouldContinue = (targetLoopCount > 0 && completedLoops < targetLoopCount) || (isLooping || targetLoopCount == -1);
            if (shouldContinue) {
                currentTime = duration;
                if (onReverseComplete != null) onReverseComplete.run();
            } else {
                currentTime = 0f;
                isPlaying = false;
                finished = true;
                if (onReverseComplete != null) onReverseComplete.run();
                if (onFinished != null) onFinished.run();
            }
            stopAllMusic();
        }

        updateTree(currentTime);
        checkMusicTriggers();
    }

    private void checkMusicTriggers() {
        for (MusicEvent me : musicEvents) {
            boolean active = isMusicEventBranchActive(me);
            float effectiveTime = me.owningGroup.applyTimeLoopChain(me, me.owningGroup.currentGroupTime);

            boolean crossedForward = active && speed > 0
                && me.lastEffectiveTime <= me.time && effectiveTime > me.time;
            if (crossedForward) {
                me.music.setVolume(me.volume * masterVolume);
                me.music.setPosition(0);
                me.music.play();
                if (!activeMusic.contains(me.music, true)) activeMusic.add(me.music);
            }

            me.lastEffectiveTime = effectiveTime;
        }
    }

    private boolean isMusicEventBranchActive(MusicEvent me) {
        SifAnimation owner = me.owningGroup;
        if (owner.switchLayerParam == null || owner.switchLayerParam.getValue() == null) return true;
        String activeBranch = evaluateActiveLayerName(owner.switchLayerParam.getValue(), owner.currentGroupTime, owner.fps);
        return me.branchDesc != null && me.branchDesc.equals(activeBranch);
    }

    private void resetMusicEdgeState() {
        for (MusicEvent me : musicEvents) {
            me.lastEffectiveTime = Float.NEGATIVE_INFINITY;
        }
    }

    private interface TimeTransform {
        float apply(float time);
    }

    private static class TimeLoop implements TimeTransform {
        final float linkTime;
        final float localTime;
        final float duration;
        final boolean symmetrical;
        TimeLoop(float linkTime, float localTime, float duration, boolean symmetrical) {
            this.linkTime = linkTime;
            this.localTime = localTime;
            this.duration = duration;
            this.symmetrical = symmetrical;
        }

        @Override
        public float apply(float time) {
            if (duration <= 0) return time;
            float delta = time - linkTime;
            if (symmetrical) {
                float period = 2 * duration;
                float mod = ((delta % period) + period) % period;
                if (mod > duration) mod = period - mod;
                return localTime + mod;
            } else {
                float mod = ((delta % duration) + duration) % duration;
                return localTime + mod;
            }
        }
    }

    private static class Stroboscope implements TimeTransform {
        private final ValueNode frequencyNode;
        private final float fpsRef;

        Stroboscope(ValueNode frequencyNode, float fpsRef) {
            this.frequencyNode = frequencyNode;
            this.fpsRef = fpsRef;
        }

        @Override
        public float apply(float time) {
            float frequency = 1f;
            if (frequencyNode != null) {
                Double v = frequencyNode.isAnimated()
                    ? evaluateAnimatedScalar(frequencyNode, time, fpsRef, 1f)
                    : frequencyNode.getDoubleValue();
                if (v != null) frequency = v.floatValue();
            }
            if (frequency <= 0f) return time;
            return (float) Math.floor(time * frequency) / frequency;
        }
    }

    private static class FreeTime implements TimeTransform {
        private final ValueNode timeNode;
        private final float fpsRef;

        FreeTime(ValueNode timeNode, float fpsRef) {
            this.timeNode = timeNode;
            this.fpsRef = fpsRef;
        }

        @Override
        public float apply(float time) {
            if (timeNode == null) return time;
            return evaluateAnimatedTime(timeNode, time, fpsRef);
        }
    }

    private final Array<Object> childItems;
    private final java.util.IdentityHashMap<Object, Array<TimeTransform>> timeLoopChains = new java.util.IdentityHashMap<>();
    private float currentGroupTime = 0f;

    private void precomputeTimeLoopChains() {
        for (int i = 0; i < childItems.size; i++) {
            Object item = childItems.get(i);
            if (!(item instanceof Actor) && !(item instanceof MusicEvent)) continue;

            Array<TimeTransform> chain = new Array<>();
            for (int j = childItems.size - 1; j > i; j--) {
                Object above = childItems.get(j);
                if (above instanceof TimeTransform) {
                    chain.add((TimeTransform) above);
                }
            }
            timeLoopChains.put(item, chain);
        }
    }

    private float applyTimeLoopChain(Object item, float time) {
        Array<TimeTransform> chain = timeLoopChains.get(item);
        if (chain == null) return time;
        float effectiveTime = time;
        for (TimeTransform tt : chain) {
            effectiveTime = tt.apply(effectiveTime);
        }
        return effectiveTime;
    }

    private void updateTree(float time) {
        currentGroupTime = time;

        if (switchLayerParam != null && switchLayerParam.getValue() != null) {
            String activeLayerName = evaluateActiveLayerName(switchLayerParam.getValue(), time, fps);
            for (Actor child : contentGroup.getChildren()) {
                Object obj = child.getUserObject();
                if (obj instanceof String) {
                    String childDesc = (String) obj;
                    boolean matches = childDesc.equals(activeLayerName);
                    child.setVisible(matches);
                }
            }
        }

        for (int i = 0; i < childItems.size; i++) {
            Object item = childItems.get(i);
            if (!(item instanceof Actor)) continue;

            Actor actor = (Actor) item;
            float effectiveTime = applyTimeLoopChain(item, time);

            if (actor instanceof SifImage) {
                ((SifImage) actor).updateAnimation(effectiveTime, fps);
            } else if (actor instanceof SifAnimation) {
                SifAnimation group = (SifAnimation) actor;
                SifImage.applyState(group, effectiveTime, fps, group.getAmountParam(), group.getTransformParam(), group.getScaleFactor(), group.getCanvasCenterX(), group.getCanvasCenterY());
                float childTime = group.resolveLocalTime(effectiveTime);
                group.updateTree(childTime);
            }
        }
    }

    private static String evaluateActiveLayerName(ValueNode node, float time, float fps) {
        if (!node.isAnimated()) {
            return node.getStringValue();
        }
        List<Waypoint> wps = node.getWaypoints();
        if (wps == null || wps.isEmpty()) {
            return node.getStringValue();
        }

        Waypoint prev = null;
        for (Waypoint wp : wps) {
            float wpTime = SifTimeUtils.parseTime(wp.getTime(), fps);
            if (wpTime <= time) {
                prev = wp;
            } else {
                break;
            }
        }

        if (prev != null && prev.getValue() != null) {
            return prev.getValue().getStringValue();
        }
        return node.getStringValue();
    }

    private void populateActors(SifCanvas canvas, FileHandle folderPath, FileHandle audioFolderPath, AssetManager assetManager, float scaleFactor, float canvasCenterX, float canvasCenterY, int depth, Array<MusicEvent> masterMusicList, Array<SifImage> masterImageList) {
        for (Layer layer : canvas.getLayers()) {
            if (!layer.isActive() || layer.isExcludeFromRendering()) continue;

            String type = layer.getType() == null ? "" : layer.getType();
            switch (type) {
                case "sound":
                    handleMusicLayer(layer, assetManager, audioFolderPath, masterMusicList);
                    break;
                case "group":
                    handleGroupLayer(layer, assetManager, folderPath, audioFolderPath, scaleFactor, canvasCenterX, canvasCenterY, depth, masterMusicList, masterImageList);
                    break;
                case "switch":
                case "pastecanvas":
                    handleSwitchLayer(layer, assetManager, folderPath, audioFolderPath, scaleFactor, canvasCenterX, canvasCenterY, depth, masterMusicList, masterImageList);
                    break;
                case "timeloop":
                    handleTimeLoopLayer(layer);
                    break;
                case "stroboscope":
                    handleStroboscopeLayer(layer);
                    break;
                case "freetime":
                    handleFreeTimeLayer(layer);
                    break;
                default:
                    handleImageLayer(layer, assetManager, folderPath, scaleFactor, canvasCenterX, canvasCenterY, masterImageList);
                    break;
            }
            updateDuration(layer);
        }
        precomputeTimeLoopChains();
    }

    private Param amountParam;
    private Param transformParam;
    private Param timeOffsetParam;
    private Param timeDilationParam;
    private float nestedScaleFactor, nestedCx, nestedCy;
    public Param getAmountParam() { return amountParam; }
    public Param getTransformParam() { return transformParam; }
    public float getScaleFactor() { return nestedScaleFactor; }
    public float getCanvasCenterX() { return nestedCx; }
    public float getCanvasCenterY() { return nestedCy; }
    public Param getSwitchLayerParam() { return switchLayerParam; }
    public Param getTimeOffsetParam() { return timeOffsetParam; }
    public Param getTimeDilationParam() { return timeDilationParam; }

    private float resolveLocalTime(float parentTime) {
        float dilation = 1f;
        if (timeDilationParam != null && timeDilationParam.getValue() != null) {
            ValueNode dNode = timeDilationParam.getValue();
            Double d = dNode.isAnimated()
                ? evaluateAnimatedScalar(dNode, parentTime, fps, 1f)
                : dNode.getDoubleValue();
            if (d != null) dilation = d.floatValue();
        }

        float offset = 0f;
        if (timeOffsetParam != null && timeOffsetParam.getValue() != null) {
            ValueNode oNode = timeOffsetParam.getValue();
            offset = evaluateAnimatedTime(oNode, parentTime, fps);
        }

        return parentTime * dilation + offset;
    }

    private static Double evaluateAnimatedScalar(ValueNode node, float time, float fps, float def) {
        if (!node.isAnimated()) {
            Double v = node.getDoubleValue();
            return v != null ? v : (double) def;
        }
        List<Waypoint> wps = node.getWaypoints();
        if (wps == null || wps.isEmpty()) return (double) def;

        Waypoint prev = null;
        Waypoint next = null;
        for (Waypoint wp : wps) {
            float wpTime = SifTimeUtils.parseTime(wp.getTime(), fps);
            if (wpTime <= time) {
                prev = wp;
            } else {
                next = wp;
                break;
            }
        }

        if (prev == null && next != null) return valueAsDouble(next.getValue(), def);
        if (prev != null && next == null) return valueAsDouble(prev.getValue(), def);
        if (prev == null) return (double) def;

        float t0 = SifTimeUtils.parseTime(prev.getTime(), fps);
        float t1 = SifTimeUtils.parseTime(next.getTime(), fps);
        float duration = t1 - t0;
        if (duration <= 0) return valueAsDouble(prev.getValue(), def);

        float progress = (time - t0) / duration;
        double v0 = valueAsDouble(prev.getValue(), def);
        double v1 = valueAsDouble(next.getValue(), def);

        if ("constant".equals(prev.getAfter()) || "constant".equals(next.getBefore())) {
            return progress < 1.0f ? v0 : v1;
        }
        return v0 + (v1 - v0) * progress;
    }

    private static double valueAsDouble(ValueNode val, float def) {
        if (val == null) return def;
        Double d = val.getDoubleValue();
        return d != null ? d : def;
    }

    private static float evaluateAnimatedTime(ValueNode node, float time, float fps) {
        if (!node.isAnimated()) {
            return SifTimeUtils.parseTime(node.getTimeRaw(), fps);
        }
        List<Waypoint> wps = node.getWaypoints();
        if (wps == null || wps.isEmpty()) {
            return SifTimeUtils.parseTime(node.getTimeRaw(), fps);
        }

        Waypoint prev = null;
        Waypoint next = null;
        for (Waypoint wp : wps) {
            float wpTime = SifTimeUtils.parseTime(wp.getTime(), fps);
            if (wpTime <= time) {
                prev = wp;
            } else {
                next = wp;
                break;
            }
        }

        if (prev == null && next != null) {
            return next.getValue() != null ? SifTimeUtils.parseTime(next.getValue().getTimeRaw(), fps) : 0f;
        }
        if (prev != null && next == null) {
            return prev.getValue() != null ? SifTimeUtils.parseTime(prev.getValue().getTimeRaw(), fps) : 0f;
        }
        if (prev == null) return 0f;

        float t0 = SifTimeUtils.parseTime(prev.getTime(), fps);
        float t1 = SifTimeUtils.parseTime(next.getTime(), fps);
        float duration = t1 - t0;
        float v0 = prev.getValue() != null ? SifTimeUtils.parseTime(prev.getValue().getTimeRaw(), fps) : 0f;
        if (duration <= 0) return v0;

        float v1 = next.getValue() != null ? SifTimeUtils.parseTime(next.getValue().getTimeRaw(), fps) : 0f;
        float progress = (time - t0) / duration;

        if ("constant".equals(prev.getAfter()) || "constant".equals(next.getBefore())) {
            return progress < 1.0f ? v0 : v1;
        }
        return v0 + (v1 - v0) * progress;
    }

    private void handleGroupLayer(Layer layer, AssetManager assetManager, FileHandle folderPath, FileHandle audioFolderPath, float scaleFactor, float canvasCenterX, float canvasCenterY, int depth, Array<MusicEvent> masterMusicList, Array<SifImage> masterImageList) {
        if (depth >= MAX_GROUP_NESTING_DEPTH) return;
        SifCanvas childCanvas = layer.getChildCanvas();
        if (childCanvas == null) return;

        SifAnimation nestedGroup = new SifAnimation(locallyCreatedTextures, locallyCreatedTextureMap, locallyCreatedMusic, assetManager, masterImageList, minFilter, magFilter);

        nestedGroup.amountParam = layer.getParam("amount");
        nestedGroup.transformParam = layer.getParam("transformation");
        nestedGroup.timeOffsetParam = layer.getParam("time_offset");
        nestedGroup.timeDilationParam = layer.getParam("time_dilation");
        nestedGroup.nestedScaleFactor = scaleFactor;
        nestedGroup.nestedCx = canvasCenterX;
        nestedGroup.nestedCy = canvasCenterY;

        nestedGroup.populateActors(childCanvas, folderPath, audioFolderPath, assetManager, scaleFactor, 0f, 0f, depth + 1, masterMusicList, masterImageList);
        nestedGroup.setUserObject(layer.getDesc());
        this.contentGroup.addActor(nestedGroup);
        this.childItems.add(nestedGroup);
    }

    private void handleSwitchLayer(Layer layer, AssetManager assetManager, FileHandle folderPath, FileHandle audioFolderPath, float scaleFactor, float canvasCenterX, float canvasCenterY, int depth, Array<MusicEvent> masterMusicList, Array<SifImage> masterImageList) {
        if (depth >= MAX_GROUP_NESTING_DEPTH) return;
        SifCanvas childCanvas = layer.getChildCanvas();
        if (childCanvas == null) return;

        SifAnimation switchGroup = new SifAnimation(locallyCreatedTextures, locallyCreatedTextureMap, locallyCreatedMusic, assetManager, masterImageList, minFilter, magFilter);

        switchGroup.amountParam = layer.getParam("amount");
        switchGroup.transformParam = layer.getParam("transformation");
        switchGroup.timeOffsetParam = layer.getParam("time_offset");
        switchGroup.timeDilationParam = layer.getParam("time_dilation");

        switchGroup.switchLayerParam = layer.getParam("layer");
        if (switchGroup.switchLayerParam == null) {
            switchGroup.switchLayerParam = layer.getParam("layer_name");
        }
        if (switchGroup.switchLayerParam == null) {
            switchGroup.switchLayerParam = layer.getParam("active_layer");
        }

        switchGroup.nestedScaleFactor = scaleFactor;
        switchGroup.nestedCx = canvasCenterX;
        switchGroup.nestedCy = canvasCenterY;

        switchGroup.populateActors(childCanvas, folderPath, audioFolderPath, assetManager, scaleFactor, 0f, 0f, depth + 1, masterMusicList, masterImageList);
        switchGroup.setUserObject(layer.getDesc());
        this.contentGroup.addActor(switchGroup);
        this.childItems.add(switchGroup);
    }

    private void handleImageLayer(Layer layer, AssetManager assetManager, FileHandle folderPath, float scaleFactor, float cx, float cy, Array<SifImage> masterImageList) {
        String layerName = layer.getDesc();
        if (layerName == null || layerName.isEmpty()) return;

        TextureRegion region = resolveTexture(folderPath.child(layerName), assetManager);
        if (region == null) return;

        SifImage animatedImage = new SifImage(layer, region, scaleFactor, cx, cy);
        animatedImage.setUserObject(layer.getDesc());
        this.contentGroup.addActor(animatedImage);
        if (masterImageList != null) {
            masterImageList.add(animatedImage);
        }
        this.childItems.add(animatedImage);
    }

    private void handleMusicLayer(Layer layer, AssetManager assetManager, FileHandle audioFolderPath, Array<MusicEvent> masterMusicList) {
        String soundName = layer.getDesc();
        if (soundName == null || soundName.isEmpty()) return;

        float delaySeconds = 0f;
        Param delayParam = layer.getParam("delay");
        if (delayParam != null && delayParam.getValue() != null) {
            delaySeconds = SifTimeUtils.parseTime(delayParam.getValue().getTimeRaw(), fps);
        }

        float volume = 1f;
        Param volumeParam = layer.getParam("volume");
        if (volumeParam != null && volumeParam.getValue() != null && volumeParam.getValue().getDoubleValue() != null) {
            volume = volumeParam.getValue().getDoubleValue().floatValue();
        }

        Music music = resolveMusic(audioFolderPath.child(soundName), assetManager);
        if (music != null) {
            MusicEvent musicEvent = new MusicEvent(delaySeconds, music, volume, this, layer.getDesc());
            masterMusicList.add(musicEvent);
            childItems.add(musicEvent);
        }
    }

    private void handleTimeLoopLayer(Layer layer) {
        float linkTime = 0f;
        Param linkTimeParam = layer.getParam("link_time");
        if (linkTimeParam != null && linkTimeParam.getValue() != null) {
            String val = linkTimeParam.getValue().getStringValue();
            if (val != null) linkTime = SifTimeUtils.parseTime(val, fps);
        }

        float localTime = 0f;
        Param localTimeParam = layer.getParam("local_time");
        if (localTimeParam != null && localTimeParam.getValue() != null) {
            String val = localTimeParam.getValue().getStringValue();
            if (val != null) localTime = SifTimeUtils.parseTime(val, fps);
        }

        float duration = 0f;
        Param durationParam = layer.getParam("duration");
        if (durationParam != null && durationParam.getValue() != null) {
            String val = durationParam.getValue().getStringValue();
            if (val != null) duration = SifTimeUtils.parseTime(val, fps);
        }

        boolean symmetrical = false;
        Param symmetricalParam = layer.getParam("symmetrical");
        if (symmetricalParam != null && symmetricalParam.getValue() != null) {
            Boolean b = symmetricalParam.getValue().getBooleanValue();
            if (b != null) symmetrical = b;
        }

        TimeLoop tl = new TimeLoop(linkTime, localTime, duration, symmetrical);
        childItems.add(tl);
    }

    private void handleStroboscopeLayer(Layer layer) {
        Param frequencyParam = layer.getParam("frequency");
        ValueNode frequencyNode = frequencyParam != null ? frequencyParam.getValue() : null;

        Stroboscope strobe = new Stroboscope(frequencyNode, fps);
        childItems.add(strobe);
    }

    private void handleFreeTimeLayer(Layer layer) {
        Param timeParam = layer.getParam("time");
        ValueNode timeNode = timeParam != null ? timeParam.getValue() : null;

        FreeTime freeTime = new FreeTime(timeNode, fps);
        childItems.add(freeTime);
    }

    private void updateDuration(Layer layer) {
        updateDuration(layer, 0f, 1f);
    }

    private void updateDuration(Layer layer, float parentOffset, float parentDilation) {
        Param tx = layer.getParam("transformation");
        if (tx != null && tx.getValue() != null) {
            duration = Math.max(duration, toRootTime(SifImage.getMaxTime(tx.getValue(), fps), parentOffset, parentDilation));
        }
        Param amt = layer.getParam("amount");
        if (amt != null && amt.getValue() != null) {
            duration = Math.max(duration, toRootTime(SifImage.getMaxTime(amt.getValue(), fps), parentOffset, parentDilation));
        }
        Param sw = layer.getParam("layer");
        if (sw == null) sw = layer.getParam("layer_name");
        if (sw == null) sw = layer.getParam("active_layer");
        if (sw != null && sw.getValue() != null) {
            duration = Math.max(duration, toRootTime(SifImage.getMaxTime(sw.getValue(), fps), parentOffset, parentDilation));
        }

        if (layer.getChildCanvas() != null) {
            float childOffset = parentOffset;
            float childDilation = parentDilation;

            String type = layer.getType() == null ? "" : layer.getType();
            if ("group".equals(type) || "switch".equals(type) || "pastecanvas".equals(type)) {
                float ownOffset = 0f;
                Param offsetParam = layer.getParam("time_offset");
                if (offsetParam != null && offsetParam.getValue() != null) {
                    ownOffset = SifTimeUtils.parseTime(offsetParam.getValue().getTimeRaw(), fps);
                }
                float ownDilation = 1f;
                Param dilationParam = layer.getParam("time_dilation");
                if (dilationParam != null && dilationParam.getValue() != null && dilationParam.getValue().getDoubleValue() != null) {
                    ownDilation = dilationParam.getValue().getDoubleValue().floatValue();
                }

                childDilation = parentDilation * ownDilation;
                childOffset = parentOffset * ownDilation + ownOffset;
            }

            for (Layer childLayer : layer.getChildCanvas().getLayers()) {
                updateDuration(childLayer, childOffset, childDilation);
            }
        }
    }

    private static float toRootTime(float localTime, float offset, float dilation) {
        if (dilation == 0f) return localTime + offset;
        return (localTime - offset) / dilation;
    }

    private static class MusicEvent {
        float time, volume;
        Music music;
        final SifAnimation owningGroup;
        final String branchDesc;
        float lastEffectiveTime = Float.NEGATIVE_INFINITY;

        MusicEvent(float t, Music m, float v, SifAnimation owningGroup, String branchDesc) {
            time = t; music = m; volume = v; this.owningGroup = owningGroup; this.branchDesc = branchDesc;
        }
    }

    private TextureRegion resolveTexture(FileHandle file, AssetManager assetManager) {
        Texture tex;
        if (assetManager != null) {
            String path = file.path();
            if (!assetManager.isLoaded(path, Texture.class)) {
                Gdx.app.error("SifAnimGroup", "Texture not loaded in AssetManager, skipping: " + path);
                return null;
            }
            tex = assetManager.get(path, Texture.class);
        } else {
            if (!file.exists()) return null;
            String path = file.path();
            if (locallyCreatedTextureMap != null && locallyCreatedTextureMap.containsKey(path)) {
                tex = locallyCreatedTextureMap.get(path);
            } else {
                tex = new Texture(file);
                locallyCreatedTextures.add(tex);
                if (locallyCreatedTextureMap != null) {
                    locallyCreatedTextureMap.put(path, tex);
                }
            }
        }

        if (minFilter != null) {
            tex.setFilter(minFilter, magFilter != null ? magFilter : minFilter);
        }
        return new TextureRegion(tex);
    }

    private Music resolveMusic(FileHandle file, AssetManager assetManager) {
        if (assetManager != null) {
            String path = file.path();
            if (!assetManager.isLoaded(path, Music.class)) {
                Gdx.app.error("SifAnimGroup", "Music not loaded in AssetManager, skipping: " + path);
                return null;
            }
            return assetManager.get(path, Music.class);
        }
        if (!file.exists()) return null;
        Music music = Gdx.audio.newMusic(file);
        locallyCreatedMusic.add(music);
        return music;
    }

    /**
     * Loads the assets and returns Array String of the assets loaded
     * @param canvas
     * @param folderPath
     * @param assetManager
     * @return
     */
    public static Array<String> LoadAssets(SifCanvas canvas, FileHandle folderPath, AssetManager assetManager) {
        return LoadAssets(canvas, folderPath, folderPath, assetManager);
    }

    /**
     * Loads the assets and returns Array String of the assets loaded
     * @param canvas
     * @param assetFolderPath
     * @param audioFolderPath
     * @param assetManager
     * @return
     */
    public static Array<String> LoadAssets(SifCanvas canvas, FileHandle assetFolderPath, FileHandle audioFolderPath, AssetManager assetManager) {
        Array<String> loadedPaths = new Array<>();
        if (canvas == null || assetManager == null) return loadedPaths;
        collectAndQueueAssets(canvas, assetFolderPath, audioFolderPath, assetManager, loadedPaths, 0);
        return loadedPaths;
    }

    private static void collectAndQueueAssets(SifCanvas canvas, FileHandle assetFolderPath, FileHandle audioFolderPath, AssetManager assetManager, Array<String> loadedPaths, int depth) {
        if (canvas == null || depth >= MAX_GROUP_NESTING_DEPTH) return;

        for (Layer layer : canvas.getLayers()) {
            if (!layer.isActive() || layer.isExcludeFromRendering()) continue;

            String type = layer.getType() == null ? "" : layer.getType();
            switch (type) {
                case "sound": {
                    String soundName = layer.getDesc();
                    if (soundName == null || soundName.isEmpty()) break;
                    queueAsset(assetManager, audioFolderPath.child(soundName).path(), Music.class, loadedPaths);
                    break;
                }
                case "group":
                case "switch":
                case "pastecanvas": {
                    SifCanvas childCanvas = layer.getChildCanvas();
                    collectAndQueueAssets(childCanvas, assetFolderPath, audioFolderPath, assetManager, loadedPaths, depth + 1);
                    break;
                }
                case "timeloop":
                case "stroboscope":
                case "freetime":
                    break;
                default: {
                    String layerName = layer.getDesc();
                    if (layerName == null || layerName.isEmpty()) break;
                    queueAsset(assetManager, assetFolderPath.child(layerName).path(), Texture.class, loadedPaths);
                    break;
                }
            }
        }
    }

    private static <T> void queueAsset(AssetManager assetManager, String fullPath, Class<T> type, Array<String> loadedPaths) {
        if (!assetManager.isLoaded(fullPath, type)) {
            assetManager.load(fullPath, type);
        }
        if (!loadedPaths.contains(fullPath, false)) {
            loadedPaths.add(fullPath);
        }
    }

    public float calculateScaleFromCanvas(SifCanvas canvas, float targetStageWidth) {
        float sifWidth = Float.parseFloat(canvas.getWidth());
        String[] parts = canvas.getViewBox().split(" ");
        float minX = Float.parseFloat(parts[0]);
        float maxX = Float.parseFloat(parts[2]);
        float viewboxWidth = Math.abs(maxX - minX);
        float baseScale = sifWidth / viewboxWidth;
        float ratio = targetStageWidth / sifWidth;
        return baseScale * ratio;
    }

    /**
     * Sets the scale of the animation based off the given values, this is a container so setSize sets the clip size not the actual animation size, use this method to set the size and use setSize to clip.
     * @param width
     * @param height
     */
    public void setAnimationSize(float width, float height){
        //Converts the width height you put into a scale since this is a container and changing its size clips it.
        setScale(width/Float.parseFloat(canvas.getWidth()), height/Float.parseFloat(canvas.getHeight()));
    }

    public void dispose() {
        if (assetManagerRef == null) {
            for (Texture tex : locallyCreatedTextures) tex.dispose();
            locallyCreatedTextures.clear();
            if (locallyCreatedTextureMap != null) locallyCreatedTextureMap.clear();
            for (Music music : locallyCreatedMusic) music.dispose();
            locallyCreatedMusic.clear();
        }
    }
}
