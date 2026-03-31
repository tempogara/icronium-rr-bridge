package it.icron.icronium.connector.rr.integration;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RRSessionService {

    private final RRSessionContext sessionContext;

    public RRSessionService(RRSessionContext sessionContext) {
        this.sessionContext = sessionContext;
    }

    public void loginRemote(String userId, String password) throws Exception {
        sessionContext.clear();
        RaceResultClient rr = sessionContext.getRaceResultClient();
        rr.login(userId, password);
        String rrPw = rr.getPw();

        if (rrPw == null || rrPw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login RR fallito: sessione non ottenuta");
        }

        sessionContext.markRemoteLogin(userId, rrPw);
    }

    public void loginLocal() {
        sessionContext.clear();
        sessionContext.markLocalLogin("local-user");
    }

    public void logout() {
        sessionContext.clear();
    }

    public void requireAuthenticated() {
        if (!sessionContext.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessione non autenticata");
        }
    }

    public boolean isLocalMode() {
        return "RR_LOCALE".equals(sessionContext.getMode());
    }

    public String getUserId() {
        return sessionContext.getUserId();
    }

    public String getMode() {
        return sessionContext.getMode();
    }

    public String getRrPw() {
        return sessionContext.getRrPw();
    }

    public RaceResultClient getClient() {
        return sessionContext.getRaceResultClient();
    }

    public String buildRrEventUrl(String eventId) {
        requireAuthenticated();
        if (eventId == null || eventId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "eventId is required");
        }
        String cleanEventId = eventId.trim();
        if (isLocalMode()) {
            return "http://localhost/_" + cleanEventId + "/?lang=en&pw=0";
        }
        return "https://events.raceresult.com/";
    }

    public void saveSyncData(String eventId, RRGaraSyncData data) {
        sessionContext.saveSyncData(eventId, data);
    }

    public RRGaraSyncData getSyncData(String eventId) {
        return sessionContext.getSyncData(eventId);
    }
}
