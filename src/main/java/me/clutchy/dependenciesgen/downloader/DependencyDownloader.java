package me.clutchy.dependenciesgen.downloader;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import me.clutchy.dependenciesgen.shared.Dependency;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DependencyDownloader {

    private static final ArrayList<String> loadedArtifacts = new ArrayList<>();

    private final ClassLoader classLoader;
    private final Logger logger;

    public DependencyDownloader(ClassLoader classLoader, Logger logger) {
        this.classLoader = classLoader;
        this.logger = logger;
    }

    public void downloadDependencies(InputStream stream) {
        if (stream == null) return;
        List<Dependency> dependencies = new ArrayList<>();
        // Read from our json file we gave it.
        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonArray object = Json.parse(reader).asArray();
            for (JsonValue value: object.values()) {
                if (value instanceof JsonObject) {
                    dependencies.add(new Dependency((JsonObject) value));
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error reading dependencies json", e);
            System.exit(0);
        }
        downloadDependencies(dependencies);
    }

    public void downloadDependencies(List<Dependency> parentDependencies) {
        logger.info("Loading dependencies");
        List<Dependency> dependencies = new ArrayList<>();
        // Already loaded list
        List<String> loadedDependenciesIds = new ArrayList<>();
        // Get all dependencies as one list
        parentDependencies.forEach(parentDependency -> dependencies.addAll(getDependenciesFromParent(parentDependency)));
        // Sort all dependencies
        Collections.sort(dependencies);
        // Lock to dependencies so we don't continue until we are out of dependencies
        CountDownLatch latch = new CountDownLatch(dependencies.size());
        for (Dependency dependency : dependencies) {
            // Make sure we don't have this artifact loaded.
            if (loadedArtifacts.contains(dependency.getGroup() + ":" + dependency.getName())) {
                latch.countDown();
                continue;
            }
            // Add to global artifacts so we don't get duplicates.
            // Possibly do version checking in the future and unload older?
            loadedArtifacts.add(dependency.getGroup() + ":" + dependency.getName());
            new Thread(() -> {
                File cacheDependencyPath = new File("cache", getPath(dependency));
                try {
                    // Create default directories
                    Files.createDirectories(cacheDependencyPath.toPath());
                    File jar = cacheDependencyPath.toPath().resolve(getFileName(dependency, false)).toFile();
                    // If the file does not exist then try to download it.
                    // If it does then check to make sure it is valid based off MD5.
                    if (!jar.exists()) {
                        downloadFile(dependency, jar, false);
                    } else {
                        logger.info("Checking dependency: " + dependency.getName());
                        // Read the dependencies MD5
                        byte[] bytes = Files.readAllBytes(jar.toPath());
                        byte[] hash = MessageDigest.getInstance("MD5").digest(bytes);
                        String md5 = toHexString(hash);
                        // Make sure our file MD5 is still valid?
                        if (!md5.trim().isEmpty()) {
                            // Read the dependency md5 from url
                            try (BufferedReader readerUrl = new BufferedReader(new InputStreamReader(getConnection(dependency, true), StandardCharsets.UTF_8))) {
                                String urlMd5 = readerUrl.readLine();
                                if (urlMd5 != null && !urlMd5.equalsIgnoreCase(md5)) {
                                    downloadFile(dependency, jar, true);
                                }
                            } catch (Exception ignored) {
                                // We don't have a url for the md5 so just continue on sadly.
                            }
                        }
                    }
                    // Add to the class loader - Spigot use a url class loader.
                    LoadJarsUtil.addFile(classLoader, logger, jar);
                    // Add to local list of dependencies
                    loadedDependenciesIds.add(dependency.getName());
                } catch (Exception e) {
                    latch.countDown();
                    // We encountered a error with needed dependencies so we can't proceed, log and shutdown.
                    logger.log(Level.SEVERE, "Error loading dependency: " + dependency.getName(), e);
                    // Remove from loaded
                    loadedArtifacts.remove(dependency.getGroup() + ":" + dependency.getName());
                    // Shutdown
                    System.exit(0);
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.severe("Error loading dependency: ");
            System.exit(0);
        }
        // Log all our dependency
        if (!loadedDependenciesIds.isEmpty()) {
            Collections.sort(loadedDependenciesIds);
            String loadedDependencies = loadedDependenciesIds.toString().substring(1).replaceFirst("]", "");
            logger.info("Loaded dependencies: " + loadedDependencies);
        }
    }

    // Turns MD5 bytes to hex string needed for dependency.
    private String toHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte aByte : bytes) {
            String hex = Integer.toHexString(0xFF & aByte);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private URL getUrl(Dependency dependency, boolean MD5) throws MalformedURLException {
        String repo = dependency.getRepo();
        if (repo == null || repo.trim().isEmpty()) repo = "https://repo.maven.apache.org/maven2/";
        if (!repo.endsWith("/")) repo += "/";
        return new URL(repo + getPath(dependency) + getFileName(dependency, MD5));
    }

    private String getFileName(Dependency dependency, boolean MD5) {
        return dependency.getName() + "-" + dependency.getVersion() + ".jar" + (MD5 ? ".md5" : "");
    }

    private String getPath(Dependency dependency) {
        return dependency.getGroup().replaceAll("\\.", "/") + "/" + dependency.getName() + "/" + dependency.getVersion() + "/";
    }

    private void downloadFile(Dependency dependency, File location, boolean reDownload) throws IOException {
        logger.info((reDownload ? "Red" : "D") + "ownloading dependency" + ": " + dependency.getName());
        // Download and copy file. Catch error here in the future?
        try (InputStream inputStream = getConnection(dependency, false)) {
            Files.copy(inputStream, location.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // Default connection process for dependencies with user-agent to make sure nothing goes wrong.
    private InputStream getConnection(Dependency dependency, boolean MD5) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) getUrl(dependency, MD5).openConnection();
        connection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
        connection.setRequestMethod("GET");
        connection.connect();
        return connection.getInputStream();
    }

    public List<Dependency> getDependenciesFromParent(Dependency dependency) {
        List<Dependency> dependencies = new ArrayList<>(Collections.singletonList(dependency));
        dependency.getDependencies().forEach(childDepend -> dependencies.addAll(getDependenciesFromParent(childDepend)));
        return dependencies;
    }
}
