package zomboid.telegram.bot;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.List;

@AllArgsConstructor
@Getter
public enum ChatMenus {
    START_MENU(List.of(new KeyboardRow(
            List.of(new KeyboardButton(Commands.PLAYERS.getChatCommand()),
                    new KeyboardButton(Commands.SERVERMESSAGE.getChatCommand()),
                    new KeyboardButton(Commands.STOP.getChatCommand()),
                    new KeyboardButton(Commands.OFFLINE.getChatCommand())))));

    private final List<KeyboardRow> replyKeyboard;
}
