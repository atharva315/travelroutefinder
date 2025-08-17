package com.atharva;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class TravelRouteBot extends TelegramLongPollingBot {
    private final Map<Long, List<String>> userInputs = new HashMap<>();

    @Override
    public String getBotUsername() {
        return "Rockstar34_bot";
    }

    @Override
    public String getBotToken() {
        return "8404623779:AAHLslNarWDYdJqGbweko8Mgj3iGj26RhR8";
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            String message = update.getMessage().getText();

            if (message.equalsIgnoreCase("/start")) {
                sendMsg(chatId, "Welcome! Send your 2D matrix of distances row by row, each row comma-separated.\nType 'done' when finished.");
                userInputs.put(chatId, new ArrayList<>());
            } else if (message.equalsIgnoreCase("done")) {
                List<String> inputRows = userInputs.getOrDefault(chatId, new ArrayList<>());
                int[][] matrix = parseMatrix(inputRows);

                String tspResult = solveTSP(matrix);

                sendMsg(chatId, tspResult);
                userInputs.remove(chatId);
            } else {
                userInputs.computeIfAbsent(chatId, k -> new ArrayList<>()).add(message);
                sendMsg(chatId, "Row saved! Send next row, or type 'done' to process.");
            }
        }
    }

    private int[][] parseMatrix(List<String> rows) {
        int n = rows.size();
        int[][] matrix = new int[n][];
        for (int i = 0; i < n; i++) {
            String[] nums = rows.get(i).split(",");
            matrix[i] = Arrays.stream(nums).map(String::trim).mapToInt(Integer::parseInt).toArray();
        }
        return matrix;
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

    private String solveTSP(int[][] matrix) {
        int n = matrix.length;
        boolean[] visited = new boolean[n];
        List<Integer> path = new ArrayList<>();
        int[] minCost = {Integer.MAX_VALUE};
        List<Integer> bestPath = new ArrayList<>();

        tspHelper(matrix, 0, visited, 0, 1, path, bestPath, minCost);

        return "Optimal route: " + bestPath + "\nTotal cost: " + minCost[0];
    }

    private void tspHelper(int[][] matrix, int pos, boolean[] visited, int cost, int count,
                           List<Integer> path, List<Integer> bestPath, int[] minCost) {
        int n = matrix.length;
        visited[pos] = true;
        path.add(pos);

        if (count == n) {
            int completeCost = cost + matrix[pos][0]; // return to start
            if (completeCost < minCost[0]) {
                minCost[0] = completeCost;
                bestPath.clear();
                bestPath.addAll(new ArrayList<>(path));
                bestPath.add(0); // return to start
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
