package com.squarepeace.nnppss.model;

public class DownloadState {
    private String title;
    private String region;
    private String console;
    private String pkgUrl;
    private String zRif;
    private long bytesDownloaded;
    private long totalBytes;
    private String filePath;
    private boolean paused;
    private String status;
    private long lastUpdateTimestamp;

    public DownloadState() {
    }

    public DownloadState(String title, String region, String console, String pkgUrl, String zRif,
                        long bytesDownloaded, long totalBytes, String filePath, boolean paused,
                        String status, long lastUpdateTimestamp) {
        this.title = title;
        this.region = region;
        this.console = console;
        this.pkgUrl = pkgUrl;
        this.zRif = zRif;
        this.bytesDownloaded = bytesDownloaded;
        this.totalBytes = totalBytes;
        this.filePath = filePath;
        this.paused = paused;
        this.status = status;
        this.lastUpdateTimestamp = lastUpdateTimestamp;
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

    public String getConsole() {
        return console;
    }

    public void setConsole(String console) {
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

    public long getBytesDownloaded() {
        return bytesDownloaded;
    }

    public void setBytesDownloaded(long bytesDownloaded) {
        this.bytesDownloaded = bytesDownloaded;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public void setTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getLastUpdateTimestamp() {
        return lastUpdateTimestamp;
    }

    public void setLastUpdateTimestamp(long lastUpdateTimestamp) {
        this.lastUpdateTimestamp = lastUpdateTimestamp;
    }

    @Override
    public String toString() {
        return "DownloadState{" +
                "title='" + title + '\'' +
                ", pkgUrl='" + pkgUrl + '\'' +
                ", bytesDownloaded=" + bytesDownloaded +
                ", totalBytes=" + totalBytes +
                ", paused=" + paused +
                ", status='" + status + '\'' +
                '}';
    }
}
