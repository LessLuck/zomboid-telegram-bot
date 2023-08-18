package zomboid.telegram.bot;

import lombok.Data;
import lombok.experimental.Accessors;
import zomboid.telegram.bot.users.User;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class Session {

    private Long chatId;
    private Context context;
    private LocalDateTime lastActiveTime;
    private User user;
    private String savedValue;

    public Command getContextCommand() {

        return Command.valueOf(context.name());
    }
}
