package it.icron.icronium.connector.rr.controller;

import it.icron.icronium.connector.rr.integration.RRSessionService;
import it.icron.icronium.connector.rr.integration.TZeroConfigService;
import it.icron.icronium.connector.rr.model.TZeroConfigRequest;
import it.icron.icronium.connector.rr.model.TZeroTimingPoint;
import it.icron.icronium.connector.rr.model.TZeroTimingPointConfigResponse;
import it.icron.icronium.connector.rr.model.TZeroTimingPointRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
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

    @GetMapping("/gare/{eventId}/timing-points")
    public ResponseEntity<TZeroTimingPointConfigResponse> getTimingPoints(@PathVariable String eventId) {
        try {
            sessionService.requireAuthenticated();
            if (!sessionService.isTZeroMode()) {
                return ResponseEntity.badRequest().build();
            }
            List<TZeroTimingPoint> timingPoints = tZeroConfigService.loadTimingPoints(sessionService.getTZeroRootFolder(), eventId);
            return ResponseEntity.ok(new TZeroTimingPointConfigResponse(eventId, timingPoints));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(e.getStatusCode().value())).build();
        }
    }

    @PostMapping("/gare/{eventId}/timing-points")
    public ResponseEntity<TZeroTimingPointConfigResponse> createTimingPoint(@PathVariable String eventId, @RequestBody TZeroTimingPointRequest request) {
        try {
            sessionService.requireAuthenticated();
            if (!sessionService.isTZeroMode()) {
                return ResponseEntity.badRequest().build();
            }
            List<TZeroTimingPoint> timingPoints = tZeroConfigService.createOrUpdateTimingPoint(
                    sessionService.getTZeroRootFolder(),
                    eventId,
                    request == null ? null : request.getName(),
                    request == null ? null : request.getDescription()
            );
            return ResponseEntity.ok(new TZeroTimingPointConfigResponse(eventId, timingPoints));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(e.getStatusCode().value())).build();
        }
    }

    @DeleteMapping("/gare/{eventId}/timing-points/{name}")
    public ResponseEntity<TZeroTimingPointConfigResponse> deleteTimingPoint(@PathVariable String eventId, @PathVariable String name) {
        try {
            sessionService.requireAuthenticated();
            if (!sessionService.isTZeroMode()) {
                return ResponseEntity.badRequest().build();
            }
            List<TZeroTimingPoint> timingPoints = tZeroConfigService.deleteTimingPoint(sessionService.getTZeroRootFolder(), eventId, name);
            return ResponseEntity.ok(new TZeroTimingPointConfigResponse(eventId, timingPoints));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(e.getStatusCode().value())).build();
        }
    }
}
