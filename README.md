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
apply(plugin = "me.clutchy.dependenciesgen")

tasks.getByName("classes").dependsOn(tasks.getByName("gen-dependencies"))

configure<DependenciesGen> {
    ignored = listOf("groupID:nameID")
}
```

## CLI Usage
`./gradlew gen-dependencies` - Exports all the dependencies to a dependencies.json file in the output resources/META-INF folder for the project.

### Example of dependencies.json
```json
[{"group": "org.reflections", "name": "reflections", "version": "0.9.12", "repo": "https://repo.maven.apache.org/maven2/", "dependencies": [{"group": "org.javassist", "name": "javassist", "version": "3.26.0-GA", "repo": "https://repo.maven.apache.org/maven2/", "dependencies": []}]}, {"group": "org.jetbrains.kotlin", "name": "kotlin-stdlib-jdk8", "version": "1.4.32", "repo": "https://repo.maven.apache.org/maven2/", "dependencies": [{"group": "org.jetbrains.kotlin", "name": "kotlin-stdlib-jdk7", "version": "1.4.32", "repo": "https://repo.maven.apache.org/maven2/", "dependencies": [{"group": "org.jetbrains.kotlin", "name": "kotlin-stdlib", "version": "1.4.32", "repo": "https://repo.maven.apache.org/maven2/", "dependencies": [{"group": "org.jetbrains", "name": "annotations", "version": "20.1.0", "repo": "https://repo.maven.apache.org/maven2/", "dependencies": []}, {"group": "org.jetbrains.kotlin", "name": "kotlin-stdlib-common", "version": "1.4.32", "repo": "https://repo.maven.apache.org/maven2/", "dependencies": []}]}]}, {"group": "org.jetbrains.kotlin", "name": "kotlin-stdlib", "version": "1.4.32", "repo": "https://repo.maven.apache.org/maven2/", "dependencies": [{"group": "org.jetbrains", "name": "annotations", "version": "20.1.0", "repo": "https://repo.maven.apache.org/maven2/", "dependencies": []}, {"group": "org.jetbrains.kotlin", "name": "kotlin-stdlib-common", "version": "1.4.32", "repo": "https://repo.maven.apache.org/maven2/", "dependencies": []}]}]}]
```