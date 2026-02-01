package com.squarepeace.nnppss.model.download;

import java.io.Serializable;

/**
 * Represents a segment of a file to be downloaded.
 */
public class Segment implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private long startByte;
    private long endByte;
    private long currentOffset; // Relative to startByte or absolute? Let's keep it absolute position in file
    private volatile SegmentStatus status;
    private int retryCount;

    public Segment() {
    }

    public Segment(int id, long startByte, long endByte) {
        this.id = id;
        this.startByte = startByte;
        this.endByte = endByte;
        this.currentOffset = startByte;
        this.status = SegmentStatus.PENDING;
        this.retryCount = 0;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getStartByte() {
        return startByte;
    }

    public void setStartByte(long startByte) {
        this.startByte = startByte;
    }

    public long getEndByte() {
        return endByte;
    }

    public void setEndByte(long endByte) {
        this.endByte = endByte;
    }

    public long getCurrentOffset() {
        return currentOffset;
    }

    public void setCurrentOffset(long currentOffset) {
        this.currentOffset = currentOffset;
    }

    public SegmentStatus getStatus() {
        return status;
    }

    public void setStatus(SegmentStatus status) {
        this.status = status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
    
    public void incrementRetryCount() {
        this.retryCount++;
    }

    public long length() {
        return endByte - startByte + 1;
    }

    public long bytesDownloaded() {
        return currentOffset - startByte;
    }
    
    public long bytesRemaining() {
        return endByte - currentOffset + 1;
    }

    public boolean isFinished() {
        return currentOffset > endByte; // careful with this logic if currentOffset is pointer to next byte
    }
}
