package io.github.astail.locatorcolors;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

/**
 * 各プレイヤーのタブ表示名（player list name）に、ロケーターバーと同じ色のドット {@code ●} と着色名を適用する。
 *
 * <p>すべての公開メソッドはメインスレッド（有効化処理・参加/退出イベント・定期タスク・コマンド）からのみ
 * 呼ばれる前提。タブ名は色が変わったときだけ送信し直す（差分更新）。
 *
 * <p>色の決め方はバニラ（{@code Waypoint.Icon#cloneAndAssignStyle} → クライアントの既定色計算）に合わせ、
 * 優先順位は <b>チーム色 →（無ければ）UUID 由来の既定色</b>。チーム色が黒（0x000000）のときは、
 * バニラがバー描画で使う代替色 {@code 0x2F2F30} に置き換える。
 */
final class TabColorizer {

    /** タブ名の先頭に付けるマーカー。ロケーターバーのドットに対応させる。 */
    private static final String MARKER = "● "; // ● ＋ 半角スペース

    /** チーム色が黒のときの代替色。バニラの -13619152(0xFF2F2F30) の RGB 部に一致。 */
    private static final int BLACK_REPLACEMENT_RGB = 0x2F2F30;

    private final LocatorColorsPlugin plugin;

    /** /waypoint で個別設定された色を読む（NMS リフレクション。取得不可なら自動で無効化）。 */
    private final WaypointIconReader iconReader;

    /** 直近に適用したタブ名（差分検出用）。同じ内容なら再送しない。 */
    private final Map<UUID, Component> applied = new HashMap<>();
    /** 初めて装飾する直前のタブ名（解除時に戻す。値が null＝バニラ既定名）。 */
    private final Map<UUID, Component> original = new HashMap<>();

    TabColorizer(LocatorColorsPlugin plugin) {
        this.plugin = plugin;
        this.iconReader = new WaypointIconReader(plugin.getLogger());
    }

    /** 全オンラインプレイヤーを現在の状態に合わせて更新する（差分があるときだけ送信）。 */
    void refresh() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            apply(player);
        }
    }

    /** 1人分のタブ名を更新する。全体 OFF・権限なしのときは元の名前へ戻す。 */
    void apply(Player player) {
        UUID id = player.getUniqueId();
        if (!plugin.isActive() || !player.hasPermission("locatorcolors.show")) {
            restore(player);
            return;
        }

        Component listName = buildListName(player);
        if (listName.equals(applied.get(id))) {
            return; // 色も付帯情報も変化なし：再送しない
        }

        // 初回だけ元の名前を退避しておく（他プラグインが設定済みなら尊重して戻せるように）。
        if (!original.containsKey(id)) {
            original.put(id, player.playerListName());
        }
        player.playerListName(listName);
        applied.put(id, listName);
    }

    /** 1人分を元の名前へ戻す。装飾していなければ何もしない。 */
    void restore(Player player) {
        UUID id = player.getUniqueId();
        applied.remove(id);
        if (original.containsKey(id)) {
            player.playerListName(original.remove(id)); // null なら既定名に戻る
        }
    }

    /** 退出プレイヤーのキャッシュを破棄する（タブからは自動で消えるため名前は戻さない）。 */
    void clear(UUID id) {
        applied.remove(id);
        original.remove(id);
    }

    /** 無効化・全体 OFF 時：オンライン全員を元の名前へ戻して状態を破棄する。 */
    void restoreAll() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            restore(player);
        }
        applied.clear();
        original.clear();
    }

    /** {@code ● 名前}（必要ならチームの接頭辞・接尾辞付き）をロケーター色で組み立てる。 */
    private Component buildListName(Player player) {
        Team team = teamOf(player);
        TextColor color = TextColor.color(resolveRgb(player, team));
        Component name = Component.text(player.getName(), color);
        Component body = team == null
                ? name
                : Component.empty().append(team.prefix()).append(name).append(team.suffix());
        return Component.text(MARKER, color).append(body);
    }

    /**
     * ロケーターバーに表示される色（0xRRGGBB）を、バニラと同じ優先順位で決める。
     * <ol>
     *   <li>{@code /waypoint modify … color} で個別設定された色（最優先）</li>
     *   <li>チーム色（黒はバーで暗すぎるため代替色 {@code 0x2F2F30} へ）</li>
     *   <li>UUID 由来の既定色</li>
     * </ol>
     */
    private int resolveRgb(Player player, Team team) {
        OptionalInt explicit = iconReader.explicitRgb(player);
        if (explicit.isPresent()) {
            return explicit.getAsInt(); // /waypoint の個別色を最優先
        }
        if (team != null && team.hasColor()) {
            int rgb = team.color().value() & 0xFFFFFF;
            return rgb == 0 ? BLACK_REPLACEMENT_RGB : rgb; // 黒はバーで暗すぎるため代替色へ
        }
        return WaypointColor.defaultRgb(player.getUniqueId());
    }

    /** バニラのバー色判定と同じく、サーバーのメインスコアボード上のチームを引く。未所属なら null。 */
    private Team teamOf(Player player) {
        ScoreboardManager manager = plugin.getServer().getScoreboardManager();
        if (manager == null) {
            return null; // 起動直後でワールド未ロード等。次の更新で再試行される。
        }
        Scoreboard main = manager.getMainScoreboard();
        return main.getEntryTeam(player.getName());
    }
}
