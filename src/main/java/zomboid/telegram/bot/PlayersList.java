package zomboid.telegram.bot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayersList {

    private List<String> players;

    public KeyboardRow getPlayersKeyboard() {
        var keyboardRow = new KeyboardRow();
        this.players.forEach(player -> keyboardRow.add(new KeyboardButton(player)));
        return new KeyboardRow(keyboardRow);
    }
}
