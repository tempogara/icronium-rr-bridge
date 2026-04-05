package it.icron.icronium.connector.rr.controller;

import it.icron.icronium.connector.rr.integration.RRGaraDettaglioService;
import it.icron.icronium.connector.rr.model.BibChipResponse;
import it.icron.icronium.connector.rr.model.FilePassaggiRequest;
import it.icron.icronium.connector.rr.model.GaraDettaglioResponse;
import it.icron.icronium.connector.rr.model.GaraDettaglioRow;
import it.icron.icronium.connector.rr.model.GaraSyncResponse;
import it.icron.icronium.connector.rr.model.LocalFileRequest;
import it.icron.icronium.connector.rr.model.RemoteFileRequest;
import it.icron.icronium.connector.rr.model.TimingPointResponse;
import it.icron.icronium.connector.rr.model.UpdateTimingPointRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/gare")
public class GaraDettaglioController {

    private final RRGaraDettaglioService rrGaraDettaglioService;

    public GaraDettaglioController(RRGaraDettaglioService rrGaraDettaglioService) {
        this.rrGaraDettaglioService = rrGaraDettaglioService;
    }

    @GetMapping("/{eventId}/dettaglio")
    public ResponseEntity<GaraDettaglioResponse> dettaglio(@PathVariable String eventId) {
        try {
            return ResponseEntity.ok(rrGaraDettaglioService.loadDettaglio(eventId));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(e.getStatusCode().value())).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{eventId}/sync")
    public ResponseEntity<GaraSyncResponse> sync(@PathVariable String eventId) {
        try {
            return ResponseEntity.ok(rrGaraDettaglioService.sync(eventId));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(e.getStatusCode().value())).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{eventId}/timing-points")
    public ResponseEntity<TimingPointResponse> timingPoints(@PathVariable String eventId) {
        try {
            return ResponseEntity.ok(rrGaraDettaglioService.getTimingPoints(eventId));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(e.getStatusCode().value())).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{eventId}/bib-chip")
    public ResponseEntity<BibChipResponse> bibChip(@PathVariable String eventId) {
        try {
            return ResponseEntity.ok(rrGaraDettaglioService.getBibChip(eventId));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(e.getStatusCode().value())).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{eventId}/rows")
    public ResponseEntity<List<GaraDettaglioRow>> rows(@PathVariable String eventId) {
        try {
            return ResponseEntity.ok(rrGaraDettaglioService.getPersistedRows(eventId));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(e.getStatusCode().value())).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{eventId}/rows/{rowId}/content")
    public ResponseEntity<String> rowContent(@PathVariable String eventId, @PathVariable String rowId) {
        try {
            return ResponseEntity.ok(rrGaraDettaglioService.getRowFileContent(eventId, rowId));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(e.getStatusCode().value())).body(e.getReason());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Errore lettura file");
        }
    }

    @PostMapping("/{eventId}/remote-file")
    public ResponseEntity<List<GaraDettaglioRow>> addRemoteFile(@PathVariable String eventId, @RequestBody RemoteFileRequest request) {
        try {
            return ResponseEntity.ok(rrGaraDettaglioService.addRemoteFile(eventId, request));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(e.getStatusCode().value())).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{eventId}/local-file")
    public ResponseEntity<List<GaraDettaglioRow>> addLocalFile(@PathVariable String eventId, @RequestBody LocalFileRequest request) {
        try {
            return ResponseEntity.ok(rrGaraDettaglioService.addLocalFile(eventId, request));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(e.getStatusCode().value())).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{eventId}/file-passaggi")
    public ResponseEntity<List<GaraDettaglioRow>> addFilePassaggi(@PathVariable String eventId, @RequestBody FilePassaggiRequest request) {
        try {
            return ResponseEntity.ok(rrGaraDettaglioService.addFilePassaggi(eventId, request));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(e.getStatusCode().value())).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{eventId}/rows/{rowId}")
    public ResponseEntity<List<GaraDettaglioRow>> deleteRow(@PathVariable String eventId, @PathVariable String rowId) {
        try {
            return ResponseEntity.ok(rrGaraDettaglioService.deleteRow(eventId, rowId));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(e.getStatusCode().value())).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{eventId}/rows")
    public ResponseEntity<List<GaraDettaglioRow>> deleteAllRows(@PathVariable String eventId) {
        try {
            return ResponseEntity.ok(rrGaraDettaglioService.deleteAllRows(eventId));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(e.getStatusCode().value())).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{eventId}/rows/{rowId}/actions/{action}")
    public ResponseEntity<List<GaraDettaglioRow>> rowAction(@PathVariable String eventId, @PathVariable String rowId, @PathVariable String action) {
        try {
            return ResponseEntity.ok(rrGaraDettaglioService.applyActionToRow(eventId, rowId, action));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(e.getStatusCode().value())).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{eventId}/actions/{action}")
    public ResponseEntity<List<GaraDettaglioRow>> globalAction(@PathVariable String eventId, @PathVariable String action) {
        try {
            return ResponseEntity.ok(rrGaraDettaglioService.applyActionToAll(eventId, action));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(e.getStatusCode().value())).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{eventId}/rows/{rowId}/timing-point")
    public ResponseEntity<List<GaraDettaglioRow>> updateTimingPoint(@PathVariable String eventId, @PathVariable String rowId, @RequestBody UpdateTimingPointRequest request) {
        try {
            return ResponseEntity.ok(rrGaraDettaglioService.updateRowTimingPoint(eventId, rowId, request));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(e.getStatusCode().value())).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{eventId}/rows/{rowId}/duplicate")
    public ResponseEntity<List<GaraDettaglioRow>> duplicateRow(@PathVariable String eventId, @PathVariable String rowId) {
        try {
            return ResponseEntity.ok(rrGaraDettaglioService.duplicateRow(eventId, rowId));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(e.getStatusCode().value())).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{eventId}/rows/{rowId}/move/{direction}")
    public ResponseEntity<List<GaraDettaglioRow>> moveRow(@PathVariable String eventId, @PathVariable String rowId, @PathVariable String direction) {
        try {
            return ResponseEntity.ok(rrGaraDettaglioService.moveRow(eventId, rowId, direction));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(e.getStatusCode().value())).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{eventId}/rows/{rowId}/relink-local")
    public ResponseEntity<List<GaraDettaglioRow>> relinkLocal(@PathVariable String eventId, @PathVariable String rowId) {
        try {
            return ResponseEntity.ok(rrGaraDettaglioService.relinkRowToLocal(eventId, rowId));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(e.getStatusCode().value())).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{eventId}/rows/{rowId}/blacklist")
    public ResponseEntity<List<GaraDettaglioRow>> blacklistCode(@PathVariable String eventId, @PathVariable String rowId, @RequestBody String code) {
        try {
            return ResponseEntity.ok(rrGaraDettaglioService.addCodeToBlacklist(eventId, rowId, code));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(e.getStatusCode().value())).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{eventId}/rows/{rowId}/blacklist")
    public ResponseEntity<List<GaraDettaglioRow>> removeBlacklistCode(@PathVariable String eventId, @PathVariable String rowId, @RequestBody String code) {
        try {
            return ResponseEntity.ok(rrGaraDettaglioService.removeCodeFromBlacklist(eventId, rowId, code));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(e.getStatusCode().value())).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
