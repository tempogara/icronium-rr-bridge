package it.icron.icronium.connector.rr.controller;

import it.icron.icronium.connector.rr.integration.RRSessionService;
import it.icron.icronium.connector.rr.integration.TZeroConfigService;
import it.icron.icronium.connector.rr.model.LoginRequest;
import it.icron.icronium.connector.rr.model.LoginResponse;
import it.icron.icronium.connector.rr.model.TZeroConfigRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RRSessionService sessionService;
    private final TZeroConfigService tZeroConfigService;

    public AuthController(RRSessionService sessionService, TZeroConfigService tZeroConfigService) {
        this.sessionService = sessionService;
        this.tZeroConfigService = tZeroConfigService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        if (request.getUserId() == null || request.getUserId().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new LoginResponse(false, "RR", null, "userId e password sono obbligatori"));
        }

        try {
            sessionService.loginRemote(request.getUserId(), request.getPassword());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponse(false, "RR", null, "Login RR fallito: " + e.getMessage()));
        }

        LoginResponse response = new LoginResponse(
                true,
                "RR",
                request.getUserId(),
                "Login RR effettuato con successo."
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login-local")
    public ResponseEntity<LoginResponse> loginLocal() {
        sessionService.loginLocal();

        LoginResponse response = new LoginResponse(
                true,
                "RR_LOCALE",
                sessionService.getUserId(),
                "Login RR locale effettuato con successo."
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login-tzero")
    public ResponseEntity<LoginResponse> loginTZero(@RequestBody TZeroConfigRequest request) {
        try {
            String savedRoot = tZeroConfigService.saveRootFolder(request == null ? null : request.getRootFolder());
            sessionService.loginTZero(savedRoot);
            return ResponseEntity.ok(new LoginResponse(
                    true,
                    "TZERO",
                    sessionService.getUserId(),
                    "Login TZero effettuato con successo."
            ));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new LoginResponse(false, "TZERO", null, e.getReason() == null ? "Errore configurazione TZero" : e.getReason()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        sessionService.logout();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/keepalive")
    public ResponseEntity<Map<String, Object>> keepAlive() {
        try {
            sessionService.requireAuthenticated();
            return ResponseEntity.ok(Map.of(
                    "authenticated", true,
                    "userId", sessionService.getUserId(),
                    "mode", sessionService.getMode()
            ));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping("/rr-open-url/{eventId}")
    public ResponseEntity<Map<String, String>> rrOpenUrl(@PathVariable String eventId) {
        try {
            String url = sessionService.buildRrEventUrl(eventId);
            return ResponseEntity.ok(Map.of("url", url));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
