package it.icron.icronium.connector.rr.integration;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.stream.Stream;

import it.icron.icronium.connector.rr.IcroniumApplication;

public class FileDownloader {

    /**
     * "Locale": mantiene il comportamento precedente.
     * Se qui in realtà passi un path locale, lo copia in WORK_DIR.
     * (Se in futuro vuoi gestire IP reali via rete, lo faremo qui.)
     */
	public static Path download(String src) throws IOException {

	    Path source = Paths.get(src);
	    String fileName = source.getFileName().toString();

	    Path target = IcroniumApplication.WORK_DIR.resolve(fileName);

	    // copia e sovrascrive mantenendo nome fisso
	    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

	    return target;
	}

    /**
     * Remoto HTTP/HTTPS:
     * - salva in WORK_DIR
     * - nome file = nome originale dell'URL
     * - scarica su .tmp
     * - sostituisce SOLO se la size del nuovo > size del precedente
     * - se il nuovo è più piccolo/uguale, scarta il tmp e ritorna il vecchio
     */
	public static Path downloadHttp(String urlStr) throws IOException {

	    URL url = new URL(urlStr);
	    HttpURLConnection conn = (HttpURLConnection) url.openConnection();

	    // Timeout (in millisecondi)
	    conn.setConnectTimeout(10000); // 10s per connettersi
	    conn.setReadTimeout(15000);    // 15s per leggere

	    conn.setRequestMethod("GET");
	    conn.setInstanceFollowRedirects(true);

	    conn.connect();

	    int status = conn.getResponseCode();

	    if (status != HttpURLConnection.HTTP_OK) {
	        throw new IOException("HTTP error: " + status + " - " + conn.getResponseMessage());
	    }

	    // Nome file dall'URL
	    String fileName = Path.of(url.getPath())
	                          .getFileName()
	                          .toString();

	    if (fileName.isBlank()) {
	        fileName = "download.tmp";
	    }

	    Path target = IcroniumApplication.WORK_DIR.resolve(fileName);
	    Path temp = IcroniumApplication.WORK_DIR.resolve(fileName + ".tmp");

	    // Download su file temporaneo
	    try (InputStream in = new BufferedInputStream(conn.getInputStream());
	         OutputStream out = Files.newOutputStream(
	                 temp,
	                 StandardOpenOption.CREATE,
	                 StandardOpenOption.TRUNCATE_EXISTING)) {

	        byte[] buffer = new byte[8192];
	        int read;

	        while ((read = in.read(buffer)) != -1) {
	            out.write(buffer, 0, read);
	        }
	    }

	    if (Files.exists(temp)) {
	    	long newSize = Files.size(temp);
	    	if (newSize == 0) {
	    		Files.deleteIfExists(temp);
	    		throw new IOException("Downloaded file is empty");
	    	}
	    	
	    	if (Files.exists(target)) {
	    		
	    		long oldSize = Files.size(target);
	    		
	    		// Evita overwrite con file più piccolo
	    		if (newSize <= oldSize) {
	    			Files.deleteIfExists(temp);
	    			return target;
	    		}
	    	}
	    	
	    	// Move atomico se possibile
	    	try {
	    		Files.move(temp, target,
	    				StandardCopyOption.REPLACE_EXISTING,
	    				StandardCopyOption.ATOMIC_MOVE);
	    	} catch (AtomicMoveNotSupportedException ex) {
	    		
	    		Files.move(temp, target,
	    				StandardCopyOption.REPLACE_EXISTING);
	    	}
	    }



	    return target;
	}


    /**
     * Ripristinato: conta righe del file.
     */
    public static long countLines(Path p) throws IOException {
        try (Stream<String> s = Files.lines(p)) {
            return s.filter(line -> line != null && !line.contains("*")).count();
        }
    }
}
