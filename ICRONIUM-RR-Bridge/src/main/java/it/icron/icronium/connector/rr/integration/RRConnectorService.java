package it.icron.icronium.connector.rr.integration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RRConnectorService {

    public static ConnectorContext connect(String rrId) throws Exception {

        ConnectorContext ctx = new ConnectorContext();
        ctx.setRrId(rrId);
        
        String participantsCsv = ctx.getRr().getData(ctx.getBaseUrl() +
    			"data/list?lang=en&fields=%5B%22BIB%22%2C%22CHIP%22%5D&sort=BIB&listformat=CSV");
    	
    	System.out.println(participantsCsv);
    	
    	ctx.setChipBibMap(parseChipBib(participantsCsv));
    	
    	String tpJson = ctx.getRr().postData(
    			ctx.getBaseUrl() + "multirequest",
    			"[\"TimingPoints\"]"
    			);
    	
    	ctx.setTimingPoints(parseTimingPoints(tpJson));
    	
    	
    	String events = ctx.getRr().getEvents();
    	System.out.println(events);
    	
    	String eventsLocal = ctx.getRr().getEventsLocal();
    	System.out.println(eventsLocal);
    	

        return ctx;
    }

//    private static String httpGet(String url) throws IOException {
//        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
//        c.setRequestMethod("GET");
//        return read(c);
//    }

//    private static String httpPost(String url, String body) throws IOException {
//        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
//        c.setRequestMethod("POST");
//        c.setRequestProperty("Content-Type", "application/json");
//        c.setDoOutput(true);
//        try (OutputStream os = c.getOutputStream()) {
//            os.write(body.getBytes(StandardCharsets.UTF_8));
//        }
//        return read(c);
//    }

//    private static String read(HttpURLConnection c) throws IOException {
//        try (InputStream is = c.getInputStream();
//             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
//            byte[] buf = new byte[1024];
//            int r;
//            while ((r = is.read(buf)) != -1) {
//                baos.write(buf, 0, r);
//            }
//            return baos.toString("UTF-8");
//        }
//    }

    private static List<String> parseTimingPoints(String json) {
        List<String> list = new ArrayList<>();
        String[] parts = json.split("\"Name\"");
        for (int i = 1; i < parts.length; i++) {
            int start = parts[i].indexOf("\"") + 1;
            int end = parts[i].indexOf("\"", start);
            list.add(parts[i].substring(start, end));
        }
        return list;
    }

//    private static Map<Integer, String> parseBibChip(String csv) {
//
//        Map<Integer, String> map = new LinkedHashMap<>();
//
//        String[] lines = csv.split("\\r?\\n");
//
//        for (String line : lines) {
//
//            line = line.trim();
//            if (line.isEmpty()) continue;
//
//            // rimuove le virgolette
//            line = line.replace("\"", "");
//
//            String[] parts = line.split(";");
//
//            if (parts.length != 2) continue;
//
//            try {
//                Integer bib = Integer.parseInt(parts[0].trim());
//                String chip = parts[1].trim();
//                map.put(bib, chip);
//            } catch (NumberFormatException ignored) {
//            }
//        }
//
//        return map;
//    }
    
    private static Map<String, Integer> parseChipBib(String csv) {

        Map<String, Integer> map = new LinkedHashMap<>();

        String[] lines = csv.split("\\r?\\n");

        for (String line : lines) {

            line = line.trim();
            if (line.isEmpty()) continue;

            // rimuove le virgolette
            line = line.replace("\"", "");

            String[] parts = line.split(";");

            if (parts.length != 2) continue;

            try {
                Integer bib = Integer.parseInt(parts[0].trim());
                String chip = parts[1].trim();
                
                String[] multichip = chip.split("-");
                for (String c : multichip) {
                	map.put(c.trim(),bib);
                }
                
            } catch (NumberFormatException ignored) {
            }
        }

        return map;
    }

   
    
}
