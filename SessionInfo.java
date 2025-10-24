package com.example.godsteam.model;

import java.time.Instant;

public class SessionInfo {
    private final String sessionId;
    private boolean paid;
    private Object tx;
    private final Instant createdAt;

    public SessionInfo(String sessionId) {
        this.sessionId = sessionId;
        this.paid = false;
        this.tx = null;
        this.createdAt = Instant.now();
    }

    public String getSessionId() { return sessionId; }
    public boolean isPaid() { return paid; }
    public void setPaid(boolean paid) { this.paid = paid; }
    public Object getTx() { return tx; }
    public void setTx(Object tx) { this.tx = tx; }
    public Instant getCreatedAt() { return createdAt; }
}
