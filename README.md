# Dependencies Gen [![Gradle Plugin](https://img.shields.io/badge/Gradle-Plugin-brightgreen)](https://plugins.gradle.org/plugin/me.clutchy.dependenciesgen) [![Maven Central](https://img.shields.io/maven-central/v/me.clutchy/DependenciesGen)](https://search.maven.org/artifact/me.clutchy/DependenciesGen)
Dependencies Gen is a gradle plugin that takes all the dependencies of a project and then exports needed information to a dependencies.json file in META-INF of the project.

Includes a way to ignore certain dependencies based on the group and name.

Include as a normal dependency to easily download the dependencies and load them in a project!

## Build Gradle Usage for Plugin
``` groovy
import me.clutchy.dependenciesgen.gradle.DependenciesGenPlugin.DependenciesGen

plugins {
    id("me.clutchy.dependenciesgen") version "1.0.1"
}

tasks.getByName("classes").dependsOn(tasks.getByName("gen-dependencies")) // Optional but good to include.

configure<DependenciesGen> {
    ignored = listOf("me.clutchy:DependenciesGen") // Optional - Probably want to include if you use the dependency part.
}
```

## Build Gradle Usage for Dependency
``` groovy
plugins {
    id("me.clutchy.dependenciesgen") version "1.0.1" // Optional but good if you want to shadow the jar.
}

dependencies {
    api("me.clutchy:DependenciesGen:1.0.1")
}

// All below is optional but good if you want to shadow the jar.
tasks.getByName("build").dependsOn(tasks.getByName("shadowJar"))

configure<DependenciesGen> {
    ignored = listOf("me.clutchy:DependenciesGen")
}

tasks.withType<ShadowJar> {
    exclude("/me/clutchy/dependenciesgen/gradle/")
    dependencies {
        include(dependency("me.clutchy:DependenciesGen:1.0.1"))
    }
}
```

## CLI Usage
`./gradlew gen-dependencies` - Exports all the dependencies to a dependencies.json file in the output resources/META-INF folder for the project.

## Java Dependency Usage
``` java
import me.clutchy.dependenciesgen.downloader.DependencyDownloader;

import java.io.File;
import java.util.logging.Logger;

public class Example {

    private static final DependencyDownloader downloader = new DependencyDownloader(ClassLoader.getSystemClassLoader(), Logger.getLogger("Example"));

    public static void main(String[] args) {
        downloader.downloadDependencies(Example.class.getResourceAsStream("META-INF" + File.separator + "dependencies.json"));
    }
}
```

## Example of dependencies.json
```json
[{"group": "org.reflections", "name": "reflections", "version": "0.9.12", "repo": "https://repo.maven.apache.org/maven2/", "dependencies": [{"group": "org.javassist", "name": "javassist", "version": "3.26.0-GA", "repo": "https://repo.maven.apache.org/maven2/", "dependencies": []}]}, {"group": "org.jetbrains.kotlin", "name": "kotlin-stdlib-jdk8", "version": "1.4.32", "repo": "https://repo.maven.apache.org/maven2/", "dependencies": [{"group": "org.jetbrains.kotlin", "name": "kotlin-stdlib-jdk7", "version": "1.4.32", "repo": "https://repo.maven.apache.org/maven2/", "dependencies": [{"group": "org.jetbrains.kotlin", "name": "kotlin-stdlib", "version": "1.4.32", "repo": "https://repo.maven.apache.org/maven2/", "dependencies": [{"group": "org.jetbrains", "name": "annotations", "version": "20.1.0", "repo": "https://repo.maven.apache.org/maven2/", "dependencies": []}, {"group": "org.jetbrains.kotlin", "name": "kotlin-stdlib-common", "version": "1.4.32", "repo": "https://repo.maven.apache.org/maven2/", "dependencies": []}]}]}, {"group": "org.jetbrains.kotlin", "name": "kotlin-stdlib", "version": "1.4.32", "repo": "https://repo.maven.apache.org/maven2/", "dependencies": [{"group": "org.jetbrains", "name": "annotations", "version": "20.1.0", "repo": "https://repo.maven.apache.org/maven2/", "dependencies": []}, {"group": "org.jetbrains.kotlin", "name": "kotlin-stdlib-common", "version": "1.4.32", "repo": "https://repo.maven.apache.org/maven2/", "dependencies": []}]}]}]
```