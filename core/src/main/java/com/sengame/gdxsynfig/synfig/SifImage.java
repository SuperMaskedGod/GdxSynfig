package com.sengame.gdxsynfig.synfig;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;

public class SifImage extends Image {
    private final Param amountParam;
    private final Param transformParam;
    private final float scaleFactor;
    private final float canvasCenterX;
    private final float canvasCenterY;
    private float trueAmount = 1f;

    private static final ThreadLocal<float[]> SCRATCH_XY = new ThreadLocal<float[]>() {
        @Override
        protected float[] initialValue() {
            return new float[2];
        }
    };

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

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (trueAmount > 1f && batch instanceof SpriteBatch) {
            SpriteBatch spriteBatch = (SpriteBatch) batch;
            AlphaOverflowShader.begin(spriteBatch, trueAmount);
            super.draw(batch, parentAlpha);
            AlphaOverflowShader.end(batch);
        } else {
            super.draw(batch, parentAlpha);
        }
    }

    public static void applyState(Actor target, float time, float fps, Param amountParam, Param transformParam, float scaleFactor, float cx, float cy) {
        float alpha = 1f;
        if (amountParam != null && amountParam.getValue() != null) {
            alpha = SifAnimMath.evaluateScalar(amountParam.getValue(), time, fps, 1f);
        }
        if (target instanceof SifImage) {
            ((SifImage) target).trueAmount = alpha;
        } else if (target instanceof SifAnimation) {
            ((SifAnimation) target).trueAmount = alpha;
        }
        target.getColor().a = Math.max(0f, Math.min(1f, alpha));

        if (transformParam == null || transformParam.getValue() == null) {
            target.setPosition(cx - target.getOriginX(), cy - target.getOriginY());
            return;
        }

        ValueNode tNode = transformParam.getValue();
        float[] xy = SCRATCH_XY.get();

        ValueNode offsetNode = tNode.getNamedChild("offset");
        float x = cx - target.getOriginX();
        float y = cy - target.getOriginY();
        if (offsetNode != null) {
            SifAnimMath.evaluateXYInto(offsetNode, time, fps, 0f, 0f, xy);
            x += xy[0] * scaleFactor;
            y += xy[1] * scaleFactor;
        }
        target.setPosition(x, y);

        ValueNode angleNode = tNode.getNamedChild("angle");
        float angle = (angleNode != null) ? SifAnimMath.evaluateScalar(angleNode, time, fps, 0f) : 0f;
        target.setRotation(angle);

        ValueNode scaleNode = tNode.getNamedChild("scale");
        float scaleX = 1f;
        float scaleY = 1f;
        if (scaleNode != null) {
            SifAnimMath.evaluateXYInto(scaleNode, time, fps, 1f, 1f, xy);
            scaleX = xy[0];
            scaleY = xy[1];
        }
        target.setScale(scaleX, scaleY);
    }

    public static float getMaxTime(ValueNode node, float fps) {
        return SifAnimMath.getMaxTime(node, fps);
    }
}
