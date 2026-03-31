package it.icron.icronium.connector.rr.integration;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import it.icron.icronium.connector.rr.AppLogger;

public class ConnectorContext {

    private String rrId;
    private String baseUrl;
    private String pw;

    private List<String> timingPoints = new ArrayList<>();
    //private Map<Integer, String> bibChipMap = new LinkedHashMap<>();
    private Map<String, Integer> chipBibMap = new LinkedHashMap<>();
    
    RaceResultClient rr = new RaceResultClient();

    public boolean isConnected() {
        return rrId != null;
    }

    public String getBaseUrl() { return baseUrl; }
    public List<String> getTimingPoints() { return timingPoints; }
   // public Map<Integer, String> getBibChipMap() { return bibChipMap; }

    public void setRrId(String rrId) {
        this.rrId = rrId;
        this.baseUrl = rrId;
        if (!rrId.startsWith("http")) {
        	this.baseUrl = "http://localhost/" + rrId+"/";
        } else {
        	this.rrId = extractRrId(rrId);
        }
        if (this.baseUrl.indexOf("?") != -1) {
            int qIndex = this.baseUrl.indexOf("?");
            this.pw = this.baseUrl.substring(qIndex + 1);
            this.baseUrl = this.baseUrl.substring(0, qIndex);
        } else {
            this.pw = "lang=en-it&pw=0";
        }
        this.baseUrl = this.baseUrl + "api/";

        String extractedPw = extractQueryParam(this.pw, "pw");
        if (extractedPw != null && !extractedPw.isBlank()) {
            rr.setPw(extractedPw);
        } else {
            rr.setPw("0");
        }
    }


    public void setTimingPoints(List<String> timingPoints) {
        this.timingPoints = timingPoints;
    }



	public Map<String, Integer> getChipBibMap() {
		return chipBibMap;
		
	}

	public void setChipBibMap(Map<String, Integer> chipBibMap) {
		this.chipBibMap = chipBibMap;
		
	}
	
	public static String extractRrId(String url) {
	    try {
	        URI uri = URI.create(url);
	        String path = uri.getPath();          // es: "/_IBGEL/"
	        if (path == null) return null;

	        // rimuove slash iniziali/finali
	        path = path.replaceAll("^/+", "").replaceAll("/+$", "");

	        // primo segmento
	        int slash = path.indexOf('/');
	        return (slash >= 0) ? path.substring(0, slash) : path;

	    } catch (Exception e) {
	    	AppLogger.logException(url, e);
	        return null;
	    }
	}
	
	public String getRrId() {
		return rrId;
	}

	public String getPw() {
		return pw;
	}

	public RaceResultClient getRr() {
		return rr;
	}

    private String extractQueryParam(String query, String key) {
        if (query == null || query.isBlank() || key == null || key.isBlank()) {
            return null;
        }
        String[] parts = query.split("&");
        for (String part : parts) {
            int eq = part.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String k = part.substring(0, eq);
            if (key.equalsIgnoreCase(k)) {
                return part.substring(eq + 1);
            }
        }
        return null;
    }
}
