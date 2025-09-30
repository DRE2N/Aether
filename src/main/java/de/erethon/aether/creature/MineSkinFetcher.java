package de.erethon.aether.creature;

import de.erethon.aether.Aether;
import org.bukkit.Bukkit;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MineSkinFetcher {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    public static final Set<String> skinsInQueue = new java.util.HashSet<>();

    public static void fetchSkinFromIdAsync(String linkUrl, Callback callback) {
        if (linkUrl == null) {
            Aether.log("Trying to fetch invalid skin for NPC.");
            return;
        }
        if (skinsInQueue.contains(linkUrl)) {
            return;
        }
        Aether plugin = Aether.getInstance();
        String auth = plugin.getSkinCache().getAuthToken();
        if (auth == null || auth.isEmpty()) {
            Aether.log("No MineSkin auth token found.");
            return;
        }
        skinsInQueue.add(linkUrl);
        EXECUTOR.execute(() -> {
            DataOutputStream out = null;
            BufferedReader reader = null;
            try {
                URL target = new URL("https://api.mineskin.org/generate/url");
                HttpURLConnection con = (HttpURLConnection) target.openConnection();
                con.setRequestMethod("POST");
                con.setDoOutput(true);
                con.setConnectTimeout(1000);
                con.setReadTimeout(30000);
                con.setRequestProperty("User-Agent", "Erethon-Aether/1.0");
                con.setRequestProperty("Authorization", "Bearer " + auth);
                con.setRequestProperty("Content-Type", "application/json");

                JSONObject jsonBody = new JSONObject();
                jsonBody.put("url", linkUrl);

                out = new DataOutputStream(con.getOutputStream());
                out.writeBytes(jsonBody.toString());
                out.close();

                int responseCode = con.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    JSONObject output = (JSONObject) new JSONParser().parse(reader);
                    JSONObject data = (JSONObject) output.get("data");
                    JSONObject texture = (JSONObject) data.get("texture");
                    String textureEncoded = (String) texture.get("value");
                    String signature = (String) texture.get("signature");
                    callback.call(new Skin(linkUrl, textureEncoded, signature));
                } else {
                    reader = new BufferedReader(new InputStreamReader(con.getErrorStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    Bukkit.getLogger().severe("Could not fetch skin! (Link: " + linkUrl + "). Response: " + response);
                    callback.failed(linkUrl);
                }
                con.disconnect();
            } catch (IOException | ParseException exception) {
                Bukkit.getLogger().severe("Could not fetch skin! (Link: " + linkUrl + "). Message: " + exception.getMessage());
                exception.printStackTrace();
                callback.failed(linkUrl);
            }
        });
    }

    public interface Callback {

        void call(Skin skinData);

        default void failed(String url) {
            skinsInQueue.remove(url);
        }
    }
}