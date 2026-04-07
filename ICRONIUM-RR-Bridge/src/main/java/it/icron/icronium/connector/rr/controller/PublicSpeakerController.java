package it.icron.icronium.connector.rr.controller;

import it.icron.icronium.connector.rr.integration.PublicSpeakerService;
import it.icron.icronium.connector.rr.model.PublicSpeakerResponse;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicSpeakerController {

    private final PublicSpeakerService publicSpeakerService;

    public PublicSpeakerController(PublicSpeakerService publicSpeakerService) {
        this.publicSpeakerService = publicSpeakerService;
    }

    @GetMapping("/speaker/{token}")
    public ResponseEntity<PublicSpeakerResponse> speaker(@PathVariable String token) {
        try {
            return ResponseEntity.ok(publicSpeakerService.loadByToken(token));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(e.getStatusCode().value())).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
