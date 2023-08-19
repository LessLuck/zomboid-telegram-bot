package zomboid.telegram.bot;

import lombok.SneakyThrows;
import nl.vv32.rcon.Rcon;
import org.apache.commons.lang3.RegExUtils;
import org.springframework.lang.Nullable;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updates.GetUpdates;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import zomboid.telegram.bot.menus.PlayerMenu;
import zomboid.telegram.bot.menus.StartMenu;
import zomboid.telegram.bot.users.User;
import zomboid.telegram.bot.users.UserMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZomboidBot extends TelegramLongPollingBot {

    public Long chatId;
    public Session currentSession;
    private LocalDateTime serverStartTime = null;
    private Integer updateOffset;
    private final ServerConfigMapper config;
    private static Rcon rcon;
    private static final List<User> botUsers = UserMapper.getBotUsers();
    private User currentUser;
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
                currentUser = null;

                // Capturing new messages and processing them
                if (!updates.isEmpty()) {
                    updates.forEach(this::processMessage);
                }
                updates.clear();
            } catch (TelegramApiRequestException e) {
                System.out.println("\n" + e.getMessage() + "\n");
                return;
            } catch (Exception e) {
                var stackTrace = e.getStackTrace();
                System.out.println(e.getMessage());
                if (e.getCause() != null)
                    System.out.println(e.getCause()
                            .toString());
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

            currentUser = botUsers.stream()
                    .filter(user -> user.username()
                            .equals(update.getMessage()
                                    .getChat()
                                    .getUserName()))
                    .findFirst()
                    .orElse(null);

            if (currentUser == null) {
                var messageBuilder = SendMessage.builder()
                        .text("You're not authorized to use this bot")
                        .chatId(chatId);
                execute(messageBuilder.build());
                return;
            }

            currentSession = getSession(update);

            var messageBuilder = SendMessage.builder()
                    .chatId(chatId);

            // if command is not from the available list - check current context
            var command = Command.get(message_text);

            boolean isRconOpened = false;
            try {
                if (rcon != null) {
                    isRconOpened = rcon.authenticate(config.getServer()
                            .getRconPassword());
                } else
                    isRconOpened = openRcon();
            }
            // If command is not start and can't connect to server - send message that server is offline
            catch (IOException e) {
                if (serverStartTime != null && serverStartTime.isAfter(LocalDateTime.now()
                        .minusMinutes(5))) {
                    messageBuilder.text("""
                            Server have been recently restarted/started.
                            Wait a little bit until it finishes loading.
                            Check server logs if you think something is wrong with it.""");
                    sendMessageWithMenuKeyboard(messageBuilder.build());
                    return;
                } else if (serverStartTime != null && serverStartTime.isBefore(LocalDateTime.now()
                        .minusMinutes(5))) {
                    messageBuilder.text("""
                            Server have been restarted/started %s minutes ago and still haven't loaded.
                            Check server logs to see what went wrong."""
                            .formatted(ChronoUnit.MINUTES.between(serverStartTime, LocalDateTime.now())));
                    sendMessageRemoveKeyboard(messageBuilder.build());
                    serverStartTime = null;
                    return;
                }

                if (command != Command.START) {
                    messageBuilder.text("Server is currently offline");
                    getStartServerButton(messageBuilder);
                    return;
                }
            }

            if (!isRconOpened && command != Command.START) {
                return;
            }

            if (command == Command.MENU) {
                new StartMenu(this).getStartMenu();
                return;
            }

            if (currentSession.getContext() != null) {
                switch (currentSession.getContext()) {
                    case SERVER_MESSAGE -> new StartMenu(this).sendServerMessage(message_text);
                    case EXECUTE -> new StartMenu(this).sendExecuteCommand(message_text);
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

    private Boolean openRcon() throws IOException, TelegramApiException {
        // trying to open RCON connection
        var messageBuilder = SendMessage.builder();
        messageBuilder.chatId(chatId);

        rcon = Rcon.open(config.getServer()
                .getHostname(), config.getServer()
                .getRconPort());

        if (rcon.authenticate(config.getServer()
                .getRconPassword())) {
            return true;
        } else {
            messageBuilder.text("Failed to authenticate");
            getStartServerButton(messageBuilder);
            return false;
        }
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
                    .setUser(currentUser);
        }
        return session;
    }

    public void openSession(Session session) {
        session.setLastActiveTime(LocalDateTime.now());
        sessions.add(session);
    }

    public void updateSession() {
        if (sessions == null || sessions.isEmpty()) {
            openSession(this.currentSession);
        }

        assert sessions != null;
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

    private void getStartServerButton(SendMessage.SendMessageBuilder messageBuilder) throws TelegramApiException {
        var replyKeyboard = ReplyKeyboardMarkup.builder()
                .keyboard(List.of(new KeyboardRow(
                        List.of(new KeyboardButton(Command.START.getChatCommand())))))
                .oneTimeKeyboard(true)
                .resizeKeyboard(true)
                .build();
        messageBuilder.replyMarkup(replyKeyboard);
        execute(messageBuilder.build());
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

    @SneakyThrows
    public void sendMessageWithMenuKeyboard(SendMessage message) {
        sendMessageWithKeyboard(message, new KeyboardRow());
    }

    @SneakyThrows
    private void runCommand(Command command, Session session) {
        var messageBuilder = SendMessage.builder()
                .chatId(chatId);

        // If command level higher than user -> throw back to main menu
        if (command.getUserGroup() == null || command.getUserGroup()
                .getGroupLevel() < session.getUser()
                .userGroup()
                .getGroupLevel()) {
            messageBuilder.text("You're not allowed to execute this command");
            execute(messageBuilder.build());
            if (session.getContext() == null) {
                new StartMenu(this).getStartMenu();
            } else {
                runCommand(session.getContextCommand(), session);
            }
            return;
        }

        switch (command) {
            case PLAYERS -> startMenu.getPlayers();
            case SERVER_MESSAGE -> startMenu.getServerMessageForm();
            case EXECUTE -> startMenu.getExecuteForm();
            case KICK -> playerMenu.kickPlayer(currentSession.getSavedValue());
            case BAN_USER -> playerMenu.banPlayer(currentSession.getSavedValue());
            case START -> {
                if (checkBashAvailable())
                    if (runBashCommand("./pzserver start")) {
                        serverStartTime = LocalDateTime.now();
                        messageBuilder.text("Server have been successfully started, wait a bit for it to load");
                        sendMessageWithMenuKeyboard(messageBuilder.build());
                    } else {
                        messageBuilder.text("Server failed to start, see above message for details");
                        sendMessageWithMenuKeyboard(messageBuilder.build());
                    }
                else {
                    messageBuilder.text("Cannot locate LinuxGSM's pzserver file," +
                            " starting/stopping server is not possible");
                    sendMessageWithMenuKeyboard(messageBuilder.build());
                }
            }
            case RESTART -> {
                if (checkBashAvailable())
                    if (runBashCommand("./pzserver restart")) {
                        serverStartTime = LocalDateTime.now();
                        messageBuilder.text("Server has been successfully restarted, wait a bit for it to load");
                        sendMessageWithMenuKeyboard(messageBuilder.build());
                        rcon.close();
                        rcon = null;
                    } else {
                        messageBuilder.text("Server failed to restart, see above message for details");
                        sendMessageWithMenuKeyboard(messageBuilder.build());
                    }
                else {
                    messageBuilder.text("Cannot locate LinuxGSM's pzserver file," +
                            " starting/stopping server is not possible");
                    sendMessageWithMenuKeyboard(messageBuilder.build());
                }
            }
            case STOP -> {
                if (checkBashAvailable())
                    if (runBashCommand("./pzserver stop")) {
                        serverStartTime = LocalDateTime.now();
                        messageBuilder.text("Server has been successfully stopped");
                        getStartServerButton(messageBuilder);
                        rcon.close();
                        rcon = null;
                    } else {
                        messageBuilder.text("Server failed to stop, see above message for details");
                        sendMessageWithMenuKeyboard(messageBuilder.build());
                    }
                else {
                    messageBuilder.text("Cannot locate LinuxGSM's pzserver file," +
                            " starting/stopping server is not possible");
                    sendMessageWithMenuKeyboard(messageBuilder.build());
                }
            }
            default -> {
                messageBuilder.text("Command %s is not possible to execute".formatted(command))
                        .chatId(session.getChatId());
                sendMessageWithMenuKeyboard(messageBuilder.build());
            }
        }
    }

    @SneakyThrows
    private Boolean runBashCommand(String command) {
        var processBuilder = new ProcessBuilder("/usr/bin/bash", "-c", command);
        var env = processBuilder.environment();
        // env var TMUX/STY should be null, else LinuxGSM throws exception if bot launched from screen/tmux session
        env.put("TMUX", "");
        env.put("STY", "");
        var process = processBuilder.start();
        var messageBuilder = SendMessage.builder()
                .chatId(chatId);
        StringBuilder consoleOutput = new StringBuilder("Console output:");
        messageBuilder.text(consoleOutput.toString());
        var messageId = execute(messageBuilder.build()).getMessageId();
        String b;
        String s;
        boolean isFailed = false;
        while ((b = process.inputReader()
                .readLine()) != null) {
            s = b;
            System.out.println(s);
            if (s.contains("FAIL"))
                isFailed = true;
            if (!s.isEmpty()) {
                // Removing stylized garbage from output to make it look good in the message
                s = RegExUtils.replacePattern(s, "\033\\[K", "");
                s = RegExUtils.replacePattern(s, "\\e\\[.?.?m", "");
                consoleOutput.append("\n")
                        .append(s);
                execute(EditMessageText.builder()
                        .chatId(chatId)
                        .messageId(messageId)
                        .text(consoleOutput.toString())
                        .build());
            } else
                consoleOutput.append("\n");
        }
        return !isFailed;
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

    @SneakyThrows
    public boolean checkBashAvailable() {
        return Path.of(new File(Main.class.getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .toURI()).getParentFile()
                        .getPath(), "pzserver")
                .toFile()
                .exists();
    }

    @Override
    public String getBotUsername() {
        // Return bot username
        // If bot username is @MyAmazingBot, it must return 'MyAmazingBot'
        return config.getBot()
                .getBotName();
    }

    @Override
    public void onUpdateReceived(Update update) {
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
