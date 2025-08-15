# player-switch
A Fabric mod for Minecraft: Java Edition that switches the active player after set time intervals. 
Only one player can play the game at a time.

## Configuration
> [!IMPORTANT]
> If you start the server without configuring the participants, you'll get an error like: "No participants are configured. Please modify ./config/player-switch/config.toml first!"
> To fix this, you'll need to configure at least the participants in `config/player-switch/config.toml`

```toml
#Define all players that should participate in the challenge in this file.
#Players will take turns in the order they occur in this file.
#You can simply define a player by their Minecraft username.
#Just add an entry like this for every player:
[[participants]]
name = "jeb_"

[[participants]]
name = "Notch"

#-----------------------------------------------------
#Additional configuration (optional)
#-----------------------------------------------------

#The time after which to switch to the next player, in ticks (1 second = 20 ticks, 1 minute = 1200 ticks, 10 minutes = 12000 ticks ...)
switchDelayTicks = 12000

#The index of the current participating player. You can manually change it to modify the player whose turn it is currently.
currentPlayer = 0

#The time the current player has already played, in ticks
elapsedTicks = 0

#This username will be assigned to every player
fixedUsername = "Player"

#This UUID will be assigned to every player, so that everyone has the same player and world data
fixedUuid = "a139e840-ff37-4cc7-a322-896af1a975f9"
```