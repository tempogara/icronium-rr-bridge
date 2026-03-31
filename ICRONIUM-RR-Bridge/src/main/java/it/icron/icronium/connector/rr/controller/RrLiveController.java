package it.icron.icronium.connector.rr.controller;

import it.icron.icronium.connector.rr.integration.RrLiveService;
import it.icron.icronium.connector.rr.model.RrLiveState;
import it.icron.icronium.connector.rr.model.RrLiveSyncRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;

@RestController
@RequestMapping("/api/rr-live")
public class RrLiveController {

    private final RrLiveService service;

    public RrLiveController(RrLiveService service) {
        this.service = service;
    }

    @PostMapping("/sync")
    public ResponseEntity<RrLiveState> sync(@RequestBody RrLiveSyncRequest request) {
        try {
            return ResponseEntity.ok(service.sync(request));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(e.getStatusCode().value())).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/state/{eventId}")
    public ResponseEntity<RrLiveState> state(@PathVariable String eventId) {
        try {
            return ResponseEntity.ok(service.getState(eventId));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(e.getStatusCode().value())).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/start/{eventId}")
    public ResponseEntity<RrLiveState> start(@PathVariable String eventId) {
        try {
            return ResponseEntity.ok(service.start(eventId));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(e.getStatusCode().value())).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/stop/{eventId}")
    public ResponseEntity<RrLiveState> stop(@PathVariable String eventId) {
        try {
            return ResponseEntity.ok(service.stop(eventId));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(e.getStatusCode().value())).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/reset/{eventId}")
    public ResponseEntity<RrLiveState> reset(@PathVariable String eventId) {
        try {
            return ResponseEntity.ok(service.resetAll(eventId));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(e.getStatusCode().value())).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/rows/{eventId}/all")
    public ResponseEntity<RrLiveState> deleteAll(@PathVariable String eventId) {
        try {
            return ResponseEntity.ok(service.deleteAll(eventId));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(e.getStatusCode().value())).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{eventId}/rows/{rowKey}/actions/{action}")
    public ResponseEntity<RrLiveState> rowAction(@PathVariable String eventId, @PathVariable String rowKey, @PathVariable String action) {
        try {
            return ResponseEntity.ok(service.applyRowAction(eventId, rowKey, action));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(e.getStatusCode().value())).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{eventId}/rows/{rowKey}")
    public ResponseEntity<RrLiveState> deleteRow(@PathVariable String eventId, @PathVariable String rowKey) {
        try {
            return ResponseEntity.ok(service.deleteRow(eventId, rowKey));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(e.getStatusCode().value())).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
