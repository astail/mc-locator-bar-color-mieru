package io.github.astail.locatorcolors;

import java.util.UUID;

/**
 * ロケーターバー（Locator Bar）のドット色を、クライアントとまったく同じ計算でサーバー側に再現する純粋ロジック。
 *
 * <p>Minecraft 26.1.2 のクライアント（{@code net.minecraft.client.gui.contextualbar.LocatorBarRenderer}）は、
 * ウェイポイントのアイコン色が未設定のとき、対象の UUID から既定色を決める:
 * <pre>{@code ARGB.setBrightness(ARGB.color(255, uuid.hashCode()), 0.9F)}</pre>
 *
 * <p>本クラスは {@code net.minecraft.util.ARGB} の該当メソッドを<b>1対1で移植</b>したものであり、
 * NMS（サーバー内部クラス）には一切触れずに同じ色を得る。{@link UUID#hashCode()} は JDK 標準で
 * サーバー・クライアントで同一に評価されるため、結果はクライアント表示と完全に一致する。
 *
 * <p>計算結果がバニラとずれないよう、ここでの演算順序・float/int の扱い・丸め（{@link Math#round})は
 * 元実装から変更しないこと。
 */
final class WaypointColor {

    /** クライアントが既定色に使う明度（HSV の V）。バニラ準拠の固定値。 */
    private static final float DEFAULT_BRIGHTNESS = 0.9F;

    private WaypointColor() {
    }

    /**
     * チーム未所属プレイヤーの既定ドット色を 0xRRGGBB（24bit）で返す。
     * UUID から一意に決まるため毎回同じ値になり、ロケーターバーのドット色と一致する。
     */
    static int defaultRgb(UUID id) {
        // ARGB.color(255, uuid.hashCode()) … 下位 24bit を RGB、α=255 とする。
        int seed = colorFromAlphaAndRgb(255, id.hashCode());
        // ARGB.setBrightness(seed, 0.9F) … 色相・彩度を保ったまま明度を 0.9 にする。
        return setBrightness(seed, DEFAULT_BRIGHTNESS) & 0xFFFFFF;
    }

    // ---- 以下、net.minecraft.util.ARGB の忠実移植（数値計算は改変禁止） ----

    /** {@code ARGB.color(int alpha, int rgb)} 相当。 */
    private static int colorFromAlphaAndRgb(int alpha, int rgb) {
        return alpha << 24 | rgb & 0xFFFFFF;
    }

    /** {@code ARGB.color(int alpha, int red, int green, int blue)} 相当。 */
    private static int color(int alpha, int red, int green, int blue) {
        return (alpha & 0xFF) << 24 | (red & 0xFF) << 16 | (green & 0xFF) << 8 | blue & 0xFF;
    }

    private static int alpha(int color) {
        return color >>> 24;
    }

    private static int red(int color) {
        return color >> 16 & 0xFF;
    }

    private static int green(int color) {
        return color >> 8 & 0xFF;
    }

    private static int blue(int color) {
        return color & 0xFF;
    }

    /**
     * {@code ARGB.setBrightness(int color, float brightness)} の忠実移植。
     * 入力色の色相・彩度を保ったまま、明度（HSV の V）を {@code brightness} に設定して返す。
     */
    private static int setBrightness(int color, float brightness) {
        int red = red(color);
        int green = green(color);
        int blue = blue(color);
        int alpha = alpha(color);
        int rgbMax = Math.max(Math.max(red, green), blue);
        int rgbMin = Math.min(Math.min(red, green), blue);
        float rgbConstantRange = rgbMax - rgbMin;
        float saturation;
        if (rgbMax != 0) {
            saturation = rgbConstantRange / rgbMax;
        } else {
            saturation = 0.0F;
        }

        float hue;
        if (saturation == 0.0F) {
            hue = 0.0F;
        } else {
            float constantRed = (rgbMax - red) / rgbConstantRange;
            float constantGreen = (rgbMax - green) / rgbConstantRange;
            float constantBlue = (rgbMax - blue) / rgbConstantRange;
            if (red == rgbMax) {
                hue = constantBlue - constantGreen;
            } else if (green == rgbMax) {
                hue = 2.0F + constantRed - constantBlue;
            } else {
                hue = 4.0F + constantGreen - constantRed;
            }

            hue /= 6.0F;
            if (hue < 0.0F) {
                hue++;
            }
        }

        if (saturation == 0.0F) {
            red = green = blue = Math.round(brightness * 255.0F);
            return color(alpha, red, green, blue);
        } else {
            float colorWheelSegment = (hue - (float) Math.floor(hue)) * 6.0F;
            float colorWheelOffset = colorWheelSegment - (float) Math.floor(colorWheelSegment);
            float primaryColor = brightness * (1.0F - saturation);
            float secondaryColor = brightness * (1.0F - saturation * colorWheelOffset);
            float tertiaryColor = brightness * (1.0F - saturation * (1.0F - colorWheelOffset));
            switch ((int) colorWheelSegment) {
                case 0:
                    red = Math.round(brightness * 255.0F);
                    green = Math.round(tertiaryColor * 255.0F);
                    blue = Math.round(primaryColor * 255.0F);
                    break;
                case 1:
                    red = Math.round(secondaryColor * 255.0F);
                    green = Math.round(brightness * 255.0F);
                    blue = Math.round(primaryColor * 255.0F);
                    break;
                case 2:
                    red = Math.round(primaryColor * 255.0F);
                    green = Math.round(brightness * 255.0F);
                    blue = Math.round(tertiaryColor * 255.0F);
                    break;
                case 3:
                    red = Math.round(primaryColor * 255.0F);
                    green = Math.round(secondaryColor * 255.0F);
                    blue = Math.round(brightness * 255.0F);
                    break;
                case 4:
                    red = Math.round(tertiaryColor * 255.0F);
                    green = Math.round(primaryColor * 255.0F);
                    blue = Math.round(brightness * 255.0F);
                    break;
                case 5:
                    red = Math.round(brightness * 255.0F);
                    green = Math.round(primaryColor * 255.0F);
                    blue = Math.round(secondaryColor * 255.0F);
            }

            return color(alpha, red, green, blue);
        }
    }
}
