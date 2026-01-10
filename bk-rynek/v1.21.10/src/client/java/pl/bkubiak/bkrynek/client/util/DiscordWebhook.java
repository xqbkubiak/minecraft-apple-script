package pl.bkubiak.bkrynek.client.util;

import com.google.gson.Gson;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class DiscordWebhook {
    private static final Gson gson = new Gson();

    public static void send(String webhookUrl, Object bodyObject) {
        if (webhookUrl == null || webhookUrl.isEmpty())
            return;

        CompletableFuture.runAsync(() -> {
            try {
                URL url = java.net.URI.create(webhookUrl).toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agent", "BK-Rynek-Mod");
                connection.setDoOutput(true);

                String json = gson.toJson(bodyObject);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = json.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                connection.getResponseCode();
                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void sendPurchaseNotification(String webhookUrl, String itemName, String price, String server) {
        Map<String, Object> body = new HashMap<>();
        body.put("username", "BK-Rynek Notifications");
        body.put("avatar_url", "https://bkubiak.dev/faviconmods.png");

        List<Map<String, Object>> embeds = new ArrayList<>();
        Map<String, Object> embed = new HashMap<>();

        embed.put("title", "🛒 Pomyślnie zakupiono przedmiot!");
        embed.put("color", 0x2ECC71); // Piękny zielony szmaragdowy

        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(createField("Przedmiot", "`" + itemName + "`", true));
        fields.add(createField("Cena", "`" + price + "$`", true));
        fields.add(createField("Serwer", "`" + server + "`", false));

        embed.put("fields", fields);

        Map<String, String> footer = new HashMap<>();
        footer.put("text", "BK-Rynek Mod • bkubiak.dev/mods");
        embed.put("footer", footer);

        embed.put("description",
                "[Strona autora](https://bkubiak.dev/mods) • [Wesprzyj projekt (Tipply)](https://tipply.pl/@rajzeh)");

        embeds.add(embed);
        body.put("embeds", embeds);

        send(webhookUrl, body);
    }

    public static void sendErrorNotification(String webhookUrl, String reason, String server) {
        Map<String, Object> body = new HashMap<>();
        body.put("username", "BK-Rynek Notifications");
        body.put("avatar_url", "https://bkubiak.dev/faviconmods.png");

        List<Map<String, Object>> embeds = new ArrayList<>();
        Map<String, Object> embed = new HashMap<>();

        embed.put("title", "⚠️ Błąd automatyzacji!");
        embed.put("color", 0xE74C3C); // Czerwony Alizarin

        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(createField("Powód", "`" + reason + "`", true));
        fields.add(createField("Serwer", "`" + server + "`", true));

        embed.put("fields", fields);

        Map<String, String> footer = new HashMap<>();
        footer.put("text", "BK-Rynek Mod • bkubiak.dev/mods");
        embed.put("footer", footer);

        embeds.add(embed);
        body.put("embeds", embeds);

        send(webhookUrl, body);
    }

    private static Map<String, Object> createField(String name, String value, boolean inline) {
        Map<String, Object> field = new HashMap<>();
        field.put("name", name);
        field.put("value", value);
        field.put("inline", inline);
        return field;
    }
}
