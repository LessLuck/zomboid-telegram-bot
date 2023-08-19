package zomboid.telegram.bot;

import lombok.Data;

@Data
public class ServerConfigMapper {

    private Bot bot;
    private Server server;

    @Data
    public static class Bot {
        private String botToken;
        private String botName;
    }

    @Data
    public static class Server {
        private String hostname;
        private Integer rconPort;
        private String rconPassword;
    }
}
