package me.clutchy.librarygen;

import org.gradle.api.Project;
import org.gradle.api.Plugin;
import org.gradle.api.artifacts.ResolvedDependency;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class LibraryGenPlugin implements Plugin<Project> {

    public void apply(Project project) {
        project.getTasks().register("gen-libs", task -> createLibrariesJson(project));
    }

    private class ExportDependency {
        private final String group;
        private final String name;
        private final String version;
        private final List<ExportDependency> dependencies;

        public ExportDependency(String group, String name, String version, List<ExportDependency> dependencies) {
            this.group = group;
            this.name = name;
            this.version = version;
            this.dependencies = dependencies;
        }

        @Override
        public String toString() {
            return "{\"group\": \"" + group + "\", \"name\": \"" + name + "\", \"version\": \"" + version + "\", \"dependencies\": " + dependencies + "}";
        }
    }

    public void createLibrariesJson(Project project) {
        Path metaResources = project.getBuildDir().toPath().resolve("resources").resolve("main").resolve("META-INF");
        metaResources.toFile().mkdirs();
        try {
            Files.write(metaResources.resolve("libraries.json"), getAllDependencies(project).toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<ExportDependency> getAllDependencies(Project project) {
        List<ExportDependency> dependencies = new ArrayList<>();
        project.getConfigurations().getByName("apiDependenciesMetadata").getResolvedConfiguration().getFirstLevelModuleDependencies().forEach(depend -> {
            // IGNORE List?
            //if (!depend.getModuleName().equals("paper-api")) {
                dependencies.addAll(getDependenciesFromParent(depend));
            //}
        });
        return dependencies;
    }

    public List<ExportDependency> getDependenciesFromParent(ResolvedDependency depend) {
        List<ExportDependency> dependencies = new ArrayList<>();
        if (depend.getChildren().size() != 0) {
            List<ExportDependency> children = new ArrayList<>();
            depend.getChildren().forEach(childDepend -> {
                if (childDepend.getParents().contains(depend) && childDepend.getConfiguration().equals("compile")){
                    children.addAll(getDependenciesFromParent(childDepend));
                }
            });
            dependencies.add(new ExportDependency(depend.getModuleGroup(), depend.getModuleName(), depend.getModuleVersion(), children));
        } else {
            dependencies.add(new ExportDependency(depend.getModuleGroup(), depend.getModuleName(), depend.getModuleVersion(), new ArrayList<>()));
        }
        return dependencies;
    }
}
