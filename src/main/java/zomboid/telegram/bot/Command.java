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
    EXECUTE(UserGroups.ADMIN, "Execute custom console command", null),
    SAVE(UserGroups.ADMIN, "Save", null),
    BAN_USER(UserGroups.MOD, "Ban user", Context.PLAYER),
    TELEPORT(UserGroups.MOD, "Teleport", Context.PLAYER),
    KICK(UserGroups.MOD, "Kick", null),
    PLAYERS(UserGroups.MOD, "Players", null),
    MENU(UserGroups.MOD, "Back to menu", null);

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
