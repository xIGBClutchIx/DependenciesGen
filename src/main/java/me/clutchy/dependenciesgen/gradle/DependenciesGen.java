package me.clutchy.dependenciesgen.gradle;

import org.gradle.api.Project;

import java.util.ArrayList;
import java.util.List;

public class DependenciesGen {
    public List<String> ignored;

    public DependenciesGen(Project project) {
        ignored = project.getObjects().listProperty(String.class).getOrElse(new ArrayList<>());
    }
}
