# LocatorColors

[日本語](README.md) | **English**

A Paper plugin that shows **each player's Locator Bar dot color** on their name in the Tab list (player list).
It answers the question "that blue dot on the bar — who is it?". Open Tab and every name is painted in the **same color as its dot**, so colors and players line up at a glance.

```text
   ┌───────────  Locator Bar  ───────────┐
        ●           ●            ●
   └─────────────────────────────────────┘

   Tab (player list)
   ● Steve      <- lime dot  = lime name
   ● Alex       <- aqua dot  = aqua name
   ● Notch      <- orange dot = orange name
```

<!-- Add a screenshot as docs/screenshot.png once captured. -->

- **● Name** … the leading dot and the name are painted in that player's Locator Bar color
- **How the color is chosen** … the team color if the team has one, otherwise the default color derived from the UUID (the same math the vanilla bar uses)
- **Exact match with the vanilla bar** … the plugin reproduces the client's color formula on the server side
- **Server-side only** … players need no mods; it works on a vanilla client

---

## Background / Purpose

Minecraft's Locator Bar shows the direction of nearby players as **colored dots**.
When players are not on a colored team, those dot colors are **assigned automatically per player**, so just looking at the bar tells you nothing about *who* a given color is.

LocatorColors computes each player's bar color on the server **with the same calculation** and applies it to their name in the Tab list. Opening Tab gives you a "color ↔ name" key, so you can instantly tell who a dot on the bar belongs to. Because it is a server-side plugin (not a client mod), **players install nothing**.

---

## Requirements

| Item | Version |
| --- | --- |
| Server | Paper **26.1.2** (verified on build 69) |
| Java | **25** (verified on 25.0.x) |
| Build | JDK 25 + Maven (`brew install openjdk@25 maven`) |
| Plugin dependencies | **None** |
| Client | **Vanilla is fine** (no mods) |
| Game rule | `locatorBar` must be **true** to see the bar itself |

> A single jar is all you need. No extra libraries or plugins (`paper-api` is `provided`, i.e. supplied by the server at runtime).
> If the bar does not show, check `/gamerule locatorBar true` (the Tab coloring works whether or not the bar is visible).

---

## Usage

1. Drop the jar into the server's `plugins/` and restart (→ [Deployment](#deployment)).
2. If needed, enable the bar with `/gamerule locatorBar true`.
3. Just **open Tab (the player list)**. Each name is shown in the same color as its bar dot.
4. To pause it temporarily use `/locatorcolors off`, and `/locatorcolors on` to restore (admin).

---

## Reading the display

| Display | Meaning |
| --- | --- |
| `● Steve` (lime) | The player whose bar dot is lime. Shown lime in Tab too |
| `● Alex` (aqua) | The player whose bar dot is aqua. Shown aqua in Tab too |
| Name in a **team color** | If the player is on a team that has a color, the team color is used |
| Name in a **unique color** | If the player is on no team, a unique color derived from the UUID (matching the bar) |

> The bar dots are for "other players". Your own color never appears on your own bar, but other players see your colored name in their Tab.

---

## Commands

| Command | Description | Permission |
| --- | --- | --- |
| `/locatorcolors` | Show the current ON/OFF state | `locatorcolors.admin` |
| `/locatorcolors on` | Turn Tab coloring ON **server-wide** | `locatorcolors.admin` |
| `/locatorcolors off` | Turn Tab coloring OFF server-wide (restores names) | `locatorcolors.admin` |
| `/locatorcolors status` | Show the current ON/OFF state | `locatorcolors.admin` |

- The Tab name (player list name) is **a single value shared by everyone**, so ON/OFF is **server-wide** (there is no per-viewer toggle).
- Default is **ON**. The state is kept only while the server is running and **resets to ON on restart**.
- Tab completion offers `on` / `off` / `status`.

---

## Permissions

| Permission node | Default | Description |
| --- | --- | --- |
| `locatorcolors.show` | `true` (everyone) | Allow that player's Tab name to be **colored** |
| `locatorcolors.admin` | `op` | Allow toggling the whole display via `/locatorcolors` |

To **exclude** specific players/groups from coloring, set their `locatorcolors.show` to `false`.

```bash
# Example: exclude a group from coloring
lp group default permission set locatorcolors.show false
# Example: exclude a user from coloring
lp user <name> permission set locatorcolors.show false
```

---

## How it works (technical notes)

