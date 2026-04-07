package it.icron.icronium.connector.rr.integration;

import it.icron.icronium.connector.rr.model.BibChipRow;
import it.icron.icronium.connector.rr.model.GaraDettaglioRow;
import it.icron.icronium.connector.rr.model.GaraDettaglioSnapshot;
import it.icron.icronium.connector.rr.model.PublicSpeakerEntry;
import it.icron.icronium.connector.rr.model.PublicSpeakerMinuteBucket;
import it.icron.icronium.connector.rr.model.PublicSpeakerResponse;
import it.icron.icronium.connector.rr.model.SpeakerAccessToken;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PublicSpeakerService {

    private static final DateTimeFormatter OUTPUT_MINUTE_LABEL = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final SpeakerTokenService speakerTokenService;
    private final GaraDettaglioRepository repository;

    public PublicSpeakerService(SpeakerTokenService speakerTokenService, GaraDettaglioRepository repository) {
        this.speakerTokenService = speakerTokenService;
        this.repository = repository;
    }

    public PublicSpeakerResponse loadByToken(String token) throws Exception {
        SpeakerAccessToken accessToken = speakerTokenService.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Token speaker non valido"));
        GaraDettaglioSnapshot snapshot = repository.findByEventId(accessToken.getEventId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gara non trovata"));

        String timingPoint = accessToken.getTimingPoint() == null ? "" : accessToken.getTimingPoint();
        List<GaraDettaglioRow> groupRows = (snapshot.getRows() == null ? List.<GaraDettaglioRow>of() : snapshot.getRows()).stream()
                .filter(row -> safe(row.getTimingPoint()).equals(timingPoint))
                .toList();

        Map<String, BibChipRow> athleteMap = new HashMap<>();
        Set<String> knownChips = new HashSet<>();
        for (BibChipRow row : snapshot.getBibChipRows() == null ? List.<BibChipRow>of() : snapshot.getBibChipRows()) {
            String chip = safe(row.getChip()).trim();
            if (!chip.isBlank()) {
                athleteMap.put(chip, row);
                knownChips.add(chip);
            }
        }

        List<PublicSpeakerEntry> entries = new ArrayList<>();
        Map<LocalDateTime, Integer> minuteMap = new LinkedHashMap<>();
        int discarded = 0;

        for (GaraDettaglioRow row : groupRows) {
            Set<String> blacklisted = new HashSet<>(row.getBlacklistedCodes() == null ? List.of() : row.getBlacklistedCodes());
            Path path = resolvePreviewPath(safe(row.getSource()));
            if (!Files.exists(path)) {
                continue;
            }
            try (BufferedReader reader = Files.newBufferedReader(path)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String raw = safe(line).trim();
                    if (raw.isBlank() || raw.contains("*")) {
                        continue;
                    }
                    String chip = extractChip(raw);
                    if (chip != null && blacklisted.contains(chip)) {
                        continue;
                    }
                    String timestamp = extractTimestamp(raw);
                    BibChipRow meta = chip == null ? null : athleteMap.get(chip);
                    PublicSpeakerEntry entry = new PublicSpeakerEntry();
                    entry.setChip(chip == null ? "" : chip);
                    entry.setBib(meta == null ? "" : String.valueOf(meta.getBib()).trim());
                    entry.setAthlete(buildAthleteName(meta, chip));
                    entry.setTeam(meta == null ? "" : resolveTeam(meta));
                    entry.setTimestamp(timestamp);
                    entries.add(entry);
                    LocalDateTime minute = parseTimestampToMinute(timestamp);
                    if (minute != null) {
                        minuteMap.put(minute, minuteMap.getOrDefault(minute, 0) + 1);
                    }
                    if (chip != null && !chip.isBlank() && !knownChips.contains(chip)) {
                        discarded++;
                    }
                }
            }
        }

        entries.sort(Comparator.comparing(PublicSpeakerEntry::getTimestamp).reversed());

        List<PublicSpeakerEntry> uniqueRecent = collectUniqueRecentEntries(entries, 10);
        Set<String> uniqueChips = new HashSet<>();
        for (PublicSpeakerEntry entry : entries) {
            String key = safe(entry.getBib()).isBlank() ? safe(entry.getChip()) : safe(entry.getBib());
            if (!key.isBlank()) {
                uniqueChips.add(key);
            }
        }

        List<PublicSpeakerMinuteBucket> buckets = minuteMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(item -> {
                    PublicSpeakerMinuteBucket bucket = new PublicSpeakerMinuteBucket();
                    bucket.setLabel(item.getKey().toString());
                    bucket.setDisplayLabel(item.getKey().format(OUTPUT_MINUTE_LABEL));
                    bucket.setCount(item.getValue());
                    return bucket;
                })
                .toList();

        PublicSpeakerResponse response = new PublicSpeakerResponse();
        response.setEventId(accessToken.getEventId());
        response.setTimingPoint(timingPoint);
        response.setMode(safe(accessToken.getMode()));
        response.setSourceCount(groupRows.size());
        response.setRead(entries.size());
        response.setSent(isTZeroMode(accessToken.getMode()) ? 0 : groupRows.stream().mapToInt(row -> Math.max(0, row.getRighePo())).sum());
        response.setDiscarded(discarded);
        response.setUnique(uniqueChips.size());
        response.setShowSent(!isTZeroMode(accessToken.getMode()));
        response.setLastPassings(uniqueRecent);
        response.setMinuteBuckets(new ArrayList<>(buckets));
        return response;
    }

    private List<PublicSpeakerEntry> collectUniqueRecentEntries(List<PublicSpeakerEntry> entries, int limit) {
        List<PublicSpeakerEntry> uniqueEntries = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (PublicSpeakerEntry entry : entries) {
            String key = !safe(entry.getBib()).isBlank() ? safe(entry.getBib()) : safe(entry.getChip());
            if (key.isBlank() || seen.contains(key)) {
                continue;
            }
            seen.add(key);
            uniqueEntries.add(entry);
            if (uniqueEntries.size() >= limit) {
                break;
            }
        }
        uniqueEntries.sort(Comparator.comparing(PublicSpeakerEntry::getTimestamp));
        return uniqueEntries;
    }

    private String buildAthleteName(BibChipRow meta, String chip) {
        if (meta == null) {
            return safe(chip);
        }
        String first = safe(meta.getFirstName()).trim();
        String last = safe(meta.getLastName()).trim();
        String full = (first + " " + last).trim();
        return full.isBlank() ? safe(chip) : full;
    }

    private String resolveTeam(BibChipRow meta) {
        return safe(meta.getTeam()).trim();
    }

    private boolean isTZeroMode(String mode) {
        return "TZERO".equalsIgnoreCase(safe(mode));
    }

    private String extractChip(String line) {
        String raw = safe(line).trim();
        if (raw.isBlank() || raw.contains("*")) {
            return null;
        }
        String separator = raw.contains("\t") ? "\t" : ";";
        String value = raw.split(separator)[0].replace("\"", "").trim();
        return value.isBlank() ? null : value;
    }

    private String extractTimestamp(String line) {
        String raw = safe(line).trim();
        if (raw.isBlank()) {
            return "";
        }
        String separator = raw.contains("\t") ? "\t" : ";";
        String[] parts = raw.split(separator);
        return parts.length > 1 ? safe(parts[1]).replace("\"", "").trim() : "";
    }

    private LocalDateTime parseTimestampToMinute(String timestamp) {
        String value = safe(timestamp).trim();
        if (value.isBlank()) {
            return null;
        }
        try {
            if (value.matches("^\\d{4}-\\d{2}-\\d{2}[ T].*")) {
                return LocalDateTime.parse(value.replace(' ', 'T').replace(',', '.').substring(0, Math.min(16, value.length())).replace(' ', 'T') + ":00".substring(Math.min(3, Math.max(0, 19 - Math.min(16, value.length())))));
            }
        } catch (Exception ignored) {
        }
        try {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("^(\\d{2})\\.(\\d{2})\\.(\\d{4})[ T](\\d{2}):(\\d{2})").matcher(value);
            if (matcher.find()) {
                return LocalDateTime.of(
                        Integer.parseInt(matcher.group(3)),
                        Integer.parseInt(matcher.group(2)),
                        Integer.parseInt(matcher.group(1)),
                        Integer.parseInt(matcher.group(4)),
                        Integer.parseInt(matcher.group(5))
                );
            }
        } catch (Exception ignored) {
        }
        try {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("^(\\d{2})\\/(\\d{2})\\/(\\d{4})[ T](\\d{2}):(\\d{2})").matcher(value);
            if (matcher.find()) {
                return LocalDateTime.of(
                        Integer.parseInt(matcher.group(3)),
                        Integer.parseInt(matcher.group(2)),
                        Integer.parseInt(matcher.group(1)),
                        Integer.parseInt(matcher.group(4)),
                        Integer.parseInt(matcher.group(5))
                );
            }
        } catch (Exception ignored) {
        }
        try {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d{2}):(\\d{2})(?::\\d{2}(?:[.,]\\d+)?)?").matcher(value);
            if (matcher.find()) {
                java.time.LocalDate today = java.time.LocalDate.now();
                return LocalDateTime.of(today.getYear(), today.getMonthValue(), today.getDayOfMonth(),
                        Integer.parseInt(matcher.group(1)),
                        Integer.parseInt(matcher.group(2)));
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Path resolvePreviewPath(String source) {
        if (source.startsWith("http://") || source.startsWith("https://")) {
            try {
                String pathPart = URI.create(source).getPath();
                String fileName = Paths.get(pathPart).getFileName() != null
                        ? Paths.get(pathPart).getFileName().toString()
                        : "";
                if (fileName.isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome file non valido in source remoto");
                }
                return it.icron.icronium.connector.rr.IcroniumApplication.WORK_DIR.resolve(fileName);
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL source non valido");
            }
        }
        return Paths.get(source);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
