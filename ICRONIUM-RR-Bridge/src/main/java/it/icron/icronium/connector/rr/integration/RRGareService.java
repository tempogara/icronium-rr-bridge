package it.icron.icronium.connector.rr.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.icron.icronium.connector.rr.AppLogger;
import it.icron.icronium.connector.rr.model.Gara;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class RRGareService {

    private final RRSessionService sessionService;
    private final ObjectMapper objectMapper;

    public RRGareService(RRSessionService sessionService, ObjectMapper objectMapper) {
        this.sessionService = sessionService;
        this.objectMapper = objectMapper;
    }

    public List<Gara> loadGareFromRR() throws Exception {
        sessionService.requireAuthenticated();

        String payload;
        if (sessionService.isLocalMode()) {
            payload = sessionService.getClient().getEventsLocal();
        } else {
            payload = sessionService.getClient().getEvents();
        }
        
        AppLogger.log(payload);

        return parseGare(payload);
    }

    private List<Gara> parseGare(String payload) throws Exception {
        JsonNode root = objectMapper.readTree(payload);
        JsonNode eventsNode = resolveEventsNode(root);
        List<Gara> result = new ArrayList<>();

        if (!eventsNode.isArray()) {
            return result;
        }

        for (JsonNode eventNode : eventsNode) {
            String id = firstText(eventNode, "ID", "id", "eventId", "EventID", "EventId", "event_id");
            String eventName = firstText(eventNode, "EventName", "eventName", "name", "description");
            String eventDate = firstText(eventNode, "EventDate", "eventDate", "date");

            if (eventName == null || eventName.isBlank()) {
                eventName = "Gara senza nome";
            }
            if (eventDate == null || eventDate.isBlank()) {
                eventDate = "-";
            }

            if (id != null && !id.isBlank()) {
                result.add(new Gara(id, eventName, eventDate));
            }
        }

        result.sort(Comparator.comparing(
                (Gara gara) -> parseEventDate(gara.getEventDate()),
                Comparator.nullsLast(Comparator.naturalOrder())
        ).reversed());

        return result;
    }

    private LocalDate parseEventDate(String eventDate) {
        if (eventDate == null || eventDate.isBlank() || "-".equals(eventDate)) {
            return null;
        }
        try {
            return LocalDate.parse(eventDate);
        } catch (Exception ignored) {
            return null;
        }
    }

    private JsonNode resolveEventsNode(JsonNode root) {
        if (root == null || root.isNull()) {
            return objectMapper.createArrayNode();
        }
        if (root.isArray()) {
            return root;
        }
        if (root.has("events")) {
            return root.get("events");
        }
        if (root.has("data")) {
            return root.get("data");
        }
        if (root.has("result")) {
            JsonNode result = root.get("result");
            if (result.isArray()) {
                return result;
            }
            if (result.has("events")) {
                return result.get("events");
            }
            if (result.has("data")) {
                return result.get("data");
            }
        }
        return objectMapper.createArrayNode();
    }

    private String firstText(JsonNode node, String... candidates) {
        for (String candidate : candidates) {
            JsonNode value = node.get(candidate);
            if (value != null && !value.isNull()) {
                String text = value.asText();
                if (text != null && !text.isBlank()) {
                    return text;
                }
            }
        }
        return null;
    }
}
