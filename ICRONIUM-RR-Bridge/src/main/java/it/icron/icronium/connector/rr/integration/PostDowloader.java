package it.icron.icronium.connector.rr.integration;

import it.icron.icronium.connector.rr.model.GaraDettaglioRow;

import java.nio.file.Path;

public class PostDowloader {

    public static PostBatchResult postDeltaBatch(Path file, GaraDettaglioRow row, ConnectorContext ctx) throws Exception {
        return PostUploader.postDeltaBatch(file, row, ctx);
    }
}
