package me.clutchy.dependenciesgen.shared;

import com.eclipsesource.json.*;

import java.util.ArrayList;
import java.util.List;

public class Dependency implements Comparable<Dependency> {
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

    public Dependency(JsonObject json) {
        this.group = json.getString("group", "");
        this.name = json.getString("name", "");
        this.version = json.getString("version", "");
        this.repo = json.getString("repo", null);
        List<Dependency> dependencies = new ArrayList<>();
        try {
            JsonValue value = json.get("dependencies");
            if (value instanceof JsonArray) {
                ((JsonArray) value).values().forEach(jsonValue -> {
                    if (jsonValue instanceof JsonObject) {
                        dependencies.add(new Dependency((JsonObject) jsonValue));
                    }
                });
            }
        } catch (NullPointerException ignored) {
        }
        this.dependencies = dependencies;
    }

    @Override
    public String toString() {
        return toJSON().toString();
    }

    public JsonObject toJSON() {
        JsonArray array = new JsonArray();
        dependencies.forEach(dependency -> array.add(dependency.toJSON()));
        return toJSONNoDepends().add("dependencies", array);
    }

    private JsonObject toJSONNoDepends() {
        JsonObject jsonObject = new JsonObject().add("group", group).add("name", name).add("version", version);
        if (repo != null) jsonObject.add("repo", repo);
        return jsonObject;
    }

    public String getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getRepo() {
        return repo;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    @Override
    public int compareTo(Dependency other) {
        return name.compareTo(other.getName());
    }
}
