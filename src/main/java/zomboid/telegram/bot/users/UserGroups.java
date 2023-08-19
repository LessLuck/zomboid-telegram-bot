package zomboid.telegram.bot.users;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public enum UserGroups {
    ADMIN(1),
    MOD(2);

    private final Integer groupLevel;
}
