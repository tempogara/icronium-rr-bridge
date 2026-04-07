package it.icron.icronium.connector.rr.model;

import java.util.ArrayList;
import java.util.List;

public class WanDiscoveryResponse {

    private List<String> readerNames = new ArrayList<>();

    public List<String> getReaderNames() {
        return readerNames;
    }

    public void setReaderNames(List<String> readerNames) {
        this.readerNames = readerNames;
    }
}
