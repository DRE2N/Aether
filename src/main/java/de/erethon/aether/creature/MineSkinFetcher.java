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
import java.util.*;
import java.util.concurrent.*;

public class MineSkinFetcher {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    public static final Set<String> skinsInQueue = Collections.synchronizedSet(new HashSet<>());
    private static final Map<String, Integer> retryCount = new ConcurrentHashMap<>();
    private static final int MAX_RETRIES = 5;
    private static final long MIN_REQUEST_DELAY_MS = 3000; // Minimum 3 seconds between requests
    private static long lastRequestTime = 0;
    private static final Queue<PendingRequest> pendingQueue = new ConcurrentLinkedQueue<>();
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    static {
        // Start the queue processor
        scheduler.scheduleAtFixedRate(MineSkinFetcher::processPendingQueue, 0, 500, TimeUnit.MILLISECONDS);
    }

    private static class PendingRequest {
        final String linkUrl;
        final Callback callback;
        final long earliestExecutionTime;
        final int attempt;

        PendingRequest(String linkUrl, Callback callback, long earliestExecutionTime, int attempt) {
            this.linkUrl = linkUrl;
            this.callback = callback;
            this.earliestExecutionTime = earliestExecutionTime;
            this.attempt = attempt;
        }
    }

    private static void processPendingQueue() {
        PendingRequest request = pendingQueue.peek();
        if (request == null) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        // Check if enough time has passed since last request and if this request is ready
        if (currentTime - lastRequestTime >= MIN_REQUEST_DELAY_MS && currentTime >= request.earliestExecutionTime) {
            pendingQueue.poll(); // Remove from queue
            lastRequestTime = currentTime;
            executeRequest(request);
        }
    }

    private static void executeRequest(PendingRequest request) {
        EXECUTOR.execute(() -> {
            DataOutputStream out = null;
            BufferedReader reader = null;
            try {
                Aether plugin = Aether.getInstance();
                String auth = plugin.getSkinCache().getAuthToken();

                URL target = new URL("https://api.mineskin.org/generate/url");
                HttpURLConnection con = (HttpURLConnection) target.openConnection();
                con.setRequestMethod("POST");
                con.setDoOutput(true);
                con.setConnectTimeout(5000);
                con.setReadTimeout(30000);
                con.setRequestProperty("User-Agent", "Erethon-Aether/1.0");
                con.setRequestProperty("Authorization", "Bearer " + auth);
                con.setRequestProperty("Content-Type", "application/json");

                JSONObject jsonBody = new JSONObject();
                jsonBody.put("url", request.linkUrl);

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

                    // Success - clear retry count
                    retryCount.remove(request.linkUrl);
                    skinsInQueue.remove(request.linkUrl);

                    Aether.log("Successfully fetched skin: " + request.linkUrl);
                    request.callback.call(new Skin(request.linkUrl, textureEncoded, signature));
                } else {
                    // Read error response
                    reader = new BufferedReader(new InputStreamReader(con.getErrorStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    handleError(request, response.toString(), responseCode);
                }
                con.disconnect();
            } catch (IOException | ParseException exception) {
                Bukkit.getLogger().warning("Exception while fetching skin (Attempt " + request.attempt + "/" + MAX_RETRIES + "): " + exception.getMessage());
                handleError(request, exception.getMessage(), -1);
            } finally {
                try {
                    if (out != null) out.close();
                    if (reader != null) reader.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        });
    }

    private static void handleError(PendingRequest request, String errorMessage, int responseCode) {
        int attempts = retryCount.getOrDefault(request.linkUrl, 0) + 1;
        retryCount.put(request.linkUrl, attempts);

        // Try to parse delay from error response
        long retryDelayMs = MIN_REQUEST_DELAY_MS;
        try {
            JSONObject errorJson = (JSONObject) new JSONParser().parse(errorMessage);
            if (errorJson.containsKey("delay")) {
                Object delayObj = errorJson.get("delay");
                if (delayObj instanceof Number) {
                    retryDelayMs = Math.max(((Number) delayObj).longValue() * 1000L, MIN_REQUEST_DELAY_MS);
                }
            } else if (errorJson.containsKey("nextRequest")) {
                Object nextReqObj = errorJson.get("nextRequest");
                if (nextReqObj instanceof Number) {
                    retryDelayMs = Math.max(((Number) nextReqObj).longValue() * 1000L, MIN_REQUEST_DELAY_MS);
                }
            }
            String errorType = (String) errorJson.get("errorType");
            if ("rate_limit".equals(errorJson.get("errorCode"))) {
                Bukkit.getLogger().info("Rate limited for skin " + request.linkUrl + ", will retry in " + (retryDelayMs / 1000) + " seconds");
            } else {
                Bukkit.getLogger().warning("Error fetching skin (Attempt " + attempts + "/" + MAX_RETRIES + "): " + errorMessage);
            }
        } catch (Exception e) {
            // Not JSON or parsing failed, use exponential backoff
            retryDelayMs = MIN_REQUEST_DELAY_MS * (1L << Math.min(attempts - 1, 4)); // Cap at 48 seconds
            Bukkit.getLogger().warning("Error fetching skin (Attempt " + attempts + "/" + MAX_RETRIES + "): " + errorMessage);
        }

        if (attempts < MAX_RETRIES) {
            // Schedule retry
            long retryTime = System.currentTimeMillis() + retryDelayMs;
            PendingRequest retryRequest = new PendingRequest(request.linkUrl, request.callback, retryTime, attempts + 1);
            pendingQueue.offer(retryRequest);
            Aether.log("Scheduled retry for " + request.linkUrl + " in " + (retryDelayMs / 1000) + " seconds (attempt " + (attempts + 1) + "/" + MAX_RETRIES + ")");
        } else {
            // Max retries exceeded
            Bukkit.getLogger().severe("Failed to fetch skin after " + MAX_RETRIES + " attempts: " + request.linkUrl);
            retryCount.remove(request.linkUrl);
            skinsInQueue.remove(request.linkUrl);
            request.callback.failed(request.linkUrl);
        }
    }

    public static void fetchSkinFromIdAsync(String linkUrl, Callback callback) {
        if (linkUrl == null) {
            Aether.log("Trying to fetch invalid skin for NPC.");
            return;
        }
        if (skinsInQueue.contains(linkUrl)) {
            Aether.log("Skin already in queue: " + linkUrl);
            return;
        }
        Aether plugin = Aether.getInstance();
        String auth = plugin.getSkinCache().getAuthToken();
        if (auth == null || auth.isEmpty()) {
            Aether.log("No MineSkin auth token found.");
            return;
        }

        skinsInQueue.add(linkUrl);
        PendingRequest request = new PendingRequest(linkUrl, callback, System.currentTimeMillis(), 1);
        pendingQueue.offer(request);
        Aether.log("Queued skin fetch request: " + linkUrl + " (Queue size: " + pendingQueue.size() + ")");
    }

    public interface Callback {

        void call(Skin skinData);

        default void failed(String url) {
            // Override if needed
        }
    }
}