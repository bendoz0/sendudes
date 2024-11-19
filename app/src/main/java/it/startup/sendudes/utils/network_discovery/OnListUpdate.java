package it.startup.sendudes.utils.network_discovery;

import java.util.HashMap;

@FunctionalInterface
public interface OnListUpdate {
    void listUpdated(HashMap<String,String> foudIps);
}
