package com.squarepeace.nnppss.model;

public enum Console {
    PSP("Psp", "db/PSP_GAMES.tsv"),
    PSVITA("Psvita", "db/PSV_GAMES.tsv"),
    PSX("Psx", "db/PSX_GAMES.tsv");

    private final String displayName;
    private final String dbPath;

    Console(String displayName, String dbPath) {
        this.displayName = displayName;
        this.dbPath = dbPath;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDbPath() {
        return dbPath;
    }

    public static Console fromDisplayName(String displayName) {
        for (Console c : values()) {
            if (c.displayName.equalsIgnoreCase(displayName)) {
                return c;
            }
        }
        throw new IllegalArgumentException("Unknown console: " + displayName);
    }
}
