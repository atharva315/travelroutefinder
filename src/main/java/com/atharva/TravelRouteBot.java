package com.atharva;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TravelRouteBot extends TelegramLongPollingBot {
    private final Map<Long, List<String>> userInputs = new HashMap<>();
    private static final String ORS_API_KEY = "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6IjFmZDExZTEwODZkMzQwNGU4ODllZjNmZTVhZDdjMGY5IiwiaCI6Im11cm11cjY0In0="; // Replace with your API key

    @Override
    public String getBotUsername() {
        return "Rockstar34_bot"; // use your bot username
    }

    @Override
    public String getBotToken() {
        return "8404623779:AAHLslNarWDYdJqGbweko8Mgj3iGj26RhR8"; // use your bot token
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            String message = update.getMessage().getText();

            if (message.equalsIgnoreCase("/start")) {
                sendMsg(chatId, "Welcome! Send city names one by one. Type 'done' when finished.");
                userInputs.put(chatId, new ArrayList<>());
            } else if (message.equalsIgnoreCase("done")) {
                List<String> cityNames = userInputs.getOrDefault(chatId, new ArrayList<>());
                if (cityNames.size() < 2) {
                    sendMsg(chatId, "Please enter at least two city names before typing 'done'.");
                    return;
                }
                try {
                    List<double[]> coordinates = new ArrayList<>();
                    for (String city : cityNames) {
                        double[] coord = geocodeCity(city);
                        coordinates.add(coord);
                    }
                    int[][] matrix = getDistanceMatrix(coordinates);
                    String tspResult = solveTSP(matrix, cityNames);
                    sendMsg(chatId, tspResult);
                } catch (Exception e) {
                    sendMsg(chatId, "API Error: " + e.getMessage());
                    e.printStackTrace();
                }
                userInputs.remove(chatId);
            } else {
                userInputs.computeIfAbsent(chatId, k -> new ArrayList<>()).add(message);
                sendMsg(chatId, "City saved! Send next city or type 'done'.");
            }
        }
    }

    // Geocode city name to [lon, lat]
    private double[] geocodeCity(String cityName) throws Exception {
        OkHttpClient client = new OkHttpClient();
        String url = "https://api.openrouteservice.org/geocode/search?api_key=" + ORS_API_KEY +
                "&text=" + URLEncoder.encode(cityName, StandardCharsets.UTF_8);
        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray features = root.getAsJsonArray("features");
            if (features.size() == 0) throw new Exception("City not found: " + cityName);
            JsonArray coords = features.get(0).getAsJsonObject()
                    .getAsJsonObject("geometry").getAsJsonArray("coordinates");
            return new double[]{coords.get(0).getAsDouble(), coords.get(1).getAsDouble()};
        }
    }

    // Build real-world distance matrix using ORS API (returns matrix in km units)
    private int[][] getDistanceMatrix(List<double[]> coordinates) throws Exception {
        OkHttpClient client = new OkHttpClient();

        JsonObject jsonBody = new JsonObject();
        JsonArray locs = new JsonArray();
        for (double[] coord : coordinates) {
            JsonArray arr = new JsonArray();
            arr.add(coord[0]); // lon
            arr.add(coord[1]); // lat
            locs.add(arr);
        }
        jsonBody.add("locations", locs);
        JsonArray metrics = new JsonArray();
        metrics.add("distance");
        jsonBody.add("metrics", metrics);

        Request request = new Request.Builder()
                .url("https://api.openrouteservice.org/v2/matrix/driving-car")
                .addHeader("Authorization", ORS_API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody.toString(), MediaType.parse("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            String respBody = response.body().string();
            JsonObject root = JsonParser.parseString(respBody).getAsJsonObject();
            JsonArray distArr = root.getAsJsonArray("distances");
            int n = coordinates.size();
            int[][] matrix = new int[n][n];
            for (int i = 0; i < n; i++) {
                JsonArray row = distArr.get(i).getAsJsonArray();
                for (int j = 0; j < n; j++) {
                    matrix[i][j] = (int) Math.round(row.get(j).getAsDouble() / 1000.0); // meters to km
                }
            }
            return matrix;
        }
    }

    private void sendMsg(long chatId, String text) {
        try {
            execute(org.telegram.telegrambots.meta.api.methods.send.SendMessage.builder()
                    .chatId(String.valueOf(chatId))
                    .text(text)
                    .build());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    // Updated solveTSP to display city names in order instead of indices
    private String solveTSP(int[][] matrix, List<String> cityNames) {
        int n = matrix.length;
        boolean[] visited = new boolean[n];
        List<Integer> path = new ArrayList<>();
        int[] minCost = {Integer.MAX_VALUE};
        List<Integer> bestPath = new ArrayList<>();

        tspHelper(matrix, 0, visited, 0, 1, path, bestPath, minCost);

        StringBuilder routeStr = new StringBuilder("Optimal route: ");
        for (int i = 0; i < bestPath.size(); i++) {
            routeStr.append(cityNames.get(bestPath.get(i)));
            if (i != bestPath.size() - 1) routeStr.append(" → ");
        }
        routeStr.append("\nTotal KM: ").append(minCost[0]).append(" km");
        return routeStr.toString();
    }

    private void tspHelper(int[][] matrix, int pos, boolean[] visited, int cost, int count,
                           List<Integer> path, List<Integer> bestPath, int[] minCost) {
        int n = matrix.length;
        visited[pos] = true;
        path.add(pos);

        if (count == n) {
            int completeCost = cost + matrix[pos][0];
            if (completeCost < minCost[0]) {
                minCost[0] = completeCost;
                bestPath.clear();
                bestPath.addAll(new ArrayList<>(path));
                bestPath.add(0);
            }
        } else {
            for (int i = 0; i < n; i++) {
                if (!visited[i] && matrix[pos][i] != 0) {
                    tspHelper(matrix, i, visited, cost + matrix[pos][i], count + 1, path, bestPath, minCost);
                }
            }
        }
        visited[pos] = false;
        path.remove(path.size() - 1);
    }

    public static void main(String[] args) throws Exception {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(new TravelRouteBot());
        System.out.println("Bot started!");
    }
}
