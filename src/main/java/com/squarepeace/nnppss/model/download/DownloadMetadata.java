package com.squarepeace.nnppss.model.download;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Metadata for a segmented download, persisted to disk to allow resumption.
 */
public class DownloadMetadata implements Serializable {
    private static final long serialVersionUID = 1L;

    private String url;
    private String destinationPath;
    private long totalSize;
    private String etag;
    private long lastModified;
    private boolean resumable;
    private List<Segment> segments;
    private boolean completed;

    public DownloadMetadata() {
        this.segments = new ArrayList<>();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    public void setDestinationPath(String destinationPath) {
        this.destinationPath = destinationPath;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public boolean isResumable() {
        return resumable;
    }

    public void setResumable(boolean resumable) {
        this.resumable = resumable;
    }

    public List<Segment> getSegments() {
        return segments;
    }

    public void setSegments(List<Segment> segments) {
        this.segments = segments;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
    
    public long getTotalBytesDownloaded() {
        if (segments == null) return 0;
        return segments.stream().mapToLong(Segment::bytesDownloaded).sum();
    }
}
