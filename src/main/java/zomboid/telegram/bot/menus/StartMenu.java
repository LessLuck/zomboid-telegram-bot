package zomboid.telegram.bot.menus;

import lombok.SneakyThrows;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import zomboid.telegram.bot.Command;
import zomboid.telegram.bot.Context;
import zomboid.telegram.bot.PlayersList;
import zomboid.telegram.bot.ZomboidBot;

import java.util.List;

public class StartMenu {
    public static final List<KeyboardRow> startMenuButtons = List.of(new KeyboardRow(
                    List.of(new KeyboardButton(Command.PLAYERS.getChatCommand()),
                            new KeyboardButton(Command.SERVER_MESSAGE.getChatCommand()))),
            new KeyboardRow(List.of(
                    new KeyboardButton(Command.START.getChatCommand()),
                    new KeyboardButton(Command.RESTART.getChatCommand()),
                    new KeyboardButton(Command.STOP.getChatCommand()))),
            new KeyboardRow(List.of(
                    new KeyboardButton(Command.EXECUTE.getChatCommand()))));

    private final ZomboidBot bot;

    public StartMenu(ZomboidBot bot) {
        this.bot = bot;
    }

    @SneakyThrows
    public void getStartMenu() {
        var messageBuilder = SendMessage.builder();
        var replyKeyboard = ReplyKeyboardMarkup.builder()
                .keyboard(startMenuButtons)
                .inputFieldPlaceholder("Pick a command from the list")
                .oneTimeKeyboard(true)
                .isPersistent(true)
                .resizeKeyboard(true)
                .build();
        messageBuilder.replyMarkup(replyKeyboard);
        messageBuilder.text("Pick a command from the list")
                .chatId(bot.chatId);
        bot.execute(messageBuilder.build());
        bot.currentSession.setSavedValue(null);
        bot.currentSession.setContext(null);
        bot.updateSession();
    }

    @SneakyThrows
    public void getPlayers() {
        bot.currentSession.setContext(Context.PLAYERS);
        var players = new PlayerMenu(bot).getPlayersList(null);
        var messageBuilder = SendMessage.builder()
                .chatId(bot.chatId);
        if (!players.isEmpty()) {
            messageBuilder.text("Number of connected players: (%s)".formatted(players.size()));
            var message = messageBuilder.build();
            var playersListKeyboard = new PlayersList(players).getPlayersKeyboard();
            bot.sendMessageWithKeyboard(message, playersListKeyboard, "Choose a player form the list");
            bot.updateSession();
        } else {
            messageBuilder.text("There are no connected players, going back to main menu");
            bot.execute(messageBuilder.build());
            bot.updateSession();
            getStartMenu();
        }
    }

    @SneakyThrows
    public void getServerMessageForm() {
        var messageBuilder = SendMessage.builder();
        messageBuilder.chatId(bot.chatId);
        messageBuilder.text("Enter the message to be sent on the server");
        var message = messageBuilder.build();
        bot.sendMessageRemoveKeyboard(message);
        bot.currentSession.setContext(Context.SERVER_MESSAGE);
        bot.updateSession();
    }

    @SneakyThrows
    public void getExecuteForm() {
        var messageBuilder = SendMessage.builder();
        messageBuilder.chatId(bot.chatId);
        messageBuilder.text("Enter the command to be executed on the server");
        var message = messageBuilder.build();
        bot.sendMessageRemoveKeyboard(message);
        bot.currentSession.setContext(Context.EXECUTE);
        bot.updateSession();
    }

    public void sendServerMessage(String message) {
        var command = "servermsg \"%s\"".formatted(message);
        bot.sendRconCommandAndGoToMenu(command);
    }

    public void sendExecuteCommand(String command) {
        bot.sendRconCommandAndGoToMenu(command);
    }
}
