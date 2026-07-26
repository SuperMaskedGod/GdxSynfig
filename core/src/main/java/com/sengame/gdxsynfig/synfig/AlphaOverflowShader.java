package com.sengame.gdxsynfig.synfig;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public final class AlphaOverflowShader {
    private static final String VERTEX =
        "attribute vec4 a_position;\n" +
        "attribute vec4 a_color;\n" +
        "attribute vec2 a_texCoord0;\n" +
        "uniform mat4 u_projTrans;\n" +
        "varying vec4 v_color;\n" +
        "varying vec2 v_texCoords;\n" +
        "void main() {\n" +
        "    v_color = a_color;\n" +
        "    v_texCoords = a_texCoord0;\n" +
        "    gl_Position = u_projTrans * a_position;\n" +
        "}";

    private static final String FRAGMENT =
        "#ifdef GL_ES\n" +
        "precision mediump float;\n" +
        "#endif\n" +
        "varying vec4 v_color;\n" +
        "varying vec2 v_texCoords;\n" +
        "uniform sampler2D u_texture;\n" +
        "uniform float u_overflow;\n" +
        "void main() {\n" +
        "    vec4 texColor = texture2D(u_texture, v_texCoords);\n" +
        "    float a = texColor.a * v_color.a * u_overflow;\n" +
        "    gl_FragColor = vec4(texColor.rgb * v_color.rgb * a, min(a, 1.0));\n" +
        "}";

    private static ShaderProgram shader;
    private static float lastAmount = -1f;

    private AlphaOverflowShader() {
    }

    private static ShaderProgram shader() {
        if (shader == null) {
            shader = new ShaderProgram(VERTEX, FRAGMENT);
            if (!shader.isCompiled()) {
                throw new IllegalStateException("AlphaOverflowShader failed to compile: " + shader.getLog());
            }
        }
        return shader;
    }

    public static void begin(SpriteBatch batch, float amount) {
        batch.setShader(shader());
        if (amount != lastAmount) {
            shader().setUniformf("u_overflow", amount);
            lastAmount = amount;
        }
        batch.setBlendFunctionSeparate(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    public static void end(Batch batch) {
        batch.setShader(null);
        batch.setBlendFunctionSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    public static void dispose() {
        if (shader != null) {
            shader.dispose();
            shader = null;
            lastAmount = -1f;
        }
    }
}
