package io.github.astail.locatorcolors;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.logging.Logger;

import org.bukkit.entity.Player;

/**
 * {@code /waypoint modify <対象> color …} で個別設定された色（エンティティの {@code locatorBarIcon.color}）を読む。
 *
 * <p>この値は paper-api に公開されていないため、サーバー内部クラスをリフレクションで参照する。
 * 参照するのは<b>公開メンバーのみ</b>——公開メソッド {@code WaypointTransmitter#waypointIcon()} と
 * 公開フィールド {@code Waypoint.Icon#color}（mojang-mapped な Paper 実行時の実名）——なので
 * {@code setAccessible} は不要。解決・読み取りに失敗した場合は一度だけ警告して無効化し、以降は常に空を返す
 * （プラグインはチーム色／UUID 色にフォールバックして動作を続ける）。
 *
 * <p>バニラの色決定（{@code Waypoint.Icon#cloneAndAssignStyle}）では、この個別色がチーム色より<b>優先</b>される。
 */
final class WaypointIconReader {

    private final Logger logger;
    /** {@code WaypointTransmitter#waypointIcon()} … エンティティの基底アイコンを返す。 */
    private final Method waypointIconMethod;
    /** {@code Waypoint.Icon#color}（public, {@code Optional<Integer>}）。 */
    private final Field colorField;
    /** {@code CraftPlayer#getHandle()} … バージョン非依存にするため初回に解決する。 */
    private Method getHandleMethod;
    private boolean disabled;

    WaypointIconReader(Logger logger) {
        this.logger = logger;
        Method icon = null;
        Field color = null;
        try {
            icon = Class.forName("net.minecraft.world.waypoints.WaypointTransmitter").getMethod("waypointIcon");
            color = Class.forName("net.minecraft.world.waypoints.Waypoint$Icon").getField("color");
        } catch (Throwable t) {
            disable("ロケーターアイコンの参照を解決できませんでした", t);
        }
        this.waypointIconMethod = icon;
        this.colorField = color;
    }

    /** {@code /waypoint} で個別設定された色（0xRRGGBB）。未設定・取得不可なら空。 */
    OptionalInt explicitRgb(Player player) {
        if (disabled) {
            return OptionalInt.empty();
        }
        try {
            if (getHandleMethod == null) {
                getHandleMethod = player.getClass().getMethod("getHandle");
            }
            Object handle = getHandleMethod.invoke(player);  // ServerPlayer
            Object icon = waypointIconMethod.invoke(handle); // Waypoint.Icon（基底＝個別色のみ。チーム色は含まない）
            if (icon == null) {
                return OptionalInt.empty();
            }
            Object value = colorField.get(icon);             // Optional<Integer>
            if (value instanceof Optional<?> opt && opt.orElse(null) instanceof Integer rgb) {
                return OptionalInt.of(rgb & 0xFFFFFF);
            }
            return OptionalInt.empty();
        } catch (Throwable t) {
            disable("ロケーター色の読み取りに失敗しました", t);
            return OptionalInt.empty();
        }
    }

    /** 個別色の参照が利用可能か（無効化されていないか）。 */
    boolean isAvailable() {
        return !disabled;
    }

    private void disable(String reason, Throwable t) {
        if (!disabled) {
            disabled = true;
            logger.warning(reason + " — /waypoint の個別色は反映されません（チーム色／UUID 色で動作します）: " + t);
        }
    }
}
