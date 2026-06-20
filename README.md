# LocatorColors

**日本語** | [English](README.en.md)

ロケーターバー（Locator Bar）に表示される**各プレイヤーのドットの色**を、タブ一覧（プレイヤーリスト）の名前にも付ける Paper プラグインです。
「バーに出てる青いドット、誰だ？」を解消します。タブを開けば、**バーと同じ色**で名前が並ぶので、色と人が一目で結びつきます。

```text
   ┌───────────  ロケーターバー  ───────────┐
        ●           ●            ●
   └────────────────────────────────────┘

   Tab（プレイヤーリスト）
   ● Steve      ← 黄緑のドット = 黄緑の名前
   ● Alex       ← 水色のドット = 水色の名前
   ● Notch      ← 橙のドット   = 橙の名前
```

<!-- スクリーンショットは撮影後に docs/screenshot.png として追加してください -->

- **● 名前** … 先頭のドットと名前を、その人のロケーターバー色で着色します
- **色の決まり方** … チームに色があればその色、なければ UUID から決まる既定色（バニラのバーと同じ計算）
- **バニラのバーと完全一致** … クライアントが使う計算式をサーバー側でそのまま再現しています
- **導入はサーバーだけ** … プレイヤーは MOD 不要、バニラクライアントのままで見えます

---

## 背景・目的

Minecraft のロケーターバーは、近くのプレイヤーの方向を**色付きのドット**で示してくれます。
ところがこのドットの色は、チーム未設定だと**プレイヤーごとに自動で割り振られる**ため、
「この色は誰なのか」がバーを見ただけでは分かりません。

LocatorColors は、各プレイヤーのバー色を**サーバー側で同じ計算**で求め、タブ一覧の名前に同じ色を付けます。
タブを開けば「色 ↔ 名前」の対応表が手に入るので、バー上のドットが誰なのかをすぐ判別できます。
クライアント MOD ではなくサーバー側プラグインなので、**プレイヤーは何もインストール不要**です。

---

## 動作要件

| 項目 | バージョン |
| --- | --- |
| サーバー | Paper **26.1.2**（build 69 で確認） |
| Java | **25**（25.0.x で確認） |
| ビルド | JDK 25 + Maven（`brew install openjdk@25 maven`） |
| 依存プラグイン | **なし** |
| クライアント | **バニラのままで可**（MOD 不要） |
| ゲームルール | バー自体の表示には `locatorBar` が **true** であること |

> この jar 1個だけで動作します。追加ライブラリ・別プラグインは不要です（`paper-api` は `provided` スコープ＝サーバーが実行時に提供）。
> ロケーターバーが表示されない場合は `/gamerule locatorBar true` を確認してください（バーが出ていなくてもタブの色付けは機能します）。

---

## 使い方

