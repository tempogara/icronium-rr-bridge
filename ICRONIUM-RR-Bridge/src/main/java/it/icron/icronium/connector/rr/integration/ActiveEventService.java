package it.icron.icronium.connector.rr.integration;

import org.springframework.stereotype.Service;

@Service
public class ActiveEventService {

    private volatile String activeEventId;

    public String getActiveEventId() {
        return activeEventId;
    }

    public void setActiveEventId(String activeEventId) {
        this.activeEventId = activeEventId == null ? null : activeEventId.trim();
    }
}
