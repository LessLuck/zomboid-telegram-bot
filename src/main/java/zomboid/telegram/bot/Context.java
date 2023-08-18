package zomboid.telegram.bot;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Context {

    TELEPORT,
    PLAYER,
    SERVER_MESSAGE,
    EXECUTE,
    PLAYERS
}
