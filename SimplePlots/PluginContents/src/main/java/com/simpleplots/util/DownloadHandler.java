package com.simpleplots.util;

import com.simpleplots.SimplePlots;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Handles uploading and deleting schematics via HTTP client.
 */
public class DownloadHandler {
    private final SimplePlots plugin;
    private final HttpClient httpClient;

    public DownloadHandler(SimplePlots plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public static class UploadResult {
        public final String downloadUrl;
        public final String deletionKey;

        public UploadResult(String downloadUrl, String deletionKey) {
            this.downloadUrl = downloadUrl;
            this.deletionKey = deletionKey;
        }
    }

    /**
     * Uploads a schematic file to the configured remote endpoint.
     */
    public CompletableFuture<UploadResult> uploadSchematic(File schemFile) {
        return CompletableFuture.supplyAsync(() -> {
            String endpoint = plugin.getConfig().getString("download.endpoint", "https://api.plotsquared.com/schematic");
            try {
                byte[] fileBytes = Files.readAllBytes(schemFile.toPath());
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .header("Content-Type", "application/octet-stream")
                        .header("File-Name", schemFile.getName())
                        .POST(HttpRequest.BodyPublishers.ofByteArray(fileBytes))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200 || response.statusCode() == 201) {
                    JSONParser parser = new JSONParser();
                    JSONObject json = (JSONObject) parser.parse(response.body());
                    String downloadUrl = (String) json.get("url");
                    String deletionKey = (String) json.get("key");
                    
                    // Fallbacks if json keys are different
                    if (downloadUrl == null) downloadUrl = (String) json.get("download_url");
                    if (deletionKey == null) deletionKey = (String) json.get("delete_key");

                    return new UploadResult(downloadUrl, deletionKey);
                } else {
                    plugin.getLogger().warning("Failed to upload schematic. Status code: " + response.statusCode() + ", Body: " + response.body());
                    return null;
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Exception occurred while uploading schematic: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        });
    }

    /**
     * Deletes an uploaded schematic from the remote endpoint using the deletion key.
     */
    public CompletableFuture<Boolean> deleteUploadedSchematic(String key) {
        return CompletableFuture.supplyAsync(() -> {
            String endpoint = plugin.getConfig().getString("download.endpoint", "https://api.plotsquared.com/schematic");
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint + "?key=" + key))
                        .DELETE()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                return response.statusCode() == 200;
            } catch (Exception e) {
                plugin.getLogger().severe("Exception occurred while deleting schematic: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        });
    }
}
