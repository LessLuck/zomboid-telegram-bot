package zomboid.telegram.bot;

import lombok.SneakyThrows;
import nl.vv32.rcon.Rcon;
import org.springframework.lang.Nullable;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updates.GetUpdates;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import zomboid.telegram.bot.menus.PlayerMenu;
import zomboid.telegram.bot.menus.StartMenu;
import zomboid.telegram.bot.users.User;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZomboidBot extends TelegramLongPollingBot {

    public Long chatId;
    public Session currentSession;
    private Integer updateOffset;
    private final ServerConfigMapper config;
    private static Rcon rcon;
    private final List<Session> sessions = new ArrayList<>();
    private final StartMenu startMenu = new StartMenu(this);
    private final PlayerMenu playerMenu = new PlayerMenu(this);

    public ZomboidBot(ServerConfigMapper config) {
        super(config.getBot()
                .getBotToken());
        this.config = config;
    }

    @SneakyThrows
    public void runner() {
        while (true) {
            try {
                var updates = execute(GetUpdates.builder()
                        .offset(updateOffset)
                        .build());
                chatId = null;
                currentSession = null;

                // Capturing new messages and processing them
                if (!updates.isEmpty()) {
                    updates.forEach(this::processMessage);
                }
                updates.clear();
            } catch (Exception e) {
                var stackTrace = e.getStackTrace();
                System.out.println(e.getMessage());
                Arrays.stream(stackTrace)
                        .forEach(stack -> System.out.println(stack.toString()));
                System.out.println();
            }
        }
    }

    @SneakyThrows
    private void processMessage(Update update) {
        updateOffset = update.getUpdateId() + 1;
        if (update.hasMessage() && update.getMessage()
                .hasText()) {
            chatId = update.getMessage()
                    .getChatId();
            var message_text = update.getMessage()
                    .getText();

            try {
                User.valueOf(update.getMessage()
                        .getChat()
                        .getUserName());
            } catch (IllegalArgumentException e) {
                var messageBuilder = SendMessage.builder()
                        .text("You're not authorized to use this bot")
                        .chatId(chatId);
                execute(messageBuilder.build());
            }

            currentSession = getSession(update);

            var messageBuilder = SendMessage.builder()
                    .chatId(chatId);

            var isRconOpened = openRcon(chatId);
            if (!isRconOpened)
                return;

            // if command is not from the available list - check current context
            var command = Command.get(message_text);

            if (command == Command.MENU) {
                new StartMenu(this).getStartMenu();
                return;
            }

            if (currentSession.getContext() != null) {
                switch (currentSession.getContext()) {
                    case SERVER_MESSAGE -> new StartMenu(this).sendServerMessage(message_text);
                    case PLAYERS -> new PlayerMenu(this).getPlayerMenu(message_text);
                    case PLAYER -> {
                        if (command != null && command.equals(Command.TELEPORT)) {
                            new PlayerMenu(this).getTeleportForm();
                        } else {
                            messageBuilder.text(
                                    "Entered command is not recognized, going back to main menu");
                            execute(messageBuilder.build());
                            new StartMenu(this).getStartMenu();
                        }
                    }
                    case TELEPORT -> new PlayerMenu(this).teleportPlayer(message_text);
                    default -> {
                        messageBuilder.text(
                                "Entered message is not possible to execute from here, going back to main menu");
                        execute(messageBuilder.build());
                        new StartMenu(this).getStartMenu();
                    }
                }
            } else if (command == null) {
                messageBuilder.text("Entered command is unknown, going back to main menu");
                execute(messageBuilder.build());
                new StartMenu(this).getStartMenu();
            } else {
                runCommand(command, currentSession);
            }
        }
    }

    @SneakyThrows
    public String sendRconCommand(String command) {
        return rcon.sendCommand(command);
    }

    @SneakyThrows
    public void sendRconCommandAndGoToMenu(String command) {
        var rconResponse = this.sendRconCommand(command);
        var messageBuilder = SendMessage.builder();
        messageBuilder.text("""
                RCON response:
                "%s"
                                
                Command have been sent, going back to main menu;
                """
                .formatted(rconResponse));
        messageBuilder.chatId(this.chatId);
        this.execute(messageBuilder.build());
        new StartMenu(this).getStartMenu();
    }

    @SneakyThrows
    private boolean openRcon(Long chatId) {
        // trying to open RCON connection
        var messageBuilder = SendMessage.builder();
        messageBuilder.chatId(chatId);

        try {
            if (rcon == null) {
                rcon = Rcon.open(config.getServer()
                        .getHostname(), config.getServer()
                        .getRconPort());

                if (rcon.authenticate(config.getServer()
                        .getRconPassword())) {
                    return true;
                } else {
                    messageBuilder.text("Failed to authenticate");
                    var replyKeyboard = ReplyKeyboardMarkup.builder()
                            .keyboard(List.of(new KeyboardRow(List.of(new KeyboardButton("/start")))))
                            .oneTimeKeyboard(true)
                            .build();
                    messageBuilder.replyMarkup(replyKeyboard);
                    execute(messageBuilder.build());
                    return false;
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
            return false;
        }
        return true;
    }

    @SneakyThrows
    private void runCommand(Command command, Session session) {
        var messageBuilder = SendMessage.builder();

        // If command level higher than user -> send message
        if (command.getUserGroup() == null || command.getUserGroup()
                .getGroupLevel() < session.getUser()
                .getUserGroup()
                .getGroupLevel()) {
            messageBuilder.text("You're not allowed to execute this command")
                    .chatId(session.getChatId());
            execute(messageBuilder.build());
            runCommand(session.getContextCommand(), session);
            return;
        }

        switch (command) {
            case PLAYERS -> startMenu.getPlayers();
            case SERVER_MESSAGE -> startMenu.getServerMessageForm();
            case KICK -> playerMenu.kickPlayer(currentSession.getSavedValue());
            case BAN_USER -> playerMenu.banPlayer(currentSession.getSavedValue());


//            case RESTART: {
//                // TODO Add server restart
//                messageBuilder.text("Server is starting...");
//                Thread.sleep(Duration.ofSeconds(60));
//                var replyKeyboard = new ReplyKeyboardMarkup();
//                replyKeyboard.setKeyboard(ChatMenus.START_MENU.getReplyKeyboard());
//                messageBuilder.replyMarkup(replyKeyboard);
//                messageBuilder.text("Pick a command from the list")
//                        .chatId(session.getChatId());
//                execute(messageBuilder.build());
//            break;
//            }

//            case ONLINE: {
//                // TODO Add server start
//                messageBuilder.text("Server is starting...");
//                Thread.sleep(Duration.ofSeconds(60));
//                var replyKeyboard = new ReplyKeyboardMarkup();
//                replyKeyboard.setKeyboard(ChatMenus.START_MENU.getReplyKeyboard());
//                messageBuilder.replyMarkup(replyKeyboard);
//                messageBuilder.text("Pick a command from the list")
//                        .chatId(session.getChatId());
//                execute(messageBuilder.build());
//            break;
//            }
//
//            case OFFLINE: {
//                // TODO Add server stop
//                var replyKeyboard = new ReplyKeyboardMarkup();
//                replyKeyboard.setKeyboard(List.of(new KeyboardRow(
//                        List.of(new KeyboardButton("1"), new KeyboardButton("2"), new KeyboardButton("3")))));
//                messageBuilder.replyMarkup(replyKeyboard);
//                messageBuilder.text("Pick a command from the list")
//                        .chatId(session.getChatId());
//                execute(messageBuilder.build());
//            break;
//            }
            default -> {
                messageBuilder.text("Command %s is not possible to execute".formatted(command))
                        .chatId(session.getChatId());
                execute(messageBuilder.build());
            }
        }
    }

    @SneakyThrows
    public Boolean checkContext(Context expectedContext) {
        var isContextSame = !(currentSession.getContext() == null || !currentSession.getContext()
                .equals(expectedContext));
        if (!isContextSame) {
            var messageBuilder = SendMessage.builder();
            messageBuilder.text("Context did not match");
            messageBuilder.chatId(chatId);
            execute(messageBuilder.build());
        }
        return isContextSame;
    }

    @Override
    public void onUpdateReceived(Update update) {
        this.runner();

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

    public Session getSession(Update update) {
        var session = sessions.stream()
                .filter(session1 -> session1.getChatId()
                        .equals(update.getMessage()
                                .getChatId()))
                .findFirst()
                .orElse(new Session());

        if (session.getChatId() == null) {
            session.setChatId(update.getMessage()
                            .getChatId())
                    .setUser(User.valueOf(update.getMessage()
                            .getChat()
                            .getUserName()));
        }
        return session;
    }

    public void openSession(Session session) {
        session.setLastActiveTime(LocalDateTime.now());
        sessions.add(session);
    }

    @SneakyThrows
    public void sendMessageWithKeyboard(SendMessage message, @Nullable KeyboardRow keyboardRow) {
        var menuRow = new KeyboardRow();
        menuRow.add(Command.MENU.getChatCommand());
        ReplyKeyboard replyKeyboard;
        // If keyboard given - send keyboard, if not - remove current
        if (keyboardRow != null) {
            var keyboardRows = List.of(keyboardRow, menuRow);
            replyKeyboard = ReplyKeyboardMarkup.builder()
                    .keyboard(keyboardRows)
                    .oneTimeKeyboard(true)
                    .isPersistent(true)
                    .resizeKeyboard(true)
                    .build();
        } else {
            replyKeyboard = ReplyKeyboardRemove.builder()
                    .removeKeyboard(true)
                    .build();
        }
        message.setReplyMarkup(replyKeyboard);
        execute(message);
    }

    @SneakyThrows
    public void sendMessageRemoveKeyboard(SendMessage message) {
        sendMessageWithKeyboard(message, null);
    }

    public void updateSession() {
        if (sessions == null || sessions.isEmpty()) {
            openSession(this.currentSession);
        }

        var sessionRow = sessions.stream()
                .filter(session1 -> session1.getChatId()
                        .equals(this.currentSession.getChatId()))
                .findFirst()
                .orElse(new Session());

        if (sessionRow.getChatId() == null) {
            openSession(this.currentSession);
        }

        sessions.remove(sessionRow);
        this.currentSession.setLastActiveTime(LocalDateTime.now());
        sessions.add(this.currentSession);
    }

}
