# Zomboid server management telegram bot
This bot was made for easy to setup project zomboid server management. It's a Java executable program, which is able to simultaneously interact with multiple users with different permissions and perform different commands.

## Contents
- [Features](#features)
- [Requirements](#requirements)
- [Installation guide](#installation-guide)
  - [How to create a telegram bot](#how-to-create-a-telegram-bot)
  - [LinuxGSM server](#linuxgsm-server)
  - [Other server](#other-server)
- [How to use](#how-to-use)
- [Example of bot usage](#example-of-bot-usage)
  - [Server restart](#server-restart)

## Features
- Kick player
- Ban player
- Teleport player to another player
- Send server messages
- Send custom console command
- LinuxGSM features only:
  - Start server
  - Stop server
  - Restart server

## Requirements
- JDK version 17 or higher
- (Optional) Zomboid server hosted using LinuxGSM (Adds features such as starting/stopping the server)

## Installation guide
### How to create a telegram bot
Instructions on how to create a telegram bot can be found [here](https://core.telegram.org/bots/features#creating-a-new-bot).

### LinuxGSM server
Put the jar file in the parent folder of the "pzserver" file, then run the bot using the following command:
```
java -jar zomboid-telegram-bot-1.1.0.jar
```

### Other server
Put the jar in any location, then run the bot using the following command:
```
java -jar zomboid-telegram-bot-1.1.0.jar
```

## How to use
During the first launch of the bot - two new files (zbot.ini, zbot_users.txt) will be created in the jar file's parent folder.

In zbot.ini insert the telegram bot's token key and it's username.

Also fill it with the server's rcon password and port (if it's different from default).

Resulting zbot.ini file should look like this:
```
[bot]
botToken=0000000000:AAAA-AAAAAAAAAAAAAAAAAAAAAAAAAAA
botName=AmazingBot

[server]
hostname=localhost
rconPassword=somePassword
rconPort=27015
```
In zbot_users.txt insert the telegram users usernames and user group which you want to apply to them.

Possible user groups:
- MOD (Teleporting/kicking/banning players)
- ADMIN (Same as MOD + Starting/stopping server/sending server message)

Resulting zbot_users.txt file should look like this where Admin is @Admin username in Telegram:
```
Admin=ADMIN
Moderator=MOD
```
## Example of bot usage
### Server restart
#### Picking a server restart option
![image](https://github.com/LessLuck/zomboid-telegram-bot/assets/16764015/3045d650-26d4-4a4a-9822-e26cb38bd671)

#### Server restarting
![image](https://github.com/LessLuck/zomboid-telegram-bot/assets/16764015/27b32170-fe18-48aa-9007-2ee4185c1f0e)
