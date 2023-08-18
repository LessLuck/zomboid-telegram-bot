package zomboid.telegram.bot;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class CachedMessage {

    private Command command;
    private LocalDateTime lastCallTime;
    private SendMessage message;
    private EditMessageText editMessage;

}
