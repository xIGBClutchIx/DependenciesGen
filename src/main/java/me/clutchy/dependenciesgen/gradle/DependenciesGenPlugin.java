package me.clutchy.dependenciesgen.gradle;

import me.clutchy.dependenciesgen.shared.Dependency;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.api.artifacts.repositories.UrlArtifactRepository;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DependenciesGenPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getTasks().register("gen-dependencies", task -> createDependenciesJson(project, project.getExtensions().getByType(DependenciesGen.class)));
        project.getExtensions().create("DependenciesGen", DependenciesGen.class, project);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void createDependenciesJson(Project project, DependenciesGen extension) {
        Path metaResources = project.getBuildDir().toPath().resolve("resources").resolve("main").resolve("META-INF");
        metaResources.toFile().mkdirs();
        try (BufferedWriter writer = Files.newBufferedWriter(metaResources.resolve("dependencies.json"), StandardCharsets.UTF_8)) {
            writer.write(getAllDependencies(project, extension).toString());
        } catch (IOException e) {
            System.out.println("Error writing dependencies.json");
        }
    }

    private List<Dependency> getAllDependencies(Project project, DependenciesGen extension) {
        List<Dependency> dependencies = new ArrayList<>();
        project.getConfigurations().getByName("apiDependenciesMetadata").getResolvedConfiguration().getFirstLevelModuleDependencies()
                .forEach(depend -> dependencies.addAll(getDependenciesFromParent(project, depend, extension)));
        return dependencies;
    }

    private List<Dependency> getDependenciesFromParent(Project project, ResolvedDependency depend, DependenciesGen extension) {
        List<Dependency> dependencies = new ArrayList<>();
        String group = depend.getModuleGroup();
        String name = depend.getModuleName();
        if (extension.ignored.contains(group + ":" + name)) return new ArrayList<>();
        String version = depend.getModuleVersion();
        String repo = getRepo(project, group, name, version);
        List<Dependency> children = new ArrayList<>();
        if (depend.getChildren().size() != 0) {
            depend.getChildren().forEach(childDepend -> {
                if (childDepend.getParents().contains(depend) && childDepend.getConfiguration().equals("compile")) {
                    children.addAll(getDependenciesFromParent(project, childDepend, extension));
                }
            });
        }
        dependencies.add(new Dependency(group, name, version, repo, children));
        return dependencies;
    }

    private String getRepo(Project project, String group, String name, String version) {
        // Get all the repositories for a project
        for (ArtifactRepository artifactRepo : project.getRepositories()) {
            // Only url repositories
            if (artifactRepo instanceof UrlArtifactRepository) {
                UrlArtifactRepository repository = (UrlArtifactRepository) artifactRepo;
                // Check if not a local file
                if (repository.getUrl().toString().matches("(?!file\\b)\\w+?:\\/\\/.*")) {
                    URL dependencyURL = getDependencyURL(repository.getUrl().toString(), group, name, version);
                    if (checkIfURLExists(dependencyURL)) {
                        // Just return the repo not the whole url
                        return repository.getUrl().toString();
                    }
                }
            }
        }
        // Can't find dependency in repositories. Possibly a local file?
        return null;
    }

    private URL getDependencyURL(String url, String group, String name, String version) {
        if (url == null || url.trim().isEmpty()) return null;
        if (!url.endsWith("/")) url += "/";
        try {
            return new URL(url + getPath(group, name, version) + getFileName(name, version));
        } catch (MalformedURLException e) {
            return null;
        }
    }

    private String getPath(String group, String name, String version) {
        return group.replaceAll("\\.", "/") + "/" + name + "/" + version + "/";
    }

    private String getFileName(String name, String version) {
        return name + "-" + version + ".jar";
    }

    private boolean checkIfURLExists(URL url) {
        if (url == null) return false;
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            int responseCode = connection.getResponseCode();
            connection.disconnect();
            return 200 <= responseCode && responseCode <= 399;
        } catch (Exception e) {
            return false;
        }
    }
}
