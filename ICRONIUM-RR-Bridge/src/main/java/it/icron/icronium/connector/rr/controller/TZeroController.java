package it.icron.icronium.connector.rr.controller;

import it.icron.icronium.connector.rr.integration.RRSessionService;
import it.icron.icronium.connector.rr.integration.TZeroConfigService;
import it.icron.icronium.connector.rr.model.TZeroConfigRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/tzero")
public class TZeroController {

    private final TZeroConfigService tZeroConfigService;
    private final RRSessionService sessionService;

    public TZeroController(TZeroConfigService tZeroConfigService, RRSessionService sessionService) {
        this.tZeroConfigService = tZeroConfigService;
        this.sessionService = sessionService;
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> getConfig() {
        try {
            String rootFolder = tZeroConfigService.getRootFolder();
            Map<String, String> response = new HashMap<>();
            response.put("rootFolder", rootFolder == null ? "" : rootFolder);
            return ResponseEntity.ok(response);
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(e.getStatusCode().value())).build();
        }
    }

    @PostMapping("/config")
    public ResponseEntity<Map<String, String>> saveConfig(@RequestBody TZeroConfigRequest request) {
        try {
            String savedRoot = tZeroConfigService.saveRootFolder(request == null ? null : request.getRootFolder());
            if (sessionService.isTZeroMode()) {
                sessionService.updateTZeroRoot(savedRoot);
            }
            return ResponseEntity.ok(Map.of("rootFolder", savedRoot));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(e.getStatusCode().value())).body(Map.of("message", e.getReason() == null ? "Errore configurazione TZero" : e.getReason()));
        }
    }
}
