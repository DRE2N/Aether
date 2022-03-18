package de.erethon.aether.creature;

import de.erethon.bedrock.chat.MessageUtil;
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
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MineSkinFetcher {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    public static void fetchSkinFromIdAsync(String linkUrl, Callback callback) {
        if (linkUrl == null) {
            MessageUtil.log("Trying to fetch invalid skin for NPC.");
            return;
        }
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
                out = new DataOutputStream(con.getOutputStream());
                out.writeBytes("url=" + URLEncoder.encode(linkUrl, "UTF-8"));
                out.close();
                reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                JSONObject output = (JSONObject) new JSONParser().parse(reader);
                JSONObject data = (JSONObject) output.get("data");
                JSONObject texture = (JSONObject) data.get("texture");
                String textureEncoded = (String) texture.get("value");
                String signature = (String) texture.get("signature");
                con.disconnect();
                callback.call(new Skin(linkUrl, textureEncoded, signature));
            } catch (IOException exception) {
                Bukkit.getLogger().severe("Could not fetch skin! (Link: " + linkUrl + "). Message: " + exception.getMessage());
                exception.printStackTrace();
                callback.failed();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        });
    }

    public interface Callback {

        void call(Skin skinData);

        default void failed() {
        }
    }
}