- **Reproducing the color**: When there is no team color, the client computes the dot color from the UUID (Minecraft 26.1.2 `LocatorBarRenderer`). The formula is `ARGB.setBrightness(ARGB.color(255, uuid.hashCode()), 0.9F)`. The plugin ports the relevant `net.minecraft.util.ARGB` logic **one-to-one** and computes the same color on the server. Since `UUID#hashCode()` is standard JDK and identical on both sides, **the bar color and the Tab color match exactly** (no NMS involved).
- **Team color priority**: When transmitting the bar, vanilla resolves the icon color in `Waypoint.Icon#cloneAndAssignStyle`, **preferring the team color** if present (substituting `0x2F2F30` when the team color is black, for bar rendering). The plugin uses the same priority: the team color on the server main scoreboard, then the UUID-derived color.
- **Applying to Tab**: via `Player#playerListName(Component)`, prefixing a `●` dot in the locator color and coloring the name in the same color. Team prefix/suffix are preserved.
- **Diff updates**: each player's list name is compared and re-sent **only when the color or related info changes**. Colors are nearly static (UUID-derived ones never change), so it applies on join and runs a light **every-2-seconds** check to catch things like team-color changes.
- **Cost**: the main cost is just "online players × string compare"; no packets are sent when nothing changes. It runs on the main thread (`runTaskTimer`) because it reads world state.
- **Read-only**: it never modifies player or waypoint data.

> Because the Tab name is global, this targets **Paper** (not Folia).

---

## Build

JDK 25 and Maven are required (`brew install openjdk@25 maven` if missing).
The bundled `deploy.sh` builds it (**no Docker**).

```bash
./deploy.sh
```

Output: `target/LocatorColors-1.0.0.jar`

`deploy.sh` runs `mvn clean package` with JDK 25. To use a different JDK, override with `JAVA_HOME=/path/to/jdk25 ./deploy.sh`. To build directly:

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
mvn clean package
```

---

## Deployment

Drop the jar into the server's `plugins/` and restart. There are two ways (A/B) to get the jar. With Docker (itzg/minecraft-server), see "Auto-download with Docker Compose" below.

### A. Use a release (no build, recommended)

Download the latest `LocatorColors-<version>.jar` from [Releases](https://github.com/astail/mc-locator-bar-color-mieru/releases). No JDK or Maven needed.

```bash
# Download the latest release jar (with gh CLI)
gh release download --repo astail/mc-locator-bar-color-mieru --pattern '*.jar'
```

### B. Build it yourself

Produce `target/LocatorColors-1.0.0.jar` via [Build](#build).

### Placement

```bash
# Bind mount (copy into the host plugins directory)
cp target/LocatorColors-1.0.0.jar /path/to/data/plugins/
docker restart <container>

# Named volume etc. (copy into the container)
docker cp target/LocatorColors-1.0.0.jar <container>:/data/plugins/
docker restart <container>
```

### Auto-download with Docker Compose (itzg/minecraft-server)

With the [`itzg/minecraft-server`](https://github.com/itzg/docker-minecraft-server) image you can just list the release URL in the **`PLUGINS` environment variable** and it is downloaded into `plugins/` on startup.

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
        https://github.com/astail/mc-locator-bar-color-mieru/releases/download/v1.0.0/LocatorColors-1.0.0.jar
    volumes:
      - ./data:/data
    restart: unless-stopped
```

`PLUGINS` accepts multiple newline-separated entries. When you update the version, change `v1.0.0` and the file name in the URL to match the new release.

A successful start logs:

```text
[LocatorColors] LocatorColors を有効化しました。タブにロケーターバーの色を表示します。
```

---

## Project layout

```text
.
├── pom.xml
├── deploy.sh
├── README.md
└── src/main/
    ├── java/io/github/astail/locatorcolors/
    │   ├── LocatorColorsPlugin.java   # entry point (command, join/quit, periodic refresh, restore on disable)
    │   ├── TabColorizer.java          # applies/removes Tab names (diff updates, original-name backup)
    │   ├── WaypointColor.java         # UUID -> locator color (faithful port of vanilla ARGB, pure logic)
    │   └── LocatorColorsCommand.java  # /locatorcolors (server-wide ON/OFF)
    └── resources/plugin.yml
```

> The package name (`io.github.astail.locatorcolors`), `LocatorColors`, and the command name can be renamed freely (change pom.xml, each `package`, and `plugin.yml` together).

---

## Notes

- **Bar not showing**: the bar itself follows the `locatorBar` game rule. Check `/gamerule locatorBar true` (Tab coloring works regardless of bar visibility).
- **Tab name override**: this plugin sets `playerListName`. It may conflict with other Tab-managing plugins. Use `/locatorcolors off` to stop coloring and restore names.
- **Team prefix/suffix**: preserved, but the name itself is colored with the locator color (when the team has a color, that equals the bar color, so it looks consistent).
- **Colors set via `/waypoint`**: a color explicitly set per player by an admin via `/waypoint modify <target> color ...` lives only in server internals (not exposed by the API), so it cannot be reflected. In that case the plugin falls back to the team/UUID color (known limitation).
- **Players only**: both the Locator Bar and the Tab list target players.
- The `paper-api` build number can track server updates (e.g. `26.1.2.build.70-stable`).
