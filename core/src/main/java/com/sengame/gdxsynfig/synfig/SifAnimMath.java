package com.sengame.gdxsynfig.synfig;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class SifAnimMath {
    private SifAnimMath() {
    }

    public interface ValueExtractor {
        float extract(ValueNode value, float def);
    }

    public static final ValueExtractor DOUBLE = (v, def) -> v.getDoubleValue() != null ? v.getDoubleValue().floatValue() : def;
    public static final ValueExtractor X = (v, def) -> v.getDoubleValue() != null ? v.getDoubleValue().floatValue() : (v.getX() != null ? v.getX().floatValue() : def);
    public static final ValueExtractor Y = (v, def) -> v.getDoubleValue() != null ? v.getDoubleValue().floatValue() : (v.getY() != null ? v.getY().floatValue() : def);
    public static final ValueExtractor R = (v, def) -> v.getR() != null ? v.getR().floatValue() : def;
    public static final ValueExtractor G = (v, def) -> v.getG() != null ? v.getG().floatValue() : def;
    public static final ValueExtractor B = (v, def) -> v.getB() != null ? v.getB().floatValue() : def;
    public static final ValueExtractor A = (v, def) -> v.getA() != null ? v.getA().floatValue() : def;

    private static final Map<List<Waypoint>, int[]> cursorCache = new WeakHashMap<>();

    private static int[] cursorFor(List<Waypoint> wps) {
        return cursorCache.computeIfAbsent(wps, k -> new int[]{0});
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

    private static float getTangent(List<Waypoint> wps, int prevIdx, int nextIdx, float fps, ValueExtractor extractor, boolean isOut) {
        Waypoint cur = wps.get(prevIdx);
        Waypoint next = wps.get(nextIdx);

        String interpType = isOut ? cur.getAfter() : next.getBefore();
        if (interpType == null) interpType = "clamped";

        if ("linear".equals(interpType)) {
            return extractor.extract(next.getValue(), 0f) - extractor.extract(cur.getValue(), 0f);
        } else if ("ease".equals(interpType) || "ease-in".equals(interpType) || "ease-out".equals(interpType) || "ease-in-out".equals(interpType) || "halt".equals(interpType)) {
            return 0f;
        }

        float p1 = extractor.extract(cur.getValue(), 0f);
        float p2 = extractor.extract(next.getValue(), 0f);
        float t1 = SifTimeUtils.parseTime(cur.getTime(), fps);
        float t2 = SifTimeUtils.parseTime(next.getTime(), fps);

        int p0Idx = Math.max(0, prevIdx - 1);
        int p3Idx = Math.min(wps.size() - 1, nextIdx + 1);

        Waypoint prev = wps.get(p0Idx);
        Waypoint post = wps.get(p3Idx);

        float p0 = extractor.extract(prev.getValue(), 0f);
        float p3 = extractor.extract(post.getValue(), 0f);
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

    public static float interpolate(List<Waypoint> wps, float time, float fps, float def, ValueExtractor extractor) {
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

        if (prev == null) {
            assert next != null;
            return extractor.extract(next.getValue(), def);
        }
        if (next == null) return extractor.extract(prev.getValue(), def);

        float t0 = SifTimeUtils.parseTime(prev.getTime(), fps);
        float t1 = SifTimeUtils.parseTime(next.getTime(), fps);
        float duration = t1 - t0;

        if (duration <= 0) return extractor.extract(prev.getValue(), def);

        float progress = (time - t0) / duration;
        float v0 = extractor.extract(prev.getValue(), def);
        float v1 = extractor.extract(next.getValue(), def);

        if ("constant".equals(prev.getAfter()) || "constant".equals(next.getBefore())) {
            return progress < 1.0f ? v0 : v1;
        }

        float m1 = getTangent(wps, prevIdx, prevIdx + 1, fps, extractor, true);
        float m2 = getTangent(wps, prevIdx, prevIdx + 1, fps, extractor, false);

        return hermite(v0, v1, m1, m2, progress);
    }

    public static float evaluateScalar(ValueNode node, float time, float fps, float def, ValueExtractor extractor) {
        if (node == null) return def;
        if (!node.isAnimated()) return extractor.extract(node, def);
        return interpolate(node.getWaypoints(), time, fps, def, extractor);
    }

    public static float evaluateScalar(ValueNode node, float time, float fps, float def) {
        return evaluateScalar(node, time, fps, def, DOUBLE);
    }

    public static float[] evaluateXY(ValueNode node, float time, float fps, float defX, float defY) {
        if (node == null) return new float[]{defX, defY};
        if (!node.isAnimated()) {
            float x = (node.getX() != null) ? node.getX().floatValue() : defX;
            float y = (node.getY() != null) ? node.getY().floatValue() : defY;
            return new float[]{x, y};
        }
        return new float[]{
            interpolate(node.getWaypoints(), time, fps, defX, X),
            interpolate(node.getWaypoints(), time, fps, defY, Y)
        };
    }

    public static float[] evaluateColor(ValueNode node, float time, float fps, float defR, float defG, float defB, float defA) {
        if (node == null) return new float[]{defR, defG, defB, defA};
        if (!node.isAnimated()) {
            float r = node.getR() != null ? node.getR().floatValue() : defR;
            float g = node.getG() != null ? node.getG().floatValue() : defG;
            float b = node.getB() != null ? node.getB().floatValue() : defB;
            float a = node.getA() != null ? node.getA().floatValue() : defA;
            return new float[]{r, g, b, a};
        }
        return new float[]{
            interpolate(node.getWaypoints(), time, fps, defR, R),
            interpolate(node.getWaypoints(), time, fps, defG, G),
            interpolate(node.getWaypoints(), time, fps, defB, B),
            interpolate(node.getWaypoints(), time, fps, defA, A)
        };
    }

    public static float getMaxTime(ValueNode node, float fps) {
        if (node == null) return 0f;
        float max = 0f;
        if (node.isAnimated() && node.getWaypoints() != null) {
            for (Waypoint wp : node.getWaypoints()) {
                max = Math.max(max, SifTimeUtils.parseTime(wp.getTime(), fps));
            }
        }
        for (ValueNode child : node.getNamedChildren().values()) {
            max = Math.max(max, getMaxTime(child, fps));
        }
        return max;
    }
}
