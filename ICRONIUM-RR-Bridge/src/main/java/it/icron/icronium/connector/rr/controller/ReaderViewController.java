package it.icron.icronium.connector.rr.controller;

import it.icron.icronium.connector.rr.integration.ReaderViewService;
import it.icron.icronium.connector.rr.model.ReaderConfigRequest;
import it.icron.icronium.connector.rr.model.ReaderFileContentResponse;
import it.icron.icronium.connector.rr.model.ReaderToRaceRequest;
import it.icron.icronium.connector.rr.model.ReaderViewState;
import it.icron.icronium.connector.rr.model.LanDiscoveryResponse;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/readers")
public class ReaderViewController {

    private final ReaderViewService readerViewService;

    public ReaderViewController(ReaderViewService readerViewService) {
        this.readerViewService = readerViewService;
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<ReaderViewState> load(@PathVariable String eventId) {
        try {
            return ResponseEntity.ok(readerViewService.loadState(eventId));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(e.getStatusCode().value())).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{eventId}/scan")
    public ResponseEntity<ReaderViewState> scan(@PathVariable String eventId) {
        try {
            return ResponseEntity.ok(readerViewService.scanNow(eventId));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(e.getStatusCode().value())).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{eventId}/discover-lan")
    public ResponseEntity<LanDiscoveryResponse> discoverLan(@PathVariable String eventId) {
        try {
            return ResponseEntity.ok(readerViewService.discoverLanReaders(eventId));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(e.getStatusCode().value())).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{eventId}/{readerId}/content")
    public ResponseEntity<ReaderFileContentResponse> content(@PathVariable String eventId, @PathVariable String readerId, @RequestParam("path") String path) {
        try {
            return ResponseEntity.ok(readerViewService.loadFileContent(eventId, readerId, path));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(e.getStatusCode().value())).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{eventId}/add-to-race")
    public ResponseEntity<String> addToRace(@PathVariable String eventId, @RequestBody ReaderToRaceRequest request) {
        try {
            return ResponseEntity.ok(readerViewService.materializeReaderFileForRace(eventId, request.getReaderId(), request.getFilePath()));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(e.getStatusCode().value())).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{eventId}")
    public ResponseEntity<ReaderViewState> create(@PathVariable String eventId, @RequestBody ReaderConfigRequest request) {
        try {
            return ResponseEntity.ok(readerViewService.saveReader(eventId, null, request));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(e.getStatusCode().value())).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{eventId}/{readerId}")
    public ResponseEntity<ReaderViewState> update(@PathVariable String eventId, @PathVariable String readerId, @RequestBody ReaderConfigRequest request) {
        try {
            return ResponseEntity.ok(readerViewService.saveReader(eventId, readerId, request));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(e.getStatusCode().value())).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{eventId}/{readerId}")
    public ResponseEntity<ReaderViewState> delete(@PathVariable String eventId, @PathVariable String readerId) {
        try {
            return ResponseEntity.ok(readerViewService.deleteReader(eventId, readerId));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(e.getStatusCode().value())).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{eventId}/{readerId}/move/{direction}")
    public ResponseEntity<ReaderViewState> move(@PathVariable String eventId, @PathVariable String readerId, @PathVariable String direction) {
        try {
            return ResponseEntity.ok(readerViewService.moveReader(eventId, readerId, direction));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(e.getStatusCode().value())).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
