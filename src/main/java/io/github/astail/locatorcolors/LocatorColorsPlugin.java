package io.github.astail.locatorcolors;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * ロケーターバーに表示される各プレイヤーの色を、タブ一覧（プレイヤーリスト）の名前へ反映する本体。
 *
 * <p>タブ名（player list name）は<b>全員に共通の1つ</b>のため、表示はサーバー全体で ON/OFF する
 * （プレイヤー個別の視点切り替えはできない）。色はほぼ静的（UUID 由来は不変、チーム色のみ変化）なので、
 * 参加時に即適用し、チーム変更などの取りこぼし防止に低頻度の定期更新（差分送信）を併用する。
 */
public final class LocatorColorsPlugin extends JavaPlugin implements Listener {

    /** 定期更新の間隔（tick）。40 tick = 2 秒ごと。チーム色変更等を拾うための保険で、差分があるときだけ送信する。 */
    private static final long REFRESH_PERIOD_TICKS = 40L;

    /** タブ名の付与・解除を管理する。 */
    private final TabColorizer colorizer = new TabColorizer(this);

    /** サーバー全体の表示状態（既定 ON）。サーバー稼働中のみ保持され、再起動で ON に戻る。 */
    private boolean active = true;

    private BukkitTask refreshTask;

    @Override
    public void onEnable() {
        if (!register("locatorcolors", new LocatorColorsCommand(this))) {
            return;
        }
        // 参加/退出でタブ名の付与・後片付けを行うためのリスナー。
        getServer().getPluginManager().registerEvents(this, this);
        // /reload 等で既にオンラインのプレイヤーへ即適用（サーバー起動直後は対象なし）。
        colorizer.refresh();
        // チーム色変更などの取りこぼし防止。負荷は「人数 × 差分チェック」のみで、変化が無ければ何も送らない。
        refreshTask = getServer().getScheduler()
                .runTaskTimer(this, colorizer::refresh, REFRESH_PERIOD_TICKS, REFRESH_PERIOD_TICKS);
        getLogger().info("LocatorColors を有効化しました。タブにロケーターバーの色を表示します。");
    }

    @Override
    public void onDisable() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
        // 装飾した全員のタブ名を元へ戻す。
        colorizer.restoreAll();
    }

    /** タブのロケーター色表示が全体で有効か。 */
    public boolean isActive() {
        return active;
    }

    /** 表示全体の ON/OFF を切り替え、即座に全員へ反映する。 */
    public void setActive(boolean active) {
        if (this.active == active) {
            return;
        }
        this.active = active;
        if (active) {
            colorizer.refresh();   // 付け直す
        } else {
            colorizer.restoreAll(); // 元へ戻す
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // 参加者のタブ名を即装飾（他プレイヤーの色は不変なので本人だけでよい）。
        colorizer.apply(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // 退出者のキャッシュを破棄（タブからは自動的に消える）。
        colorizer.clear(event.getPlayer().getUniqueId());
    }

    private boolean register(String name, CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().severe("コマンド '" + name + "' が plugin.yml に未定義です。プラグインを無効化します。");
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }
        command.setExecutor(executor);
        if (executor instanceof TabCompleter completer) {
            command.setTabCompleter(completer);
        }
        return true;
    }
}
