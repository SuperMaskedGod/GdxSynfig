package com.sengame.gdxsynfig.synfig;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class SifImage extends Image {
    private final Param amountParam;
    private final Param transformParam;
    private final float scaleFactor;
    private final float canvasCenterX;
    private final float canvasCenterY;

    public SifImage(Layer layer, TextureRegion region, float scaleFactor, float canvasCenterX, float canvasCenterY) {
        super(region);
        this.amountParam = layer.getParam("amount");
        this.transformParam = layer.getParam("transformation");
        this.scaleFactor = scaleFactor;
        this.canvasCenterX = canvasCenterX;
        this.canvasCenterY = canvasCenterY;

        setOrigin(getWidth() / 2f, getHeight() / 2f);
    }

    public void updateAnimation(float currentTime, float fps) {
        applyState(this, currentTime, fps, amountParam, transformParam, scaleFactor, canvasCenterX, canvasCenterY);
    }

    public static void applyState(Actor target, float time, float fps, Param amountParam, Param transformParam, float scaleFactor, float cx, float cy) {
        float alpha = 1f;
        if (amountParam != null && amountParam.getValue() != null) {
            alpha = evaluateScalar(amountParam.getValue(), time, fps, 1f);
        }
        target.getColor().a = alpha;

        if (transformParam == null || transformParam.getValue() == null) {
            target.setPosition(cx - target.getOriginX(), cy - target.getOriginY());
            return;
        }

        ValueNode tNode = transformParam.getValue();

        ValueNode offsetNode = tNode.getNamedChild("offset");
        float x = cx - target.getOriginX();
        float y = cy - target.getOriginY();
        if (offsetNode != null) {
            float[] offsetXY = evaluateXY(offsetNode, time, fps, 0f, 0f);
            x += offsetXY[0] * scaleFactor;
            y += offsetXY[1] * scaleFactor;
        }
        target.setPosition(x, y);

        ValueNode angleNode = tNode.getNamedChild("angle");
        float angle = (angleNode != null) ? evaluateScalar(angleNode, time, fps, 0f) : 0f;
        target.setRotation(angle);

        ValueNode scaleNode = tNode.getNamedChild("scale");
        float scaleX = 1f;
        float scaleY = 1f;
        if (scaleNode != null) {
            float[] scaleXY = evaluateXY(scaleNode, time, fps, 1f, 1f);
            scaleX = scaleXY[0];
            scaleY = scaleXY[1];
        }
        target.setScale(scaleX, scaleY);
    }

    private static float evaluateScalar(ValueNode node, float time, float fps, float def) {
        if (!node.isAnimated()) return extractDoubleAsFloat(node, def);
        return interpolateWaypoints(node.getWaypoints(), time, fps, def, true);
    }

    private static float[] evaluateXY(ValueNode node, float time, float fps, float defX, float defY) {
        if (!node.isAnimated()) {
            float x = (node.getX() != null) ? node.getX().floatValue() : defX;
            float y = (node.getY() != null) ? node.getY().floatValue() : defY;
            return new float[]{x, y};
        }
        return interpolateWaypointsXY(node.getWaypoints(), time, fps, defX, defY);
    }

    private static final Map<List<Waypoint>, int[]> waypointCursorCache = new WeakHashMap<>();

    private static int[] cursorFor(List<Waypoint> wps) {
        int[] cursor = waypointCursorCache.get(wps);
        if (cursor == null) {
            cursor = new int[]{0};
            waypointCursorCache.put(wps, cursor);
        }
        return cursor;
    }

    private static int resolveStartIndex(List<Waypoint> wps, int[] cursor, float time, float fps) {
        int idx = cursor[0];
        if (idx < 0 || idx >= wps.size()) return 0;
        float cursorTime = SifTimeUtils.parseTime(wps.get(idx).getTime(), fps);
        if (cursorTime > time) return 0;
        return idx;
    }

    private static float hermite(float p1, float p2, float m1, float m2, float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        return (2 * t3 - 3 * t2 + 1) * p1 + (t3 - 2 * t2 + t) * m1 + (-2 * t3 + 3 * t2) * p2 + (t3 - t2) * m2;
    }

    private static float getTangent(List<Waypoint> wps, int prevIdx, int nextIdx, float fps, boolean isX, boolean isOut) {
        Waypoint cur = wps.get(prevIdx);
        Waypoint next = wps.get(nextIdx);

        String interpType = isOut ? cur.getAfter() : next.getBefore();
        if (interpType == null) interpType = "clamped";

        if ("linear".equals(interpType)) {
            return extractVal(next.getValue(), 0f, isX) - extractVal(cur.getValue(), 0f, isX);
        } else if ("ease".equals(interpType) || "ease-in".equals(interpType) || "ease-out".equals(interpType) || "ease-in-out".equals(interpType) || "halt".equals(interpType)) {
            return 0f;
        }

        float p1 = extractVal(cur.getValue(), 0f, isX);
        float p2 = extractVal(next.getValue(), 0f, isX);
        float t1 = SifTimeUtils.parseTime(cur.getTime(), fps);
        float t2 = SifTimeUtils.parseTime(next.getTime(), fps);

        int p0Idx = Math.max(0, prevIdx - 1);
        int p3Idx = Math.min(wps.size() - 1, nextIdx + 1);

        Waypoint prev = wps.get(p0Idx);
        Waypoint post = wps.get(p3Idx);

        float p0 = extractVal(prev.getValue(), 0f, isX);
        float p3 = extractVal(post.getValue(), 0f, isX);
        float t0 = SifTimeUtils.parseTime(prev.getTime(), fps);
        float t3 = SifTimeUtils.parseTime(post.getTime(), fps);

        if (t1 == t0) {
            p0 = p1 - (p2 - p1);
            t0 = t1 - (t2 - t1);
        }
        if (t3 == t2) {
            p3 = p2 + (p2 - p1);
            t3 = t2 + (t2 - t1);
        }

        float dt10 = t1 - t0;
        float dt21 = t2 - t1;
        float dt32 = t3 - t2;

        float d1 = (dt10 == 0) ? 0 : (p1 - p0) / dt10;
        float d2 = (dt21 == 0) ? 0 : (p2 - p1) / dt21;
        float d3 = (dt32 == 0) ? 0 : (p3 - p2) / dt32;

        float m;
        if (isOut) {
            float t = cur.getTension();
            float c = cur.getContinuity();
            float b = cur.getBias();
            m = 0.5f * ((1 - t) * (1 + c) * (1 + b) * d1 + (1 - t) * (1 - c) * (1 - b) * d2) * dt21;
            if ("clamped".equals(interpType) && d1 * d2 <= 0) {
                m = 0f;
            }
        } else {
            float t = next.getTension();
            float c = next.getContinuity();
            float b = next.getBias();
            m = 0.5f * ((1 - t) * (1 - c) * (1 + b) * d2 + (1 - t) * (1 + c) * (1 - b) * d3) * dt21;
            if ("clamped".equals(interpType) && d2 * d3 <= 0) {
                m = 0f;
            }
        }
        return m;
    }

    private static float interpolateWaypoints(List<Waypoint> wps, float time, float fps, float def, boolean isX) {
        if (wps == null || wps.isEmpty()) return def;

        int[] cursor = cursorFor(wps);
        int startIdx = resolveStartIndex(wps, cursor, time, fps);

        Waypoint prev = null;
        Waypoint next = null;
        int prevIdx = -1;

        for (int i = startIdx; i < wps.size(); i++) {
            Waypoint wp = wps.get(i);
            float wpTime = SifTimeUtils.parseTime(wp.getTime(), fps);
            if (wpTime <= time) {
                prev = wp;
                prevIdx = i;
            } else {
                next = wp;
                break;
            }
        }

        cursor[0] = Math.max(prevIdx, 0);

        if (prev == null) return extractVal(next.getValue(), def, isX);
        if (next == null) return extractVal(prev.getValue(), def, isX);

        float t0 = SifTimeUtils.parseTime(prev.getTime(), fps);
        float t1 = SifTimeUtils.parseTime(next.getTime(), fps);
        float duration = t1 - t0;

        if (duration <= 0) return extractVal(prev.getValue(), def, isX);

        float progress = (time - t0) / duration;
        float v0 = extractVal(prev.getValue(), def, isX);
        float v1 = extractVal(next.getValue(), def, isX);

        if ("constant".equals(prev.getAfter()) || "constant".equals(next.getBefore())) {
            return progress < 1.0f ? v0 : v1;
        }

        float m1 = getTangent(wps, prevIdx, prevIdx + 1, fps, isX, true);
        float m2 = getTangent(wps, prevIdx, prevIdx + 1, fps, isX, false);

        return hermite(v0, v1, m1, m2, progress);
    }

    private static float[] interpolateWaypointsXY(List<Waypoint> wps, float time, float fps, float defX, float defY) {
        if (wps == null || wps.isEmpty()) return new float[]{defX, defY};

        int[] cursor = cursorFor(wps);
        int startIdx = resolveStartIndex(wps, cursor, time, fps);

        Waypoint prev = null;
        Waypoint next = null;
        int prevIdx = -1;

        for (int i = startIdx; i < wps.size(); i++) {
            Waypoint wp = wps.get(i);
            float wpTime = SifTimeUtils.parseTime(wp.getTime(), fps);
            if (wpTime <= time) {
                prev = wp;
                prevIdx = i;
            } else {
                next = wp;
                break;
            }
        }

        cursor[0] = Math.max(prevIdx, 0);

        if (prev == null) {
            return new float[]{extractVal(next.getValue(), defX, true), extractVal(next.getValue(), defY, false)};
        }
        if (next == null) {
            return new float[]{extractVal(prev.getValue(), defX, true), extractVal(prev.getValue(), defY, false)};
        }

        float t0 = SifTimeUtils.parseTime(prev.getTime(), fps);
        float t1 = SifTimeUtils.parseTime(next.getTime(), fps);
        float duration = t1 - t0;

        if (duration <= 0) {
            return new float[]{extractVal(prev.getValue(), defX, true), extractVal(prev.getValue(), defY, false)};
        }

        float progress = (time - t0) / duration;
        float x0 = extractVal(prev.getValue(), defX, true);
        float x1 = extractVal(next.getValue(), defX, true);
        float y0 = extractVal(prev.getValue(), defY, false);
        float y1 = extractVal(next.getValue(), defY, false);

        if ("constant".equals(prev.getAfter()) || "constant".equals(next.getBefore())) {
            if (progress < 1.0f) {
                return new float[]{x0, y0};
            } else {
                return new float[]{x1, y1};
            }
        }

        float mx1 = getTangent(wps, prevIdx, prevIdx + 1, fps, true, true);
        float mx2 = getTangent(wps, prevIdx, prevIdx + 1, fps, true, false);
        float my1 = getTangent(wps, prevIdx, prevIdx + 1, fps, false, true);
        float my2 = getTangent(wps, prevIdx, prevIdx + 1, fps, false, false);

        return new float[]{
            hermite(x0, x1, mx1, mx2, progress),
            hermite(y0, y1, my1, my2, progress)
        };
    }

    private static float extractVal(ValueNode val, float def, boolean isX) {
        if (val == null) return def;
        if (val.getDoubleValue() != null) return val.getDoubleValue().floatValue();
        if (isX && val.getX() != null) return val.getX().floatValue();
        if (!isX && val.getY() != null) return val.getY().floatValue();
        return def;
    }

    private static float extractDoubleAsFloat(ValueNode node, float def) {
        return node.getDoubleValue() != null ? node.getDoubleValue().floatValue() : def;
    }

    public static float getMaxTime(ValueNode node, float fps) {
        float max = 0f;
        if (node.isAnimated() && node.getWaypoints() != null) {
            for (Waypoint wp : node.getWaypoints()) {
                max = Math.max(max, SifTimeUtils.parseTime(wp.getTime(), fps));
            }
        }
        if (node.getNamedChild("offset") != null) max = Math.max(max, getMaxTime(node.getNamedChild("offset"), fps));
        if (node.getNamedChild("angle") != null) max = Math.max(max, getMaxTime(node.getNamedChild("angle"), fps));
        if (node.getNamedChild("scale") != null) max = Math.max(max, getMaxTime(node.getNamedChild("scale"), fps));

        return max;
    }
}