1. サーバーの `plugins/` に jar を置いて再起動（→ [サーバーへの配置](#サーバーへの配置)）。
2. 必要なら `/gamerule locatorBar true` でロケーターバーを表示状態に。
3. **タブ（プレイヤーリスト）を開く**だけ。各プレイヤーの名前が、バーのドットと同じ色になります。
4. 表示を一時的に止めたいときは `/locatorcolors off`、戻すときは `/locatorcolors on`（管理者向け）。

---

## 表示の見かた

| 表示 | 意味 |
| --- | --- |
| `● Steve`（黄緑） | バーで黄緑のドットの人。タブでも黄緑で表示 |
| `● Alex`（水色） | バーで水色のドットの人。タブでも水色で表示 |
| 名前が**チーム色** | そのプレイヤーがチームに所属し、チームに色がある場合はチーム色になります |
| 名前が**固有色** | チーム未所属の場合は UUID から決まる固有色（バーと同じ） |

> ロケーターバーのドットは「自分以外」が対象です。自分の色はバーには出ませんが、他の人のタブには自分の色付き名前が見えています。

---

## コマンド仕様

| コマンド | 説明 | 権限 |
| --- | --- | --- |
| `/locatorcolors` | 現在の ON/OFF を表示 | `locatorcolors.admin` |
| `/locatorcolors on` | タブの色付けを**全体で** ON | `locatorcolors.admin` |
| `/locatorcolors off` | タブの色付けを**全体で** OFF（名前を元に戻す） | `locatorcolors.admin` |
| `/locatorcolors status` | 現在の ON/OFF を表示 | `locatorcolors.admin` |

- タブ名（player list name）は**全員に共通の1つ**のため、表示の ON/OFF は**サーバー全体**で切り替わります（プレイヤー個別の視点切り替えはできません）。
- 既定は **ON**。状態はサーバー稼働中のみ保持され、**サーバー再起動で ON に戻ります**。
- `/locatorcolors` のタブ補完で `on` / `off` / `status` が候補に出ます。

---

## 権限

| 権限ノード | 既定 | 説明 |
| --- | --- | --- |
| `locatorcolors.show` | `true`（全員） | そのプレイヤーのタブ名にロケーター色を**付ける**ことを許可 |
| `locatorcolors.admin` | `op` | `/locatorcolors` で表示全体を ON/OFF することを許可 |

特定のプレイヤー／グループだけ色付けから**除外**したい場合は、対象の `locatorcolors.show` を `false` にします。

```bash
# 例: あるグループを色付け対象から除外
lp group default permission set locatorcolors.show false
# 例: あるユーザーを色付け対象から除外
lp user <name> permission set locatorcolors.show false
```

---

## 仕組み（技術メモ）

- **色の優先順位**: バニラの `Waypoint.Icon#cloneAndAssignStyle`（バー送信時の色決定）とクライアント `LocatorBarRenderer` に合わせ、次の順で色を決めます。
  1. `/waypoint modify <対象> color …` で**個別設定した色**（最優先）
  2. **チーム色**（サーバーのメインスコアボード。チーム色が黒のときはバー描画用の代替色 `0x2F2F30` に置換）
  3. **UUID 由来の既定色**
- **個別色（`/waypoint`）の取得**: この色はエンティティ内部の `locatorBarIcon.color` に入り paper-api では非公開のため、**公開メンバーのみ**（`WaypointTransmitter#waypointIcon()` と `Waypoint.Icon#color`）をリフレクションで読みます。取得できない環境では一度警告を出して自動的にチーム色／UUID 色へフォールバックします。
- **UUID 由来色の再現**: チーム色も個別色も無い場合、クライアントは UUID から色を計算します（式 `ARGB.setBrightness(ARGB.color(255, uuid.hashCode()), 0.9F)`）。本プラグインは `net.minecraft.util.ARGB` の該当処理を**1対1で移植**して同じ色を求めます。`UUID#hashCode()` は JDK 標準で両者一致するため、**バー色とタブ色は完全一致**します（この計算自体は NMS 非依存）。
- **タブへの反映**: `Player#playerListName(Component)` で、先頭にロケーター色のドット `●` を付け、名前を同じ色で着色します。チームの接頭辞・接尾辞があれば保持します。
- **差分更新**: 各プレイヤーの表示名を都度比較し、**色や付帯情報が変わったときだけ**送り直します。色はほぼ静的（UUID 由来は不変）なので、参加時に即適用し、チーム色変更などの取りこぼし防止に **2 秒ごと**の軽い定期チェックを併用します。
- **負荷について**: 主コストは「オンライン人数 × 文字列比較」だけで、変化が無ければパケットは飛びません。ワールド状態を読むためメインスレッド（`runTaskTimer`）で実行します。
- **読み取り専用**: プレイヤーやウェイポイントのデータを書き換えることはありません。

> タブ名はグローバルなため、対象は **Paper**（非 Folia）です。

---

## ビルド

JDK 25 と Maven が必要です（未導入なら `brew install openjdk@25 maven`）。
付属の `deploy.sh` でビルドできます（**Docker 不要**）。

```bash
./deploy.sh
```

生成物: `target/LocatorColors-1.1.0.jar`

`deploy.sh` は内部で JDK 25 を指定して `mvn clean package` を実行します。
別の場所の JDK を使う場合は `JAVA_HOME=/path/to/jdk25 ./deploy.sh` で上書きできます。直接ビルドするなら:

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
mvn clean package
```

---

## サーバーへの配置

サーバーの `plugins/` に jar を置いてサーバーを再起動します。jar の入手は次の 2 通り（A・B）です。Docker（itzg/minecraft-server）を使う場合は、後述の「Docker Compose で自動ダウンロード」も利用できます。

### A. リリース版を使う（ビルド不要・推奨）

[Releases](https://github.com/astail/mc-locator-bar-color-mieru/releases) から最新の `LocatorColors-<version>.jar` をダウンロードします。JDK や Maven は不要です。

```bash
# 最新リリースの jar をダウンロード（gh CLI を使う場合）
gh release download --repo astail/mc-locator-bar-color-mieru --pattern '*.jar'
```

### B. 自分でビルドする

[ビルド](#ビルド) の手順で `target/LocatorColors-1.1.0.jar` を生成します。

### 配置

入手した jar をサーバーの `plugins/` に置いてサーバーを再起動します。

```bash
# バインドマウントしている場合（ホスト側 plugins ディレクトリへコピー）
cp target/LocatorColors-1.1.0.jar /path/to/data/plugins/
docker restart <コンテナ名>

# 名前付きボリューム等の場合（コンテナへ直接コピー）
docker cp target/LocatorColors-1.1.0.jar <コンテナ名>:/data/plugins/
docker restart <コンテナ名>
```

### Docker Compose（itzg/minecraft-server）で自動ダウンロード

[`itzg/minecraft-server`](https://github.com/itzg/docker-minecraft-server) イメージを使う場合は、jar を手元に用意しなくても **`PLUGINS` 環境変数にリリースの URL を並べるだけ**で、起動時に自動ダウンロードして `plugins/` に配置できます。

```yaml
services:
  mc:
    image: itzg/minecraft-server
    tty: true
    stdin_open: true
    ports:
      - "25565:25565"
    environment:
      EULA: "TRUE"
      TYPE: "PAPER"
      VERSION: "26.2"
      PAPER_CHANNEL: "experimental"
      PLUGINS: |
        https://github.com/astail/mc-locator-bar-color-mieru/releases/download/v1.1.0/LocatorColors-1.1.0.jar
    volumes:
      - ./data:/data
    restart: unless-stopped
```

`PLUGINS` は改行区切りで複数指定できます。バージョンを更新したら、URL の `v1.1.0` とファイル名を新しいリリースに合わせて変更してください（例: `.../download/v1.1.0/LocatorColors-1.1.0.jar`）。

起動ログに以下が出れば成功です。

```text
[LocatorColors] LocatorColors を有効化しました。タブにロケーターバーの色を表示します。
```

---

## プロジェクト構成

```text
.
├── pom.xml
├── deploy.sh
├── README.md
└── src/main/
    ├── java/io/github/astail/locatorcolors/
    │   ├── LocatorColorsPlugin.java   # 本体（コマンド登録・参加/退出・定期更新・無効化時の復元）
    │   ├── TabColorizer.java          # タブ名の付与・解除（差分更新・元の名前の退避）
    │   ├── WaypointColor.java         # UUID → ロケーター色（バニラ ARGB の忠実移植・純粋ロジック）
    │   └── LocatorColorsCommand.java  # /locatorcolors（全体 ON/OFF）
    └── resources/plugin.yml
```

> パッケージ名（`io.github.astail.locatorcolors`）/ `LocatorColors` / コマンド名は任意でリネーム可能です（pom.xml・各 `package`・`plugin.yml` を揃えて変更）。

---

## 注意点

- **ロケーターバーが出ない**: バー自体の表示は `locatorBar` ゲームルールに従います。`/gamerule locatorBar true` を確認してください（タブの色付けはバーの表示有無に関わらず動作します）。
- **タブ名の上書き**: 本プラグインは `playerListName` を設定します。タブ名を変更する他プラグイン（タブ管理系）と併用すると、表示が競合することがあります。`/locatorcolors off` で本プラグインの色付けを止め、元の名前へ戻せます。
- **チームの接頭辞・接尾辞**: チームの接頭辞・接尾辞は保持しますが、名前そのものはロケーター色で着色します（チームに色がある場合はチーム色＝バー色なので見た目は一致します）。
- **`/waypoint` で個別設定した色**: `/waypoint modify <対象> color <色>`（または `color hex <hex>`）で設定した色を**最優先**で反映します。反映は定期更新のタイミングで行われるため、設定から**最大約 2 秒**で色が変わります。`color reset` で個別設定を解除すると、チーム色／UUID 由来色に戻ります。
  - この色はサーバー内部値のためリフレクションで読み取ります。取得できない環境（難読化サーバー等）では自動的にチーム色／UUID 色へフォールバックし、起動ログに一度だけ警告を出します。
- **対象はプレイヤー**: ロケーターバー／タブともに対象はプレイヤーです。
- `paper-api` の build 番号はサーバー更新に追従可能です（例: `26.1.2.build.70-stable`）。
