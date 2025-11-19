package com.squarepeace.nnppss.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.squarepeace.nnppss.model.Console;
import com.squarepeace.nnppss.model.Game;

public class GameRepository {

    public List<Game> loadGames(Console console) throws IOException {
        List<Game> games = new ArrayList<>();
        String filePath = console.getDbPath();

        try (BufferedReader reader = Files.newBufferedReader(Path.of(filePath), StandardCharsets.UTF_8)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return games;
            }

            String[] headers = headerLine.split("\t");
            Map<String, Integer> columnMap = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                columnMap.put(headers[i], i);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                String[] data = line.split("\t");
                if (data.length == 0) continue;

                Game game = new Game();
                game.setConsole(console);

                // Helper to safely get value
                game.setTitle(getValue(data, columnMap, "Name"));
                game.setRegion(getValue(data, columnMap, "Region"));
                game.setPkgUrl(getValue(data, columnMap, "PKG direct link"));
                game.setzRif(getValue(data, columnMap, "zRIF"));
                game.setContentId(getValue(data, columnMap, "Content ID"));
                
                String fileSizeStr = getValue(data, columnMap, "File Size");
                if (fileSizeStr != null && !fileSizeStr.isEmpty()) {
                    game.setFileSize(parseSizeToBytes(fileSizeStr));
                }

                // Filter invalid games as per original logic (size > 0)
                if (game.getFileSize() > 0) {
                    games.add(game);
                }
            }
        }

        return games;
    }

    private String getValue(String[] data, Map<String, Integer> columnMap, String columnName) {
        Integer index = columnMap.get(columnName);
        if (index != null && index < data.length) {
            return data[index];
        }
        return null;
    }

    private long parseSizeToBytes(String text) {
        String s = text.trim();
        try {
            // Pure number => assume bytes
            if (s.matches("^\\d+$")) {
                return Long.parseLong(s);
            }
            // Number with unit
            s = s.replaceAll(",", ".");
            String[] parts = s.split("\\s+", 2);
            double value = Double.parseDouble(parts[0]);
            String unit = parts.length > 1 ? parts[1].toLowerCase() : "b";
            long mul = 1L;
            if (unit.startsWith("kb") || unit.startsWith("kib")) mul = 1024L;
            else if (unit.startsWith("mb") || unit.startsWith("mib")) mul = 1024L * 1024L;
            else if (unit.startsWith("gb") || unit.startsWith("gib")) mul = 1024L * 1024L * 1024L;
            else if (unit.startsWith("b")) mul = 1L;
            return (long) (value * mul);
        } catch (Exception ignore) {
            return 0L;
        }
    }
}
