package it.icron.icronium.connector.rr.controller;

import it.icron.icronium.connector.rr.integration.RRGareService;
import it.icron.icronium.connector.rr.model.Gara;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/gare")
public class GareController {

    private final RRGareService rrGareService;

    public GareController(RRGareService rrGareService) {
        this.rrGareService = rrGareService;
    }

    @GetMapping
    public ResponseEntity<List<Gara>> elencoGare() {
        try {
            List<Gara> gare = rrGareService.loadGareFromRR();
            return ResponseEntity.ok(gare);
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(e.getStatusCode().value())).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
