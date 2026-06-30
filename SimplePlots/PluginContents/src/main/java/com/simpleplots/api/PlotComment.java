package com.simpleplots.api;

import java.util.UUID;

/**
 * Represents a visitor comment left on a plot.
 */
public class PlotComment {
    private final UUID commenterUuid;
    private final String commenterName;
    private final String commentText;
    private final long timestamp;

    public PlotComment(UUID commenterUuid, String commenterName, String commentText, long timestamp) {
        this.commenterUuid = commenterUuid;
        this.commenterName = commenterName;
        this.commentText = commentText;
        this.timestamp = timestamp;
    }

    public UUID getCommenterUuid() {
        return commenterUuid;
    }

    public String getCommenterName() {
        return commenterName;
    }

    public String getCommentText() {
        return commentText;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
