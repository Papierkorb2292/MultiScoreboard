# MultiScoreboard Minecraft Mod

This Minecraft Fabric mod allows you to view multiple scoreboards
and nbt data on the sidebar at the same time!

## Usage

### Scoreboards

To view multiple objectives at once, simply call
`/scoreboard objectives setdisplay sidebar <objective>` multiple
times with different objectives. Calling the command with the same
scoreboard again will remove the objective from the sidebar again.

`/scoreboard objectives setdisplay sidebar` still clears the sidebar.

For the mod to work, it must be installed on both the client and the server.
Otherwise, scoreboards act like in vanilla.

### Nbt Sidebar

To view nbt data in the sidebar, use the command `/data multiscoreboard toggle (block <targetPos>|entity <target>|storage <target>) [<path>] [<name>]`
to toggle whether the specified nbt data is displayed in the sidebar.
If `<path>` is not specified, the root compound is displayed.
If `<name>` is not specified, a default name is build based on the data object and nbt path.

The command `/data multiscoreboard remove <name>` removes the nbt sidebar with the specified name.

The command `/data multiscoreboard removeAll (block <targetPos>|entity <target>|storage <target>)`
removes all nbt sidebars for the specified data object.

### Scrolling

Use the `Up` or `Down` arrow keys to scroll through the sidebar.
If `Ctrl` is pressed, the next sidebar entry of the particular direction
will be brought into view.

## Download

You can download this mod on CurseForge (https://www.curseforge.com/minecraft/mc-mods/multiscoreboard),
on Modrinth (https://modrinth.com/mod/multiscoreboard) or on Github (https://github.com/Papierkorb2292/MultiScoreboard/releases/latest)

Alternatively, you can build the mod yourself by downloading this repository
and running `./gradlew build` in the root directory. The built mod will appear in `build/libs`.

## License

This mod is available under the CC0 license.
