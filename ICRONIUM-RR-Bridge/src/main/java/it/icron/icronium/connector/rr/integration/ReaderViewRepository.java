package it.icron.icronium.connector.rr.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.icron.icronium.connector.rr.model.ReaderViewState;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Repository
public class ReaderViewRepository {

    private final ObjectMapper objectMapper;
    private final Path storageDir = Paths.get("data", "reader-view");

    public ReaderViewRepository(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public synchronized Optional<ReaderViewState> findByEventId(String eventId) throws IOException {
        Path path = fileForEvent(eventId);
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        return Optional.of(objectMapper.readValue(path.toFile(), ReaderViewState.class));
    }

    public synchronized void save(ReaderViewState state) throws IOException {
        Files.createDirectories(storageDir);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(fileForEvent(state.getEventId()).toFile(), state);
    }

    private Path fileForEvent(String eventId) {
        return storageDir.resolve(eventId + ".json");
    }
}
