package com.squarepeace.nnppss.model;

/**
 * Represents a download history entry
 */
public class DownloadHistory {
    private String pkgUrl;
    private String title;
    private String console;
    private String status; // "completed", "failed", "paused"
    private long downloadDate;
    private String filePath;

    public DownloadHistory() {
    }

    public DownloadHistory(String pkgUrl, String title, String console, String status, long downloadDate, String filePath) {
        this.pkgUrl = pkgUrl;
        this.title = title;
        this.console = console;
        this.status = status;
        this.downloadDate = downloadDate;
        this.filePath = filePath;
    }

    public String getPkgUrl() {
        return pkgUrl;
    }

    public void setPkgUrl(String pkgUrl) {
        this.pkgUrl = pkgUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getConsole() {
        return console;
    }

    public void setConsole(String console) {
        this.console = console;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getDownloadDate() {
        return downloadDate;
    }

    public void setDownloadDate(long downloadDate) {
        this.downloadDate = downloadDate;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
