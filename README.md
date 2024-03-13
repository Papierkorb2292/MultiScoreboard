# MultiScoreboard Minecraft Mod

This Minecraft Fabric mod allows you to view multiple scoreboards
on the sidebar at the same time!

## Usage

To do this, simply call
`/scoreboard objectives setdisplay sidebar <objective>` multiple
times with different objectives. Calling the command with the same
scoreboard again will remove the objective from the sidebar again.

`/scoreboard objectives setdisplay sidebar` still clears the sidebar.

For the mod to work, it must be installed on both the client and the server.
Otherwise, scoreboards act like in vanilla.

## Download

You can download this mod on CurseForge (https://www.curseforge.com/minecraft/mc-mods/multiscoreboard)
or Modrinth (https://modrinth.com/mod/multiscoreboard)

Alternatively, you can build the mod yourself by downloading this repository
and running `./gradlew build` in the root directory. The built mod will appear in `build/libs`.

## License

This mod is available under the CC0 license.
