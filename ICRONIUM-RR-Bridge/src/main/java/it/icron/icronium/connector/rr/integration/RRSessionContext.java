package it.icron.icronium.connector.rr.integration;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RRSessionContext {

    private final RaceResultClient raceResultClient = new RaceResultClient();
    private String userId;
    private String mode;
    private String rrPw;
    private boolean authenticated;
    private final Map<String, RRGaraSyncData> syncDataByEventId = new HashMap<>();

    public RaceResultClient getRaceResultClient() {
        return raceResultClient;
    }

    public String getUserId() {
        return userId;
    }

    public String getMode() {
        return mode;
    }

    public String getRrPw() {
        return rrPw;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void markRemoteLogin(String userId, String rrPw) {
        this.userId = userId;
        this.mode = "RR";
        this.rrPw = rrPw;
        this.authenticated = true;
    }

    public void markLocalLogin(String userId) {
        this.userId = userId;
        this.mode = "RR_LOCALE";
        this.rrPw = "0";
        this.raceResultClient.setPw(this.rrPw);
        this.authenticated = true;
    }

    public void clear() {
        this.userId = null;
        this.mode = null;
        this.rrPw = null;
        this.authenticated = false;
        this.raceResultClient.setPw(null);
        this.syncDataByEventId.clear();
    }

    public void saveSyncData(String eventId, RRGaraSyncData data) {
        this.syncDataByEventId.put(eventId, data);
    }

    public RRGaraSyncData getSyncData(String eventId) {
        return this.syncDataByEventId.get(eventId);
    }
}
