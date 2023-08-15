package zomboid.telegram.bot.users;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserList {
    LessLuck(UserGroups.ADMIN),
    Vilkas(UserGroups.GM);

    private final UserGroups groupLevel;
}
