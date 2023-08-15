package zomboid.telegram.bot;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class ConfigMapper {

    private Bot bot;
    private Server server;

    @NoArgsConstructor
    @Data
    public static class Bot {
        private String botToken;
        private String botName;
    }

    @NoArgsConstructor
    @Data
    public static class Server {
        private String hostname;
        private Integer rconPort;
        private String rconPassword;
    }
}
