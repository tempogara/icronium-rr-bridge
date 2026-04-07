package it.icron.icronium.connector.rr.model;

import java.util.ArrayList;
import java.util.List;

public class LanDiscoveryResponse {

    private String subnetPrefix;
    private List<String> readerIps = new ArrayList<>();

    public String getSubnetPrefix() {
        return subnetPrefix;
    }

    public void setSubnetPrefix(String subnetPrefix) {
        this.subnetPrefix = subnetPrefix;
    }

    public List<String> getReaderIps() {
        return readerIps;
    }

    public void setReaderIps(List<String> readerIps) {
        this.readerIps = readerIps;
    }
}
