package zomboid.telegram.bot;

import lombok.SneakyThrows;
import nl.vv32.rcon.Rcon;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage.SendMessageBuilder;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import zomboid.telegram.bot.users.UserGroups;
import zomboid.telegram.bot.users.UserList;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

public class ZomboidBot extends TelegramLongPollingBot {

    private final ConfigMapper config;
    private Long chatId;
    private UserGroups userLevel;
    private Rcon rcon;
    private List<Session> sessions;

    public ZomboidBot(ConfigMapper config) {
        super(config.getBot()
                .getBotToken());
        this.config = config;
    }

    @Override
    @SneakyThrows
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage()
                .hasText()) {
            chatId = update.getMessage()
                    .getChatId();
            var message_text = update.getMessage()
                    .getText();

            var currentSession = sessions.stream()
                    .filter(session -> session.getChatId()
                            .equals(chatId))
                    .findFirst()
                    .orElse(new Session());
            if (currentSession.getChatId() == null) {
                runCommand(Commands.START);
            }

            SendMessageBuilder messageBuilder = SendMessage.builder()
                    .chatId(chatId);
            Commands command;

            try {
                command = Commands.get(message_text);
            } catch (IllegalArgumentException e) {
                messageBuilder.text("Command unknown");
                execute(messageBuilder.build());
                return;
            }

            try {
                userLevel = UserList.valueOf(update.getMessage()
                                .getFrom()
                                .getUserName())
                        .getGroupLevel();
            } catch (IllegalArgumentException e) {
                messageBuilder.text("You're not authorized to use this bot");
                execute(messageBuilder.build());
                return;
            }

            // trying to open RCON connection
            try {
                if (rcon == null) {
                    rcon = Rcon.open(config.getServer()
                            .getHostname(), config.getServer()
                            .getRconPort());

                    if (rcon.authenticate(config.getServer()
                            .getRconPassword())) {
                    } else {
                        messageBuilder.text("Failed to authenticate");
                        var replyKeyboard = ReplyKeyboardMarkup.builder()
                                .keyboard(List.of(new KeyboardRow(List.of(new KeyboardButton("/start")))))
                                .oneTimeKeyboard(true)
                                .build();
                        messageBuilder.replyMarkup(replyKeyboard);
                        execute(messageBuilder.build());
                        return;
                    }
                }

            } catch (IOException e) {
                messageBuilder.text("Server is currently offline");
                var replyKeyboard = ReplyKeyboardMarkup.builder()
                        .keyboard(List.of(new KeyboardRow(List.of(new KeyboardButton("START THE SERVER")))))
                        .oneTimeKeyboard(true)
                        .build();
                messageBuilder.replyMarkup(replyKeyboard);
                execute(messageBuilder.build());
                return;
            }

            runCommand(command);
        }
    }

    @SneakyThrows
    private void runCommand(Commands command) {
        var messageBuilder = SendMessage.builder();

        // If command level higher than user -> send message
        if (command.getUserGroup()
                .getGroupLevel() < userLevel.getGroupLevel()) {
            messageBuilder.text("You're not allowed to execute this command")
                    .chatId(chatId);
            execute(messageBuilder.build());
            return;
        }

        if (command.equals(Commands.START)) {
            var replyKeyboard = ReplyKeyboardMarkup.builder()
                    .keyboard(ChatMenus.START_MENU.getReplyKeyboard())
                    .inputFieldPlaceholder("Pick a command from the list")
                    .oneTimeKeyboard(true)
                    .build();
            messageBuilder.replyMarkup(replyKeyboard);
            messageBuilder.text("Pick a command from the list")
                    .chatId(chatId);
            execute(messageBuilder.build());
            return;
        }

        if (command.equals(Commands.ONLINE)) {
            // TODO добавить запуск сервера
            messageBuilder.text("Server is starting...");
            Thread.sleep(Duration.ofSeconds(60));
            var replyKeyboard = new ReplyKeyboardMarkup();
            replyKeyboard.setKeyboard(ChatMenus.START_MENU.getReplyKeyboard());
            messageBuilder.replyMarkup(replyKeyboard);
            messageBuilder.text("Pick a command from the list")
                    .chatId(chatId);
            execute(messageBuilder.build());
            return;
        }

        if (command.equals(Commands.PLAYERS)) {
            var response = rcon.sendCommand("players");
            var replyKeyboard = new ReplyKeyboardMarkup();
            replyKeyboard.setKeyboard(ChatMenus.START_MENU.getReplyKeyboard());
            messageBuilder.replyMarkup(replyKeyboard);
            messageBuilder.text(response)
                    .chatId(chatId);
            execute(messageBuilder.build());
            return;
        }

        if (command.equals(Commands.STOP)) {
            var replyKeyboard = new ReplyKeyboardMarkup();
            replyKeyboard.setKeyboard(List.of(new KeyboardRow(
                    List.of(new KeyboardButton("1"), new KeyboardButton("2"), new KeyboardButton("3")))));
            messageBuilder.replyMarkup(replyKeyboard);
            messageBuilder.text("Pick a command from the list")
                    .chatId(chatId);
            execute(messageBuilder.build());
        }
    }

    @Override
    public String getBotUsername() {
        // Return bot username
        // If bot username is @MyAmazingBot, it must return 'MyAmazingBot'
        return config.getBot()
                .getBotName();
    }

    @Override
    public void onClosing() {
        try {
            rcon.close();
        } catch (IOException ignored) {
        }
        exe.shutdown();
    }
}
