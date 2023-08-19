package zomboid.telegram.bot.users;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
// Change to read from the file
public enum User {
    LessLuck(UserGroups.ADMIN),
    LessLuckTest(UserGroups.GM);

    private final UserGroups userGroup;
}
