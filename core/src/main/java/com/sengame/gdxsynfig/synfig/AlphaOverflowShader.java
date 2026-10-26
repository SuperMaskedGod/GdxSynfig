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

    private static int depth = 0;
    private static float currentAmount = 1f;
    private static ShaderProgram[] savedShader = new ShaderProgram[8];
    private static int[] savedBlend = new int[8 * 4];
    private static float[] savedAmount = new float[8];

    private AlphaOverflowShader() {
    }

    private static ShaderProgram shader() {
        if (shader == null) {
            shader = new ShaderProgram(VERTEX, FRAGMENT);
            if (!shader.isCompiled()) {
                String log = shader.getLog();
                shader.dispose();
                shader = null;
                throw new IllegalStateException("AlphaOverflowShader failed to compile: " + log);
            }
        }
        return shader;
    }

    private static void ensureCapacity(int d) {
        if (d < savedShader.length) return;
        int n = savedShader.length * 2;
        savedShader = java.util.Arrays.copyOf(savedShader, n);
        savedAmount = java.util.Arrays.copyOf(savedAmount, n);
        savedBlend = java.util.Arrays.copyOf(savedBlend, n * 4);
    }

    public static void begin(SpriteBatch batch, float amount) {
        int d = depth++;
        ensureCapacity(d);

        savedShader[d] = batch.getShader();
        savedAmount[d] = currentAmount;
        savedBlend[d * 4] = batch.getBlendSrcFunc();
        savedBlend[d * 4 + 1] = batch.getBlendDstFunc();
        savedBlend[d * 4 + 2] = batch.getBlendSrcFuncAlpha();
        savedBlend[d * 4 + 3] = batch.getBlendDstFuncAlpha();

        ShaderProgram s = shader();
        batch.setShader(s);
        s.setUniformf("u_overflow", amount);
        currentAmount = amount;
        batch.setBlendFunctionSeparate(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    public static void end(Batch batch) {
        if (depth <= 0) {
            depth = 0;
            return;
        }
        int d = --depth;

        ShaderProgram prev = savedShader[d];
        savedShader[d] = null;
        batch.setShader(prev);
        batch.setBlendFunctionSeparate(savedBlend[d * 4], savedBlend[d * 4 + 1], savedBlend[d * 4 + 2], savedBlend[d * 4 + 3]);

        currentAmount = savedAmount[d];
        if (prev != null && prev == shader) {

            prev.setUniformf("u_overflow", currentAmount);
        }
    }

    public static void dispose() {
        if (shader != null) {
            shader.dispose();
            shader = null;
        }
        depth = 0;
        currentAmount = 1f;
        java.util.Arrays.fill(savedShader, null);
    }
}
