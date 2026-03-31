package it.icron.icronium.connector.rr.integration;

import it.icron.icronium.connector.rr.model.GaraDettaglioRow;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DownloadTask implements Runnable {

    private static final DateTimeFormatter TS_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final GaraDettaglioRow row;

    public DownloadTask(GaraDettaglioRow row) {
        this.row = row;
    }

    @Override
    public void run() {
        if (row == null || row.getSource() == null || row.getSource().isBlank()) {
            return;
        }
        try {
            String source = row.getSource().trim();
            Path downloaded;
            if (source.startsWith("http://") || source.startsWith("https://")) {
                downloaded = FileDownloader.downloadHttp(source);
            } else {
                downloaded = FileDownloader.download(source);
            }
            long lines = FileDownloader.countLines(downloaded);
            row.setRigheFile((int) lines);
            row.setProgresso(100);
            row.setUltimoDownload(LocalDateTime.now().format(TS_FORMATTER));
        } catch (Exception ignored) {
        }
    }
}
