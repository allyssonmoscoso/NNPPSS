package com.squarepeace.nnppss.model;

public class Game {
    private String title;
    private String region;
    private Console console;
    private String pkgUrl;
    private String zRif;
    private String contentId;
    private long fileSize;
    private String lastModification;

    public Game() {
    }

    public Game(String title, String region, Console console, String pkgUrl, String zRif, long fileSize) {
        this.title = title;
        this.region = region;
        this.console = console;
        this.pkgUrl = pkgUrl;
        this.zRif = zRif;
        this.fileSize = fileSize;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Console getConsole() {
        return console;
    }

    public void setConsole(Console console) {
        this.console = console;
    }

    public String getPkgUrl() {
        return pkgUrl;
    }

    public void setPkgUrl(String pkgUrl) {
        this.pkgUrl = pkgUrl;
    }

    public String getzRif() {
        return zRif;
    }

    public void setzRif(String zRif) {
        this.zRif = zRif;
    }

    public String getContentId() {
        return contentId;
    }

    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getLastModification() {
        return lastModification;
    }

    public void setLastModification(String lastModification) {
        this.lastModification = lastModification;
    }

    public String getFileName() {
        if (pkgUrl == null || pkgUrl.isEmpty()) {
            return null;
        }
        return pkgUrl.substring(pkgUrl.lastIndexOf("/") + 1);
    }

    @Override
    public String toString() {
        return title + " (" + region + ")";
    }
}
