package org.debian;

import org.debian.javapackage.dependency.SourceListParser;

import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

public class AppGetProposedMigration {
    public static void main(String[] args) throws Throwable {
        SourceListParser sourceListParser = new SourceListParser();
        var deps = sourceListParser.findSourceDependencies(new String[]{
                "default-jdk",
                "default-jdk-headless",
                "default-jre",
                "default-jre-headless",
                "openjdk-8-jdk",
                "openjdk-8-jdk-headless",
        }, "resolute", new String[]{"main", "universe"});
        var hSet = new HashSet<>(deps);
        var yaml = args.length > 0 ? args[0] : "/home/vladimirp/Downloads/update_excuses.yaml";

        // LoadSettings builder allows you to tweak limits, aliases, and custom constructors.
        LoadSettings settings = LoadSettings.builder()
                .setCodePointLimit(100 * 1024 * 1024)
                .build();

        // 2. Initialize the Load instance
        Load load = new Load(settings);
        String data = Files.readString(Path.of(yaml));
        Map<String, Object> excuses = (Map<String, Object> )load.loadFromString(data);
        ArrayList<Map<String,Object>> sources = (ArrayList<Map<String,Object>>) excuses.get("sources");
        ArrayList<String> toDo = new ArrayList<>();
        for (var source : sources) {
            String name = (String)source.get("item-name");
            if (hSet.contains(name)) {
                toDo.add(name);
            }
        }
        Collections.sort(toDo);
        for (var x : toDo) {
            System.out.println(x);
        }

    }
}
