package pl.bkubiak.bkrynek.core.config;

import java.util.ArrayList;
import java.util.List;

public class ServersConfig {
    public String defaultProfile = "default";
    public List<ServerEntry> servers = new ArrayList<>();
    public boolean soundsEnabled = false;
    public String discordWebhookUrl = "";
    public static Boolean adsEnabled;
}
