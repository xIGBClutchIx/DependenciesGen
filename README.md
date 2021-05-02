# Library Gen
Library Gen is a gradle plugin that takes all the dependencies of a project and then exports needed information to a Libraries.json file in META-INF of the project.

## Usage
`./gradlew gen-libs` - Exports all the dependencies to a libraries.json file in the output resources/META-INF folder for the project.