# CLAUDE.md

このリポジトリで作業する Claude / 開発者向けのメモ。

## 何のプラグインか

ロケーターバー（Locator Bar）に出る各プレイヤーのドット色を、タブ一覧（player list name）の名前へ
同じ色で付ける Paper プラグイン。プレイヤーは MOD 不要（サーバー側のみ）。

## ビルド / 実行

```bash
./deploy.sh                       # JDK25 + Maven でビルド → target/LocatorColors-1.1.0.jar
JAVA_HOME=/path/to/jdk25 ./deploy.sh   # 別の JDK を使う場合
mvn -B clean package              # 直接ビルド
```

- Java **25**（`maven.compiler.release=25`）。
- 依存は `io.papermc.paper:paper-api:26.2.build.40-alpha`（`provided`）のみ。`plugin.yml` の `api-version` は `26.2`。
- サーバー更新時は paper-api の build 番号を上げるだけで追従できることが多い（例: `...build.41-alpha`）。

## アーキテクチャ（src/main/java/io/github/astail/locatorcolors/）

- `LocatorColorsPlugin` … 本体。コマンド登録、参加/退出リスナー、2 秒ごとの定期更新タスク、無効化時の復元。全体 ON/OFF 状態（`active`）を保持。
- `TabColorizer` … タブ名の付与/解除。差分更新（同じ内容なら再送しない）、初回に元の名前を退避して解除時に戻す。色の優先順位（個別色→チーム色→UUID 由来）もここ。
- `WaypointColor` … UUID → ロケーター色の純粋ロジック。Bukkit 非依存。
- `WaypointIconReader` … `/waypoint` 個別色（`locatorBarIcon.color`）を NMS リフレクションで読む。公開メンバーのみ参照し、失敗時は自動で無効化＆フォールバック。
- `LocatorColorsCommand` … `/locatorcolors [on|off|status]`（全体 ON/OFF、`locatorcolors.admin`）。

## 最重要の不変条件：色計算をバニラと一致させること

タブ色がバー色と**完全一致**することがこのプラグインの存在意義。色の決め方はバニラ準拠：

1. `/waypoint` で明示設定された `locatorBarIcon.color`（最優先）。`WaypointIconReader` が公開メソッド `WaypointTransmitter#waypointIcon()` と公開フィールド `Waypoint.Icon#color` をリフレクションで読む（mojmap 実行時の実名。取得不可なら警告して 2/3 にフォールバック）。
2. チーム色（メインスコアボード）。チーム色が黒 `0x000000` のときはバー用代替色 `0x2F2F30` に置換
3. UUID 由来の既定色 = `ARGB.setBrightness(ARGB.color(255, uuid.hashCode()), 0.9F)`

個別色は `/waypoint` 後すぐには変わらず、定期更新（2 秒）のタイミングで反映される。

`WaypointColor` は `net.minecraft.util.ARGB` の `color` / `setBrightness` を**1対1で移植**したもの。
**この計算ロジック（演算順序・float/int・`Math.round`）は変更しないこと。** 変えると色がずれて意味が無くなる。

検証方法（実施済み）: Mojang 配布の server jar から `net.minecraft.util.ARGB` を反射で呼び、
ランダム UUID 200 万件＋境界値で `WaypointColor.defaultRgb` と突き合わせて全一致を確認した。
ロジックを触ったら同等の照合をやり直すこと。Minecraft のバージョンを上げる際は、
`LocatorBarRenderer` の色式と `ARGB` が変わっていないかを必ず確認する。

## 既知の制限・注意

- タブ名は全員共通の 1 つ。ON/OFF はサーバー全体（プレイヤー個別の視点切り替えは不可）。
- `playerListName` を設定するため、タブ管理系の他プラグインと競合しうる。
- チーム色判定はサーバーのメインスコアボード（`getMainScoreboard().getEntryTeam(name)`）を参照（バニラの `entity.getTeam()` と同じ）。
- Folia 非対応（グローバルスケジューラ前提）。

## リリース

`v*` タグの push で GitHub Actions（`.github/workflows/build.yml`）がビルドし、jar を Release に添付する。
バージョンは `pom.xml` / `plugin.yml` / README / deploy.sh / 本ファイルの記述を揃えて更新する。
