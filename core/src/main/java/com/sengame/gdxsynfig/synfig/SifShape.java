package com.sengame.gdxsynfig.synfig;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.ShortArray;

public class SifShape extends Actor {
    private static ShapeRenderer sharedRenderer;
    private final String shapeType;
    private final Param colorParam;
    private final Param amountParam;
    private final Param originParam;
    private final Param radius1Param;
    private final Param radius2Param;
    private final Param angleParam;
    private final Param pointsParam;
    private final Param regularPolygonParam;
    private final Param radiusParam;
    private final Param point1Param;
    private final Param point2Param;
    private final float scaleFactor;
    private final float canvasCenterX;
    private final float canvasCenterY;
    private final EarClippingTriangulator triangulator = new EarClippingTriangulator();
    private final Color currentColor = new Color(1f, 1f, 1f, 1f);
    private float currentAlpha = 1f;
    private float[] currentVertices = new float[0];
    private float currentX;
    private float currentY;
    private float currentWidth;
    private float currentHeight;
    private float currentRadius;

    public SifShape(Layer layer, float scaleFactor, float canvasCenterX, float canvasCenterY) {
        this.shapeType = layer.getType();
        this.colorParam = layer.getParam("color");
        this.amountParam = layer.getParam("amount");
        this.originParam = layer.getParam("origin");
        this.radius1Param = layer.getParam("radius1");
        this.radius2Param = layer.getParam("radius2");
        this.angleParam = layer.getParam("angle");
        this.pointsParam = layer.getParam("points");
        this.regularPolygonParam = layer.getParam("regular_polygon");
        this.radiusParam = layer.getParam("radius");
        this.point1Param = layer.getParam("point1");
        this.point2Param = layer.getParam("point2");
        this.scaleFactor = scaleFactor;
        this.canvasCenterX = canvasCenterX;
        this.canvasCenterY = canvasCenterY;
    }

    public void updateAnimation(float time, float fps) {
        float alpha = (amountParam != null && amountParam.getValue() != null)
            ? SifAnimMath.evaluateScalar(amountParam.getValue(), time, fps, 1f) : 1f;
        currentAlpha = Math.max(0f, Math.min(1f, alpha));

        float[] col = (colorParam != null && colorParam.getValue() != null)
            ? SifAnimMath.evaluateColor(colorParam.getValue(), time, fps, 1f, 1f, 1f, 1f)
            : new float[]{1f, 1f, 1f, 1f};
        currentColor.set(col[0], col[1], col[2], col[3]);
        getColor().a = currentAlpha;

        if ("star".equals(shapeType)) {
            updateStar(time, fps);
        } else if ("circle".equals(shapeType)) {
            updateCircle(time, fps);
        } else if ("rectangle".equals(shapeType)) {
            updateRectangle(time, fps);
        }
    }

    private void updateStar(float time, float fps) {
        float[] origin = (originParam != null && originParam.getValue() != null)
            ? SifAnimMath.evaluateXY(originParam.getValue(), time, fps, 0f, 0f) : new float[]{0f, 0f};
        float r1 = (radius1Param != null && radius1Param.getValue() != null)
            ? SifAnimMath.evaluateScalar(radius1Param.getValue(), time, fps, 1f) : 1f;
        float r2 = (radius2Param != null && radius2Param.getValue() != null)
            ? SifAnimMath.evaluateScalar(radius2Param.getValue(), time, fps, 1f) : 1f;
        float angleDeg = (angleParam != null && angleParam.getValue() != null)
            ? SifAnimMath.evaluateScalar(angleParam.getValue(), time, fps, 90f) : 90f;
        int points = (pointsParam != null && pointsParam.getValue() != null)
            ? Math.round(SifAnimMath.evaluateScalar(pointsParam.getValue(), time, fps, 5f)) : 5;
        boolean regular = regularPolygonParam != null && regularPolygonParam.getValue() != null
            && Boolean.TRUE.equals(regularPolygonParam.getValue().getBooleanValue());
        if (points < 2) points = 2;

        currentX = canvasCenterX + origin[0] * scaleFactor;
        currentY = canvasCenterY + origin[1] * scaleFactor;

        int vertexCount = points * 2;
        float[] verts = new float[vertexCount * 2];
        float angleRad = (float) Math.toRadians(angleDeg);
        float step = (float) Math.PI / points;
        for (int i = 0; i < vertexCount; i++) {
            float radius = (regular || i % 2 == 0) ? r1 : r2;
            float vAngle = angleRad + i * step;
            verts[i * 2] = (float) Math.cos(vAngle) * radius * scaleFactor;
            verts[i * 2 + 1] = (float) Math.sin(vAngle) * radius * scaleFactor;
        }
        currentVertices = verts;
    }

