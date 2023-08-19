package zomboid.telegram.bot;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.ini4j.Ini;
import org.modelmapper.ModelMapper;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

    @SneakyThrows
    public static void main(String[] args) {
        var parentLocation = new File(Main.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI()).getParentFile();
        var configFile = Path.of(parentLocation.getPath(), "ztbot.ini")
                .toFile();
        var usersFile = Path.of(parentLocation.getPath(), "ztbot_users.txt")
                .toFile();
        if (!configFile.exists() || !usersFile.exists()) {
            var configExample = Main.class.getResourceAsStream("/ztbot.ini");
            var usersExample = Main.class.getResourceAsStream("/ztbot_users.txt");
            if (!configFile.exists() && !usersFile.exists()) {
                assert configExample != null;
                Files.write(configFile.toPath(),
                        IOUtils.toString(configExample, StandardCharsets.UTF_8)
                                .getBytes());
                assert usersExample != null;
                Files.write(usersFile.toPath(),
                        IOUtils.toString(usersExample, StandardCharsets.UTF_8)
                                .getBytes());
                System.out.println("""
                        Bot config files have not been found.
                                            
                        Seems like this is your first time using this bot.
                        "ztbot.ini" and "ztbot_users.txt" files have been created in current folder.
                        Configure them to start using the bot.
                        """);
            } else if (!configFile.exists()) {
                assert configExample != null;
                Files.write(configFile.toPath(),
                        IOUtils.toString(configExample, StandardCharsets.UTF_8)
                                .getBytes());
                System.out.println("""
                        Bot config file have not been found.
                                            
                        "ztbot.ini" file have been created in current folder.
                        Configure it to start using the bot.
                        """);
            } else if (!usersFile.exists()) {
                assert usersExample != null;
                Files.write(usersFile.toPath(),
                        IOUtils.toString(usersExample, StandardCharsets.UTF_8)
                                .getBytes());
                System.out.println("""
                        Bot users config file have not been found.
                                            
                        "ztbot_users.txt" file have been created in the current folder.
                        Configure it to start using the bot.
                        """);
            }
            System.out.println("""
                                        
                    Press Enter to continue...""");
            System.in.read();
            return;
        }
        var config = new ModelMapper().map(new Ini(configFile), ServerConfigMapper.class);
        var zomboidBot = new ZomboidBot(config);
        zomboidBot.runner();
    }
}