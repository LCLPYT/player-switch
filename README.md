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

#Message of the day options
[motd]
    #Whether to set the message of the day (MOTD) according to the player whose turn it is.
    enabled = true

    #The language to use for the message of the day
    language = "en_us"

#A Discord Webhook can be configured so that notifications about new turns are sent to a Discord channel. 
#Participants that have a Discord user ID defined will also be pinged when it's their turn.
[discordWebhook]
    #The Discord Webhook url. Can be retrieved by creating a Webhook in the "Integration" tab of the server settings.
    url = ""

    #The language to use for Discord messages
    messageLanguage = "en_us"
```

## Troubleshooting
### The server fails to start with player-switch installed.
Make sure to configure at least one participant. 
The server will only allow the configured participants to join.
When you are adding the participants definitions, make sure to delete the pre-generated participant definition:

```diff
-#A list of participating players.
-participants = []
+[[participants]]
+name = "Notch"
```