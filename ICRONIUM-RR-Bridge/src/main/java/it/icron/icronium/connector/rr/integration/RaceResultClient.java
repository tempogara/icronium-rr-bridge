package it.icron.icronium.connector.rr.integration;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

public class RaceResultClient {

    private final CookieManager cookieManager;
    private final HttpClient http;
    
    private String pw=null;

    public String getPw() {
		return pw;
	}

	public void setPw(String pw) {
		this.pw = pw;
	}

	public RaceResultClient() {
        cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);

        http = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    // ---------------------------------------------------
    // LOGIN
    // ---------------------------------------------------

    public void login(String user, String password) throws Exception {

        String form = "user=" + enc(user) +
                "&pw=" + enc(password) +
                "&totp=" +
                "&signinas=";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://www.raceresult.com/api/token/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpResponse<Void> resp = http.send(req, HttpResponse.BodyHandlers.discarding());

        if (resp.statusCode() != 200) {
            throw new RuntimeException("Login fallito: " + resp.statusCode());
        }
        
        this.pw = startRR14AndGetPw();
    }

    // ---------------------------------------------------
    // STEP SSO RR14
    // ---------------------------------------------------

    private String startRR14AndGetPw() throws Exception {

        // prendi il cookie "at"
        String bearer = cookieManager.getCookieStore().getCookies()
                .stream()
                .filter(c -> c.getName().equals("at"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("cookie at non trovato"))
                .getValue();

        // rimuove eventuali virgolette
        bearer = bearer.replace("\"", "");

        // rimuove "Bearer "
        String jwt = bearer.substring(7);
        
        System.out.println(jwt);

        String form = "rruser_token=" + URLEncoder.encode(jwt, StandardCharsets.UTF_8);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://events.raceresult.com/start?lang=it"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

        // qui arriva redirect verso events/?pw=...
        Optional<String> location = resp.headers().firstValue("location");

        if (location.isEmpty()) {
            throw new RuntimeException("redirect events non trovato");
        }

        URI redirect = URI.create(location.get());

        for (String p : redirect.getQuery().split("&")) {
            if (p.startsWith("pw=")) {
                return p.substring(3);
            }
        }

        throw new RuntimeException("pw non trovato nel redirect events");
    }

    // ---------------------------------------------------
    // BOOTSTRAP EVENTS
    // ---------------------------------------------------

    public void openEvents(String pw, String lang) throws Exception {

        String url = "https://events.raceresult.com/?pw=" + enc(pw) + "&lang=" + enc(lang);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    // ---------------------------------------------------
    // CHIAMATA API
    // ---------------------------------------------------

    private String addPw(String url) {
        if (pw == null || pw.isBlank()) {
            return url;
        }

        if (url.contains("?")) {
            return url + "&pw=" + enc(pw);
        } else {
            return url + "?pw=" + enc(pw);
        }
    }
    
    
    private String addSessId(String url) {

        if (url.contains("?")) {
            return url + "&sessId=" + enc(pw);
        } else {
            return url + "?sessid=" + enc(pw);
        }
    }
    
    public String getData(String url) throws Exception {
    	return getData( url,  null);
    }

    public String getDataRaw(String url) throws Exception {
        System.out.println("getData(raw) = " + url);
        return sendGetFollowingRedirects(url);
    }
    
    public String getData(String url, String sessId) throws Exception {

    	if (sessId == null) url = addPw(url);
    	else url = addSessId(url);
    	
    	System.out.println("getData = " + url);
        String body = sendGetFollowingRedirects(url);

        // Some RR endpoints require an events session cookie in addition to pw.
        if (body != null
                && body.contains("session cookie not set or incorrect")
                && pw != null
                && !pw.isBlank()
                && !"0".equals(pw)) {
            openEvents(pw, "en");
            body = sendGetFollowingRedirects(url);
        }

        return body;
    }
    
    
    
    public String postData(String url, String body) throws Exception {

        url = addPw(url);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

        return resp.body();
    }
    
    
    
    public String getEvents() throws Exception {
        return getData("https://events.raceresult.com/api/public/eventlist?lang=en-it&AddSettings=EventName%2CEventDate%2CEventType%2CEventLogo%2CEventType%2CTestMode%2CRegActive%2CModJobIDSettings", "sessid");
    }
    
    public String getEventsLocal() throws Exception {
        return getData("http://localhost/api/local/eventlist?lang=en-it&AddSettings=EventName%2CEventDate%2CEventType%2CEventLogo%2CEventType%2CTestMode%2CRegActive%2CModJobIDSettings");
        
    }
//    
//    
//    public String getEventList(String eventId) throws Exception {
//
//        String url =
//                "https://events.raceresult.com/_" + eventId + "/api/data/list" +
//                        "?lang=en-it" +
//                        "&fields=%5B%22BIB%22%2C%22CHIP%22%5D&sort=BIB&listformat=CSV" +
//                        "&pw=" + enc(this.pw);
//
//        HttpRequest req = HttpRequest.newBuilder()
//                .uri(URI.create(url))
//                .GET()
//                .build();
//
//        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
//
//        return resp.body();
//    }

    // ---------------------------------------------------
    // UTIL
    // ---------------------------------------------------

    private String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private String sendGetFollowingRedirects(String url) throws Exception {
        URI current = URI.create(url);
        for (int i = 0; i < 5; i++) {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(current)
                    .GET()
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            int status = resp.statusCode();
            if (status >= 300 && status < 400) {
                Optional<String> location = resp.headers().firstValue("location");
                if (location.isPresent() && !location.get().isBlank()) {
                    current = current.resolve(location.get());
                    continue;
                }
            }
            return resp.body();
        }
        throw new RuntimeException("Too many redirects for GET " + url);
    }

    public void dumpCookies() {

        System.out.println("Cookies:");

        for (HttpCookie c : cookieManager.getCookieStore().getCookies()) {
            System.out.println(" - " + c.getName() +
                    " ; domain=" + c.getDomain() +
                    " ; path=" + c.getPath());
        }
    }

    // ---------------------------------------------------
    // DEMO
    // ---------------------------------------------------

    public static void main(String[] args) throws Exception {

        RaceResultClient rr = new RaceResultClient();

        rr.login("TEMPOGARASRLS", "jordan72");

        //String pw = rr.startRR14AndGetPw();

        System.out.println("PW dinamico = " + rr.pw);

        //rr.openEvents(pw, "it");

        rr.dumpCookies();

        String json = rr.getEvents();

        System.out.println(json);
    }
}
