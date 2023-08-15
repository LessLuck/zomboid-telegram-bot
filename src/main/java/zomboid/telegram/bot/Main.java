package zomboid.telegram.bot;

import lombok.SneakyThrows;
import org.ini4j.Ini;
import org.modelmapper.ModelMapper;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.File;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

public class Main {
    @SneakyThrows
    public static void main(String[] args) {
        try {
            ConfigMapper config;
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);

            try {
                var fileToParse = Path.of(new File(Main.class.getProtectionDomain()
                                .getCodeSource()
                                .getLocation()
                                .toURI()).getPath(), "ztbot.ini")
                        .toFile();
                config = new ModelMapper().map(new Ini(fileToParse), ConfigMapper.class);
            } catch (NoSuchFileException e) {
                // TODO Сделать создание конфига при первом запуске
                throw new RuntimeException("The configuration file have not been initialized");
            }
            telegramBotsApi.registerBot(new ZomboidBot(config));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}