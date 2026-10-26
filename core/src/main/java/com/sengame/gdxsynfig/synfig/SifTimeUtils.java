package com.sengame.gdxsynfig.synfig;

public class SifTimeUtils {

    public static float parseTime(String timeStr, float fps) {
        if (timeStr == null) return 0f;
        String s = timeStr.trim();
        if (s.isEmpty()) return 0f;

        try {

            char last = s.charAt(s.length() - 1);
            if ((last >= '0' && last <= '9' || last == '.') && s.indexOf(' ') < 0) {
                return Float.parseFloat(s);
            }

            float total = 0f;
            int i = 0;
            int n = s.length();
            while (i < n) {
                while (i < n && Character.isWhitespace(s.charAt(i))) i++;
                if (i >= n) break;
                int start = i;
                while (i < n && !Character.isWhitespace(s.charAt(i))) i++;
                String token = s.substring(start, i);

                char unit = token.charAt(token.length() - 1);
                String number = token;
                float scale = 1f;
                switch (unit) {
                    case 'h':
                        number = token.substring(0, token.length() - 1);
                        scale = 3600f;
                        break;
                    case 'm':
                        number = token.substring(0, token.length() - 1);
                        scale = 60f;
                        break;
                    case 's':
                        number = token.substring(0, token.length() - 1);
                        break;
                    case 'f':
                        number = token.substring(0, token.length() - 1);
                        scale = fps > 0f ? 1f / fps : 0f;
                        break;
                    default:
                        break;
                }
                total += Float.parseFloat(number) * scale;
            }
            return total;
        } catch (NumberFormatException e) {
            return 0f;
        }
    }
}
