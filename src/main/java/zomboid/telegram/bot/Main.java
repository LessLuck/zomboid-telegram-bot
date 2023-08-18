package zomboid.telegram.bot;

import lombok.SneakyThrows;
import org.ini4j.Ini;
import org.modelmapper.ModelMapper;

import java.io.File;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

public class Main {

    @SneakyThrows
    public static void main(String[] args) {
        ServerConfigMapper config;
        try {
            var fileToParse = Path.of(new File(Main.class.getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()).getPath(), "ztbot.ini")
                    .toFile();
            config = new ModelMapper().map(new Ini(fileToParse), ServerConfigMapper.class);
        } catch (NoSuchFileException e) {
            // TODO Make default config file creation if file doesn't exist
            throw new RuntimeException("The configuration file have not been initialized");
        }
        var zomboidBot = new ZomboidBot(config);
            zomboidBot.runner();
    }
}