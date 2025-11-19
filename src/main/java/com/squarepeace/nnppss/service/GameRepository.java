package com.squarepeace.nnppss.service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
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

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
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
                    try {
                        game.setFileSize(Long.parseLong(fileSizeStr));
                    } catch (NumberFormatException e) {
                        game.setFileSize(0);
                    }
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
}
