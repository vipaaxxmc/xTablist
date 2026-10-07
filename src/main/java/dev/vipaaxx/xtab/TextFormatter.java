package dev.vipaaxx.xtab;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Supports: &amp; color/format codes, &amp;#RRGGBB hex, and
 * [gradient=#RRGGBB#RRGGBB(...)]text[/gradient] (2+ colors).
 */
final class TextFormatter {

    private static final Pattern GRADIENT =
            Pattern.compile("\\[gradient=([^\\]]*)\\](.*?)\\[/gradient\\]", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern HEX = Pattern.compile("#?([0-9A-Fa-f]{6})");
    private static final String CODES = "0123456789abcdefklmnorABCDEFKLMNOR";

    private TextFormatter() {}

    static String format(String input) {
        return toLegacy(applyGradients(input));
    }

    /** Converts &c, &l, &#RRGGBB into section-sign codes (hex as §x§R§R§G§G§B§B). */
    private static String toLegacy(String s) {
        StringBuilder out = new StringBuilder(s.length() * 2);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '&' && i + 7 < s.length() && s.charAt(i + 1) == '#' && isHex(s, i + 2, 6)) {
                out.append("\u00a7x");
                for (int k = 2; k < 8; k++) out.append('\u00a7').append(s.charAt(i + k));
                i += 7;
            } else if (isCode(s, i)) {
                out.append('\u00a7').append(s.charAt(i + 1));
                i++;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static boolean isHex(String s, int start, int len) {
        if (start + len > s.length()) return false;
        for (int i = start; i < start + len; i++) {
            if (Character.digit(s.charAt(i), 16) < 0) return false;
        }
        return true;
    }

    private static String applyGradients(String input) {
        Matcher m = GRADIENT.matcher(input);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(out, Matcher.quoteReplacement(buildGradient(m.group(1), m.group(2))));
        }
        m.appendTail(out);
        return out.toString();
    }

    private static String buildGradient(String colorArg, String text) {
        List<int[]> colors = new ArrayList<>();
        Matcher hm = HEX.matcher(colorArg);
        while (hm.find()) {
            int rgb = Integer.parseInt(hm.group(1), 16);
            colors.add(new int[]{(rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF});
        }
        if (colors.isEmpty()) return text;
        if (colors.size() == 1) colors.add(colors.get(0));

        // count visible characters (skip & codes)
        int visible = 0;
        for (int i = 0; i < text.length(); i++) {
            if (isCode(text, i)) { i++; continue; }
            visible++;
        }

        StringBuilder sb = new StringBuilder();
        StringBuilder formats = new StringBuilder();
        int index = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (isCode(text, i)) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                if (code == 'r') formats.setLength(0);
                else if (code >= 'k' && code <= 'o' && formats.indexOf("&" + code) < 0) formats.append('&').append(code);
                i++; // other color codes are ignored inside a gradient
                continue;
            }
            double t = visible <= 1 ? 0 : (double) index / (visible - 1);
            sb.append("&#").append(colorAt(colors, t)).append(formats).append(c);
            index++;
        }
        return sb.toString();
    }

    private static boolean isCode(String s, int i) {
        return s.charAt(i) == '&' && i + 1 < s.length() && CODES.indexOf(s.charAt(i + 1)) >= 0;
    }

    private static String colorAt(List<int[]> colors, double t) {
        int segments = colors.size() - 1;
        double pos = t * segments;
        int idx = Math.min((int) pos, segments - 1);
        double local = pos - idx;
        int[] a = colors.get(idx), b = colors.get(idx + 1);
        int r = (int) Math.round(a[0] + (b[0] - a[0]) * local);
        int g = (int) Math.round(a[1] + (b[1] - a[1]) * local);
        int bl = (int) Math.round(a[2] + (b[2] - a[2]) * local);
        return String.format("%02x%02x%02x", r, g, bl);
    }
}
