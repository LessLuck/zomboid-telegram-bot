package zomboid.telegram.bot;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Session {

    private Long chatId;
    private Commands context;
    private LocalDateTime lastActiveTime;
}
