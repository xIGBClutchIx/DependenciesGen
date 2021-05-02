# Dependencies Gen
Dependencies Gen is a gradle plugin that takes all the dependencies of a project and then exports needed information to a dependencies.json file in META-INF of the project.

Includes a way to ignore certain dependencies based on the group and name.

## Build Gradle Usage
``` groovy
import me.clutchy.dependenciesgen.DependenciesGenPlugin.DependenciesGen

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
    }
    dependencies {
        classpath("me.clutchy:DependenciesGen:1.0.0")
    }
}
apply(plugin = "DependenciesGen")

tasks.getByName("classes").dependsOn(tasks.getByName("gen-dependencies"))

configure<DependenciesGen> {
    ignored = listOf("groupID:nameID")
}
```


## CLI Usage
`./gradlew gen-dependencies` - Exports all the dependencies to a dependencies.json file in the output resources/META-INF folder for the project.