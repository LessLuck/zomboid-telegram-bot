package zomboid.telegram.bot;

import lombok.AllArgsConstructor;
import lombok.Getter;
import zomboid.telegram.bot.users.UserGroups;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@Getter
public enum Command {

    START(UserGroups.ADMIN, "Start", null),
    RESTART(UserGroups.ADMIN, "Restart", null),
    STOP(UserGroups.ADMIN, "Stop", null),
    SERVER_MESSAGE(UserGroups.ADMIN, "Server message", null),
    TELEPORT(UserGroups.ADMIN, "Teleport", Context.PLAYER),
    EXECUTE(UserGroups.ADMIN, "Execute", null),
    BAN_USER(UserGroups.GM, "Ban user", Context.PLAYER),
    SAVE(UserGroups.GM, "Save", null),
    KICK(UserGroups.GM, "Kick", null),
    PLAYERS(UserGroups.GM, "Players", null),
    MENU(UserGroups.GM, "Back to menu", null);

    private final UserGroups userGroup;
    private final String chatCommand;
    private final Context context;

    private static final Map<String, Command> lookup = new HashMap<>();

    static {
        for (Command d : Command.values()) {
            lookup.put(d.getChatCommand(), d);
        }
    }

    public static Command get(String chatCommand) {
        return lookup.get(chatCommand);
    }
}
