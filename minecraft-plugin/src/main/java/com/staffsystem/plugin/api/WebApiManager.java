package com.staffsystem.plugin.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.staffsystem.plugin.StaffSystemPlugin;
import com.staffsystem.plugin.models.Punishment;

import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class WebApiManager {

    private final StaffSystemPlugin plugin;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final String apiUrl;
    private final String apiKey;

    public WebApiManager(StaffSystemPlugin plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
        this.apiUrl = plugin.getConfig().getString("web-integration.api-url", "http://localhost:3000/api");
        this.apiKey = plugin.getConfig().getString("web-integration.api-key", "");

        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

        plugin.getLogger().info("Web API Manager initialized. API URL: " + apiUrl);
    }

    public CompletableFuture<Boolean> sendPunishmentNotification(Punishment punishment) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("type", "punishment");
                json.addProperty("punishmentType", punishment.getType().name());
                json.addProperty("playerUuid", punishment.getPlayerUuid().toString());
                json.addProperty("playerName", punishment.getPlayerName());
                json.addProperty("staffUuid", punishment.getStaffUuid().toString());
                json.addProperty("staffName", punishment.getStaffName());
                json.addProperty("reason", punishment.getReason());
                json.addProperty("duration", punishment.getDuration());
                json.addProperty("timestamp", punishment.getTimestamp());
                json.addProperty("server", punishment.getServer());

                RequestBody body = RequestBody.create(
                    gson.toJson(json),
                    MediaType.parse("application/json; charset=utf-8")
                );

                Request request = new Request.Builder()
                    .url(apiUrl + "/webhook/punishment")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    return response.isSuccessful();
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to send punishment notification to web API: " + e.getMessage());
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> sendFreezeNotification(String playerName, String staffName, boolean frozen) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("type", "freeze");
                json.addProperty("playerName", playerName);
                json.addProperty("staffName", staffName);
                json.addProperty("frozen", frozen);
                json.addProperty("timestamp", System.currentTimeMillis());

                RequestBody body = RequestBody.create(
                    gson.toJson(json),
                    MediaType.parse("application/json; charset=utf-8")
                );

                Request request = new Request.Builder()
                    .url(apiUrl + "/webhook/freeze")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    return response.isSuccessful();
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to send freeze notification to web API: " + e.getMessage());
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> syncPunishment(Punishment punishment) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("id", punishment.getId());
                json.addProperty("playerUuid", punishment.getPlayerUuid().toString());
                json.addProperty("playerName", punishment.getPlayerName());
                json.addProperty("staffUuid", punishment.getStaffUuid().toString());
                json.addProperty("staffName", punishment.getStaffName());
                json.addProperty("type", punishment.getType().name());
                json.addProperty("reason", punishment.getReason());
                json.addProperty("timestamp", punishment.getTimestamp());
                json.addProperty("duration", punishment.getDuration());
                json.addProperty("expiration", punishment.getExpiration());
                json.addProperty("active", punishment.isActive());
                json.addProperty("server", punishment.getServer());

                RequestBody body = RequestBody.create(
                    gson.toJson(json),
                    MediaType.parse("application/json; charset=utf-8")
                );

                Request request = new Request.Builder()
                    .url(apiUrl + "/punishments/sync")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    return response.isSuccessful();
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to sync punishment to web API: " + e.getMessage());
                return false;
            }
        });
    }

    public void shutdown() {
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }
}
