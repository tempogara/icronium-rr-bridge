package it.icron.icronium.connector.rr.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.icron.icronium.connector.rr.model.SpeakerAccessToken;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SpeakerTokenService {

    private final ObjectMapper objectMapper;
    private final Path storagePath = Paths.get("data", "speaker-tokens.json");

    public SpeakerTokenService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public synchronized String issueToken(String eventId, String timingPoint, String mode) throws IOException {
        List<SpeakerAccessToken> tokens = loadTokens();
        Optional<SpeakerAccessToken> existing = tokens.stream()
                .filter(item -> safeEquals(item.getEventId(), eventId))
                .filter(item -> safeEquals(item.getTimingPoint(), timingPoint))
                .filter(item -> safeEquals(item.getMode(), mode))
                .findFirst();
        if (existing.isPresent()) {
            return existing.get().getToken();
        }
        SpeakerAccessToken token = new SpeakerAccessToken();
        token.setToken(UUID.randomUUID().toString().replace("-", ""));
        token.setEventId(eventId);
        token.setTimingPoint(timingPoint);
        token.setMode(mode);
        token.setCreatedAt(System.currentTimeMillis());
        tokens.add(token);
        saveTokens(tokens);
        return token.getToken();
    }

    public synchronized Optional<SpeakerAccessToken> findByToken(String token) throws IOException {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return loadTokens().stream()
                .filter(item -> token.equals(item.getToken()))
                .findFirst();
    }

    private List<SpeakerAccessToken> loadTokens() throws IOException {
        if (!Files.exists(storagePath)) {
            return new ArrayList<>();
        }
        return objectMapper.readValue(storagePath.toFile(), new TypeReference<>() {});
    }

    private void saveTokens(List<SpeakerAccessToken> tokens) throws IOException {
        Files.createDirectories(storagePath.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(storagePath.toFile(), tokens);
    }

    private boolean safeEquals(String left, String right) {
        return (left == null ? "" : left).equals(right == null ? "" : right);
    }
}