    private void updateCircle(float time, float fps) {
        float[] origin = (originParam != null && originParam.getValue() != null)
            ? SifAnimMath.evaluateXY(originParam.getValue(), time, fps, 0f, 0f) : new float[]{0f, 0f};
        float radius = (radiusParam != null && radiusParam.getValue() != null)
            ? SifAnimMath.evaluateScalar(radiusParam.getValue(), time, fps, 1f) : 1f;

        currentX = canvasCenterX + origin[0] * scaleFactor;
        currentY = canvasCenterY + origin[1] * scaleFactor;
        currentRadius = radius * scaleFactor;
    }

    private void updateRectangle(float time, float fps) {
        float[] p1 = (point1Param != null && point1Param.getValue() != null)
            ? SifAnimMath.evaluateXY(point1Param.getValue(), time, fps, 0f, 0f) : new float[]{0f, 0f};
        float[] p2 = (point2Param != null && point2Param.getValue() != null)
            ? SifAnimMath.evaluateXY(point2Param.getValue(), time, fps, 0f, 0f) : new float[]{0f, 0f};

        float minX = Math.min(p1[0], p2[0]);
        float maxX = Math.max(p1[0], p2[0]);
        float minY = Math.min(p1[1], p2[1]);
        float maxY = Math.max(p1[1], p2[1]);

        currentX = canvasCenterX + minX * scaleFactor;
        currentY = canvasCenterY + minY * scaleFactor;
        currentWidth = (maxX - minX) * scaleFactor;
        currentHeight = (maxY - minY) * scaleFactor;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        float finalAlpha = currentColor.a * currentAlpha * parentAlpha;
        if (finalAlpha <= 0f) return;

        batch.end();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        ShapeRenderer renderer = getRenderer();
        renderer.setProjectionMatrix(batch.getProjectionMatrix());
        renderer.setTransformMatrix(batch.getTransformMatrix());
        renderer.begin(ShapeRenderer.ShapeType.Filled);
        renderer.setColor(currentColor.r, currentColor.g, currentColor.b, finalAlpha);

        if ("star".equals(shapeType)) {
            drawStar(renderer);
        } else if ("circle".equals(shapeType)) {
            int segments = Math.max(16, (int) currentRadius);
            renderer.circle(currentX, currentY, currentRadius, segments);
        } else if ("rectangle".equals(shapeType)) {
            renderer.rect(currentX, currentY, currentWidth, currentHeight);
        }

        renderer.end();
        batch.begin();
    }

    private void drawStar(ShapeRenderer renderer) {
        if (currentVertices.length < 6) return;
        ShortArray triangles = triangulator.computeTriangles(currentVertices);
        for (int i = 0; i < triangles.size; i += 3) {
            int a = triangles.get(i) * 2;
            int b = triangles.get(i + 1) * 2;
            int c = triangles.get(i + 2) * 2;
            renderer.triangle(
                currentX + currentVertices[a], currentY + currentVertices[a + 1],
                currentX + currentVertices[b], currentY + currentVertices[b + 1],
                currentX + currentVertices[c], currentY + currentVertices[c + 1]
            );
        }
    }

    private static ShapeRenderer getRenderer() {
        if (sharedRenderer == null) {
            sharedRenderer = new ShapeRenderer();
        }
        return sharedRenderer;
    }

    public static boolean isShapeLayer(String type) {
        return "star".equals(type) || "circle".equals(type) || "rectangle".equals(type);
    }

    public static float getMaxTime(Layer layer, float fps) {
        float max = 0f;
        String[] paramNames = {"amount", "color", "origin", "radius1", "radius2", "angle", "points", "radius", "point1", "point2"};
        for (String name : paramNames) {
            Param p = layer.getParam(name);
            if (p != null && p.getValue() != null) {
                max = Math.max(max, SifAnimMath.getMaxTime(p.getValue(), fps));
            }
        }
        return max;
    }
}
