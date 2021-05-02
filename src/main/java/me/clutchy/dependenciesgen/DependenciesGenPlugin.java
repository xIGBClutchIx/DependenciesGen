package me.clutchy.dependenciesgen;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.Plugin;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.api.artifacts.repositories.UrlArtifactRepository;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DependenciesGenPlugin implements Plugin<Project> {

    public void apply(Project project) {
        project.getTasks().register("gen-dependencies", DependenciesGenTask.class);
        project.getExtensions().create("DependenciesGen", DependenciesGen.class, project);
    }

    public abstract static class DependenciesGenTask extends DefaultTask {

        @TaskAction
        public void generate() {
            DependenciesGen extension = getProject().getExtensions().getByType(DependenciesGen.class);
            createDependenciesJson(getProject(), extension);
        }
    }

    public static class DependenciesGen {
        public List<String> ignored;

        public DependenciesGen(Project project) {
            ignored = project.getObjects().listProperty(String.class).getOrElse(new ArrayList<>());
        }
    }

    public static class Dependency {
        private final String group;
        private final String name;
        private final String version;
        private final String repo;
        private final List<Dependency> dependencies;

        public Dependency(String group, String name, String version, String repo, List<Dependency> dependencies) {
            this.group = group;
            this.name = name;
            this.version = version;
            this.repo = repo;
            this.dependencies = dependencies;
        }

        @Override
        public String toString() {
            String output = "{";
            output += "\"group\": \"" + group + "\"";
            output += ", \"name\": \"" + name + "\"";
            output += ", \"version\": \"" + version + "\"";
            if (repo != null) output += ", \"repo\": \"" + repo + "\"";
            output += ", \"dependencies\": " + dependencies;
            output += "}";
            return output;
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void createDependenciesJson(Project project, DependenciesGen extension) {
        Path metaResources = project.getBuildDir().toPath().resolve("resources").resolve("main").resolve("META-INF");
        metaResources.toFile().mkdirs();
        try {
            Files.write(metaResources.resolve("dependencies.json"), getAllDependencies(project, extension).toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<Dependency> getAllDependencies(Project project, DependenciesGen extension) {
        List<Dependency> dependencies = new ArrayList<>();
        project.getConfigurations().getByName("apiDependenciesMetadata").getResolvedConfiguration().getFirstLevelModuleDependencies()
                .forEach(depend -> dependencies.addAll(getDependenciesFromParent(project, depend, extension)));
        return dependencies;
    }

    private static List<Dependency> getDependenciesFromParent(Project project, ResolvedDependency depend, DependenciesGen extension) {
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

    private static String getRepo(Project project, String group, String name, String version) {
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

    private static URL getDependencyURL(String url, String group, String name, String version) {
        if (url == null || url.trim().isEmpty()) return null;
        if (!url.endsWith("/")) url += "/";
        try {
            return new URL(url + getPath(group, name, version) + getFileName(name, version));
        } catch (MalformedURLException e) {
            return null;
        }
    }

    private static String getPath(String group, String name, String version) {
        return group.replaceAll("\\.", "/") + "/" + name + "/" + version + "/";
    }

    private static String getFileName(String name, String version) {
        return name + "-" + version + ".jar";
    }

    private static boolean checkIfURLExists(URL url) {
        if (url == null) return false;
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            int responseCode = connection.getResponseCode();
            return 200 <= responseCode && responseCode <= 399;
        } catch (Exception e) {
            return false;
        }
    }
}
