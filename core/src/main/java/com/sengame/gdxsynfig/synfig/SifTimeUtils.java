package com.sengame.gdxsynfig.synfig;

public class SifTimeUtils {
    public static float parseTime(String timeStr, float fps) {
        if (timeStr == null || timeStr.isEmpty()) return 0f;
        timeStr = timeStr.trim();

        try {
            if (timeStr.endsWith("s")) {
                return Float.parseFloat(timeStr.replace("s", ""));
            } else if (timeStr.endsWith("f")) {
                return Float.parseFloat(timeStr.replace("f", "")) / fps;
            }
            return Float.parseFloat(timeStr);
        } catch (NumberFormatException e) {
            return 0f;
        }
    }
}
