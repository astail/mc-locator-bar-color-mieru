package io.github.astail.locatorcolors;

import java.util.List;
import java.util.Locale;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;

/** /locatorcolors … タブのロケーター色表示を<b>サーバー全体</b>で ON/OFF（引数なしは状態表示）する管理コマンド。 */
public final class LocatorColorsCommand implements TabExecutor {

    private static final List<String> SUBCOMMANDS = List.of("on", "off", "status");

    private final LocatorColorsPlugin plugin;

    public LocatorColorsCommand(@NotNull LocatorColorsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        // locatorcolors.admin の有無は plugin.yml の commands.locatorcolors.permission により Bukkit が事前チェック済み。
        if (args.length == 0) {
            sendStatus(sender);
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage("§e使い方: /" + label + " [on|off|status]");
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "on" -> setActive(sender, true);
            case "off" -> setActive(sender, false);
            case "status" -> sendStatus(sender);
            default -> sender.sendMessage("§e使い方: /" + label + " [on|off|status]");
        }
        return true;
    }

    private void setActive(CommandSender sender, boolean active) {
        if (plugin.isActive() == active) {
            sender.sendMessage(active ? "§aすでに ON です。" : "§7すでに OFF です。");
            return;
        }
        plugin.setActive(active); // 全員のタブ名を即座に付け直す／元へ戻す
        sender.sendMessage(active
                ? "§aタブのロケーター色表示を ON にしました。"
                : "§7タブのロケーター色表示を OFF にしました。元の名前に戻しました。");
    }

    private void sendStatus(CommandSender sender) {
        sender.sendMessage(plugin.isActive()
                ? "§aタブのロケーター色表示は ON です。"
                : "§7タブのロケーター色表示は OFF です。/locatorcolors on で有効化できます。");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        // 第1引数のみ候補を返す。既定のプレイヤー名補完を抑止するため、該当なしでも空リストを返す。
        if (args.length != 1) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return SUBCOMMANDS.stream()
                .filter(s -> s.startsWith(prefix))
                .toList();
    }
}
