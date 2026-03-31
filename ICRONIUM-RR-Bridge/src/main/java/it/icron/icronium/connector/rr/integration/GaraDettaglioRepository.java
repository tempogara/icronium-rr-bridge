package it.icron.icronium.connector.rr.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.icron.icronium.connector.rr.model.GaraDettaglioSnapshot;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Repository
public class GaraDettaglioRepository {

    private final ObjectMapper objectMapper;
    private final Path storageDir = Paths.get("data", "gara-dettaglio");

    public GaraDettaglioRepository(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public synchronized Optional<GaraDettaglioSnapshot> findByEventId(String eventId) throws IOException {
        Path path = fileForEvent(eventId);
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        return Optional.of(objectMapper.readValue(path.toFile(), GaraDettaglioSnapshot.class));
    }

    public synchronized void save(GaraDettaglioSnapshot snapshot) throws IOException {
        Files.createDirectories(storageDir);
        Path path = fileForEvent(snapshot.getEventId());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), snapshot);
    }

    public synchronized List<GaraDettaglioSnapshot> findAll() throws IOException {
        List<GaraDettaglioSnapshot> snapshots = new ArrayList<>();
        if (!Files.exists(storageDir)) {
            return snapshots;
        }
        try (Stream<Path> stream = Files.list(storageDir)) {
            stream.filter(path -> path.getFileName().toString().endsWith(".json"))
                    .forEach(path -> {
                        try {
                            snapshots.add(objectMapper.readValue(path.toFile(), GaraDettaglioSnapshot.class));
                        } catch (IOException ignored) {
                        }
                    });
        }
        return snapshots;
    }

    private Path fileForEvent(String eventId) {
        return storageDir.resolve(eventId + ".json");
    }
}
