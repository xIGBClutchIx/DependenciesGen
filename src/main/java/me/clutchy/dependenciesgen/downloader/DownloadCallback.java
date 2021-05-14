package me.clutchy.dependenciesgen.downloader;

import java.net.URL;

public interface DownloadCallback {
    void callback(URL url);
}
