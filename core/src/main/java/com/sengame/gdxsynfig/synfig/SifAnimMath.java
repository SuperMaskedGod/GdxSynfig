package com.sengame.gdxsynfig.synfig;

import java.util.List;

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

    private static float timeOf(Waypoint wp, float fps) {
        return wp.getTimeSeconds(fps);
    }

    private static float valueOf(Waypoint wp, float def, ValueExtractor extractor) {
        ValueNode v = wp.getValue();
        return v == null ? def : extractor.extract(v, def);
    }

    private static boolean fits(List<Waypoint> wps, int i, float time, float fps) {
        int n = wps.size();
        if (i < -1 || i >= n) return false;
        if (i >= 0 && timeOf(wps.get(i), fps) > time) return false;
        return i + 1 >= n || timeOf(wps.get(i + 1), fps) > time;
    }

    private static int search(List<Waypoint> wps, float time, float fps) {
        int lo = 0;
        int hi = wps.size() - 1;
        int ans = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (timeOf(wps.get(mid), fps) <= time) {
                ans = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return ans;
    }

    public static int findPrevWaypoint(ValueNode node, float time, float fps) {
        List<Waypoint> wps = node.getWaypoints();
        if (wps == null || wps.isEmpty()) return -1;
        int c = node.cursor;
        if (fits(wps, c, time, fps)) return c;
        if (fits(wps, c + 1, time, fps)) {
            node.cursor = c + 1;
            return c + 1;
        }
        if (fits(wps, c - 1, time, fps)) {
            node.cursor = c - 1;
            return c - 1;
        }
        int idx = search(wps, time, fps);
        node.cursor = Math.max(idx, 0);
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
            return valueOf(next, 0f, extractor) - valueOf(cur, 0f, extractor);
        } else if ("ease".equals(interpType) || "ease-in".equals(interpType) || "ease-out".equals(interpType) || "ease-in-out".equals(interpType) || "halt".equals(interpType)) {
            return 0f;
        }

        float p1 = valueOf(cur, 0f, extractor);
        float p2 = valueOf(next, 0f, extractor);
        float t1 = timeOf(cur, fps);
        float t2 = timeOf(next, fps);

        int p0Idx = Math.max(0, prevIdx - 1);
        int p3Idx = Math.min(wps.size() - 1, nextIdx + 1);

        Waypoint prev = wps.get(p0Idx);
        Waypoint post = wps.get(p3Idx);

        float p0 = valueOf(prev, 0f, extractor);
        float p3 = valueOf(post, 0f, extractor);
        float t0 = timeOf(prev, fps);
        float t3 = timeOf(post, fps);

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

    private static float interpolateAt(List<Waypoint> wps, int prevIdx, float time, float fps, float def, ValueExtractor extractor) {
        int size = wps.size();
        if (prevIdx < 0) return valueOf(wps.get(0), def, extractor);

        Waypoint prev = wps.get(prevIdx);
        if (prevIdx + 1 >= size) return valueOf(prev, def, extractor);
        Waypoint next = wps.get(prevIdx + 1);

        float t0 = timeOf(prev, fps);
        float t1 = timeOf(next, fps);
        float duration = t1 - t0;
        float v0 = valueOf(prev, def, extractor);
        if (duration <= 0) return v0;

        float progress = (time - t0) / duration;
        float v1 = valueOf(next, def, extractor);

        if ("constant".equals(prev.getAfter()) || "constant".equals(next.getBefore())) {
            return progress < 1.0f ? v0 : v1;
        }

        float m1 = getTangent(wps, prevIdx, prevIdx + 1, fps, extractor, true);
        float m2 = getTangent(wps, prevIdx, prevIdx + 1, fps, extractor, false);
        return hermite(v0, v1, m1, m2, progress);
    }

    public static float interpolate(List<Waypoint> wps, float time, float fps, float def, ValueExtractor extractor) {
        if (wps == null || wps.isEmpty()) return def;
        return interpolateAt(wps, search(wps, time, fps), time, fps, def, extractor);
    }

    private static float interpolate(ValueNode node, float time, float fps, float def, ValueExtractor extractor) {
        List<Waypoint> wps = node.getWaypoints();
        if (wps == null || wps.isEmpty()) return def;
        return interpolateAt(wps, findPrevWaypoint(node, time, fps), time, fps, def, extractor);
    }

    public static float evaluateScalar(ValueNode node, float time, float fps, float def, ValueExtractor extractor) {
        if (node == null) return def;
        if (!node.isAnimated()) return extractor.extract(node, def);
        return interpolate(node, time, fps, def, extractor);
    }

    public static float evaluateScalar(ValueNode node, float time, float fps, float def) {
        return evaluateScalar(node, time, fps, def, DOUBLE);
    }

    public static boolean evaluateBoolean(ValueNode node, float time, float fps, boolean def) {
        if (node == null) return def;
        if (!node.isAnimated()) {
            Boolean b = node.getBooleanValue();
            return b != null ? b : def;
        }
        List<Waypoint> wps = node.getWaypoints();
        if (wps == null || wps.isEmpty()) return def;

        int idx = findPrevWaypoint(node, time, fps);
        Waypoint chosen = wps.get(Math.max(idx, 0));
        if (chosen.getValue() == null) return def;
        Boolean b = chosen.getValue().getBooleanValue();
        return b != null ? b : def;
    }

    public static void evaluateXYInto(ValueNode node, float time, float fps, float defX, float defY, float[] out) {
        if (node == null) {
            out[0] = defX;
            out[1] = defY;
            return;
        }
        if (!node.isAnimated()) {
            out[0] = (node.getX() != null) ? node.getX().floatValue() : defX;
            out[1] = (node.getY() != null) ? node.getY().floatValue() : defY;
            return;
        }
        List<Waypoint> wps = node.getWaypoints();
        if (wps == null || wps.isEmpty()) {
            out[0] = defX;
            out[1] = defY;
            return;
        }
        int idx = findPrevWaypoint(node, time, fps);
        out[0] = interpolateAt(wps, idx, time, fps, defX, X);
        out[1] = interpolateAt(wps, idx, time, fps, defY, Y);
    }

    public static void evaluateColorInto(ValueNode node, float time, float fps, float defR, float defG, float defB, float defA, float[] out) {
        if (node == null) {
            out[0] = defR;
            out[1] = defG;
            out[2] = defB;
            out[3] = defA;
            return;
        }
        if (!node.isAnimated()) {
            out[0] = node.getR() != null ? node.getR().floatValue() : defR;
            out[1] = node.getG() != null ? node.getG().floatValue() : defG;
            out[2] = node.getB() != null ? node.getB().floatValue() : defB;
            out[3] = node.getA() != null ? node.getA().floatValue() : defA;
            return;
        }
        List<Waypoint> wps = node.getWaypoints();
        if (wps == null || wps.isEmpty()) {
            out[0] = defR;
            out[1] = defG;
            out[2] = defB;
            out[3] = defA;
            return;
        }
        int idx = findPrevWaypoint(node, time, fps);
        out[0] = interpolateAt(wps, idx, time, fps, defR, R);
        out[1] = interpolateAt(wps, idx, time, fps, defG, G);
        out[2] = interpolateAt(wps, idx, time, fps, defB, B);
        out[3] = interpolateAt(wps, idx, time, fps, defA, A);
    }

    public static float[] evaluateXY(ValueNode node, float time, float fps, float defX, float defY) {
        float[] out = new float[2];
        evaluateXYInto(node, time, fps, defX, defY, out);
        return out;
    }

    public static float[] evaluateColor(ValueNode node, float time, float fps, float defR, float defG, float defB, float defA) {
        float[] out = new float[4];
        evaluateColorInto(node, time, fps, defR, defG, defB, defA, out);
        return out;
    }

    public static float getMaxTime(ValueNode node, float fps) {
        if (node == null) return 0f;
        float max = 0f;
        if (node.isAnimated() && node.getWaypoints() != null) {
            for (Waypoint wp : node.getWaypoints()) {
                max = Math.max(max, wp.getTimeSeconds(fps));
            }
        }
        for (ValueNode child : node.getNamedChildren().values()) {
            max = Math.max(max, getMaxTime(child, fps));
        }
        return max;
    }
}
