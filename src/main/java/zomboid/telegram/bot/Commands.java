package zomboid.telegram.bot;

import lombok.AllArgsConstructor;
import lombok.Getter;
import zomboid.telegram.bot.users.UserGroups;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@Getter
public enum Commands {

    STOP(UserGroups.ADMIN, "stop"),
    ONLINE(UserGroups.ADMIN, "START THE SERVER"),
    OFFLINE(UserGroups.ADMIN, "offline"),
    SERVERMESSAGE(UserGroups.ADMIN, "server message"),
    TELEPORT(UserGroups.ADMIN, "teleport"),
    EXECUTE(UserGroups.ADMIN, "execute"),
    CONFIG(UserGroups.ADMIN, "CONFIG"),
    START(UserGroups.GM, "/start"),
    AUTHENTICATE(UserGroups.GM, "Authenticate"),
    BANUSER(UserGroups.GM, "banuser"),
    SAVE(UserGroups.GM, "save"),
    PLAYERS(UserGroups.GM, "players");

    private final UserGroups userGroup;
    private final String chatCommand;

    private static final Map<String, Commands> lookup = new HashMap<>();

    static {
        for (Commands d : Commands.values()) {
            lookup.put(d.getChatCommand(), d);
        }
    }

    public static Commands get(String chatCommand) {
        return lookup.get(chatCommand);
    }
}
