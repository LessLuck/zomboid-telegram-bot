package zomboid.telegram.bot.menus;

import lombok.SneakyThrows;
import org.springframework.lang.Nullable;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import zomboid.telegram.bot.Command;
import zomboid.telegram.bot.Context;
import zomboid.telegram.bot.PlayersList;
import zomboid.telegram.bot.ZomboidBot;

import java.util.ArrayList;
import java.util.List;

public class PlayerMenu {
    public static final KeyboardRow playerMenuButtons = new KeyboardRow(
            List.of(new KeyboardButton(Command.TELEPORT.getChatCommand()),
                    new KeyboardButton(Command.KICK.getChatCommand()),
                    new KeyboardButton(Command.BAN_USER.getChatCommand())));

    private final ZomboidBot bot;

    public PlayerMenu(ZomboidBot bot) {
        this.bot = bot;
    }

    @SneakyThrows
    public void getPlayerMenu(String playerName) {
        bot.currentSession.setContext(Context.PLAYER);
        bot.currentSession.setSavedValue(playerName);
        var messageBuilder = SendMessage.builder();
        messageBuilder.text("Pick an action from the list")
                .chatId(bot.chatId);
        var message = messageBuilder.build();
        bot.sendMessageWithKeyboard(message, playerMenuButtons, "Pick an action from the list");
        bot.updateSession();
    }

    @SneakyThrows
    public void kickPlayer(String userName) {
        if (!bot.checkContext(Context.PLAYER)) {
            new StartMenu(bot).getStartMenu();
            return;
        }
        var command = ("kick \"%s\"".formatted(userName));
        bot.sendRconCommandAndGoToMenu(command);
    }

    @SneakyThrows
    public void banPlayer(String userName) {
        if (!bot.checkContext(Context.PLAYER)) {
            new StartMenu(bot).getStartMenu();
            return;
        }
        var command = ("banuser \"%s\"".formatted(userName));
        bot.sendRconCommandAndGoToMenu(command);
    }

    @SneakyThrows
    public void teleportPlayer(String destinationUsername) {
        if (!bot.checkContext(Context.TELEPORT)) {
            new StartMenu(bot).getStartMenu();
            return;
        }
        var command = ("teleport \"%s\" \"%s\"".formatted(bot.currentSession.getSavedValue(), destinationUsername));
        bot.sendRconCommandAndGoToMenu(command);
    }

    @SneakyThrows
    public void getTeleportForm() {
        bot.currentSession.setContext(Context.TELEPORT);
        var messageBuilder = SendMessage.builder()
                .chatId(bot.chatId);
        var players = getPlayersList(bot.currentSession.getSavedValue());
        if (!players.isEmpty()) {
            messageBuilder.text("Choose the player to teleport to:");
            var message = messageBuilder.build();
            var playersListKeyboard = new PlayersList(players).getPlayersKeyboard();
            bot.sendMessageWithKeyboard(message, playersListKeyboard, "Choose the player to teleport to");
            bot.updateSession();
        } else {
            messageBuilder.text("There are no other connected players, going back to player menu");
            bot.execute(messageBuilder.build());
            bot.updateSession();
            getPlayerMenu(bot.currentSession.getSavedValue());
        }
    }

    @SneakyThrows
    public List<String> getPlayersList(@Nullable String ignorePlayer) {
        var playersResponse = bot.sendRconCommand("players")
                .lines()
                .toList();
        var players = new ArrayList<>(playersResponse.stream()
                .filter(player1 -> player1.startsWith("-"))
                .toList());
        players.replaceAll(p -> p.substring(1));
        players.remove(ignorePlayer);
        return players;
    }
}
