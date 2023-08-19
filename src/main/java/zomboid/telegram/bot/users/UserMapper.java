package zomboid.telegram.bot.users;

import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import zomboid.telegram.bot.Main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UserMapper {

    @SneakyThrows
    public static List<User> getBotUsers() {
        var users = new ArrayList<User>();
        var fileToParse = Path.of(new File(Main.class.getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .toURI()).getParentFile()
                        .getPath(), "ztbot_users.txt")
                .toFile();
        var br = new BufferedReader(new FileReader(fileToParse));
        while (br.ready()) {
            var line = br.readLine();
            // If line contains character which look like commented lines - ignore
            if (StringUtils.containsAny(line.substring(0, 2), "//", "##"))
                continue;
            var user = line.split("=");
            var username = user[0];
            try {
                var userRole = UserGroups.valueOf(user[1]);
                users.add(new User(username, userRole));
            } catch (Exception e) {
                var stackTrace = e.getStackTrace();
                System.out.printf("An error occurred while trying to process user %s:", username);
                System.out.println(e.getMessage());
                Arrays.stream(stackTrace)
                        .forEach(stack -> System.out.println(stack.toString()));
                System.out.println();
            }
        }
        return users;
    }
}
