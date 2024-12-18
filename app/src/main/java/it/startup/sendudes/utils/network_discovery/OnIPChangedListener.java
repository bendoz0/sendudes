package it.startup.sendudes.utils.network_discovery;

public interface OnIPChangedListener {
    void onNetworkChanged(String newIp);
    void onNoNetwork();
}
