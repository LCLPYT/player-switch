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
#Just add simple entry like this for every player:
[[participants]]
    name = "jeb_"

#You can also create more advanced entries:
[[participants]]
    #The Minecraft username of the participant
    #Optional if you configure the UUID instead.
    name = "Notch"

    #You can also use the UUID of the participant's Minecraft account. 
    #If not set, the UUID will automatically be fetched from the name.
    uuid = "069a79f4-44e9-4726-a5be-fca90e38aaf5"

    #The Discord user id. If the Discord webhook is configured, the participant will be pinged with this user id.
    #If unset, the Minecraft username will be used and nobody will be pinged.
    discordId = "000000000000000000"

    #Can be used to override the name displayed for this user.
    #It's also useful when multiple people play with the same account.
    displayName = "Markus"

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

#Whether to show a player limit of one player in the server list
limitMaxPlayers = true

#Message of the day options
[motd]
    #Whether to set the message of the day (MOTD) according to the player whose turn it is.
    enabled = true

    #The language to use for the message of the day
    language = "en_us"

#A Discord webhook can be configured so that notifications about new turns are sent to a Discord channel. 
#Participants that have a Discord user ID defined will also be pinged when it's their turn.
[discordWebhook]
    #The Discord webhook url. Can be retrieved by creating a Webhook in the "Integration" tab of the server settings.
    url = ""

    #The language to use for Discord messages
    messageLanguage = "en_us"
```

## Troubleshooting
### The server fails to start with player-switch installed.
Make sure to configure at least one participant. 
The server will allow only the configured participants to join.
When you are adding the participants definitions, make sure to delete the pre-generated participant definition:

```diff
-#A list of participating players.
-participants = []
+[[participants]]
+name = "Notch"
```

## Docker
You can easily run a Minecraft server with player-switch installed using Docker.

### Starting a server
```
docker compose up server --build -d
```
Alternatively, you can use docker directly:

```
docker build -f docker/Dockerfile -t player-switch .
mkdir -p run/{config,world}
docker run \
    --mount type=bind,src="$(pwd)/run/config,dst=/app/config" \
    --mount type=bind,src="$(pwd)/run/world,dst=/app/world" \
    -u "$(id -u):$(id --rm -p 25565:25565 -d -e EULA=true --name=player-switch player-switch
```

Please notice that by passing `EULA=true`, or starting via docker compose, you accept the [Minecraft EULA](https://aka.ms/MinecraftEULA).

### Accessing the configuration
You can access all configuration in the `run/config` directory on the host system.
Specifically, you need to edit `run/config/player-switch/config.toml` initially to configure the participants etc.

### Stopping the server
```
docker compose down
```

Or if you invoked docker directly:
```
docker stop player-switch
```

### Resetting a run
```
docker compose down server && docker compose up reset
```

Or alternatively via docker directly:
```
mkdir -p run/{config,world}
docker container stop player-switch 2>/dev/null
docker run \
    --mount type=bind,src="$(pwd)/run/config,dst=/app/config" \
    --mount type=bind,src="$(pwd)/run/world,dst=/app/world" \
    -u "$(id -u):$(id -g)" --rm player-switch ./reset.sh
```