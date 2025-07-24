package org.debian.mavenproxy.build;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GradleBuildExecutor extends BuildExecutor {
    public GradleBuildExecutor(Map<String, Object> config) {
        super(config);
    }

    @Override
    protected Map<String, String> getEnvironment() {
        HashMap<String, String> map = new HashMap<>();
        map.put("DEBIAN_MAVEN_REPO", "http://localhost:8080");
        map.put("DEBIAN_SKIP_MAVEN_RULES", "1");
        map.put("GRADLE_USER_HOME", getWorkingDirectory() + "/.gradle-cache");
        return map;
    }

    @Override
    protected String[] getCommand() {
        ArrayList<String> command = new ArrayList<>();
        command.add(getBuildCommand());
        command.add("--no-daemon");
        command.add("--init-script");
        command.add(getInitScript());
        command.addAll(getBuildArgs());
        return command.toArray(String[]::new);
    }

    private String getInitScript() {
        try {
            File initFile = File.createTempFile("init", ".gradle.kts");
            initFile.deleteOnExit();
            Files.writeString(initFile.toPath(),
                    String.format("""
                            initscript {
                                dependencies {
                                    classpath(files("%s/gradle8-helper-plugin.jar"))
                                    classpath(files("%s/maven-repo-helper.jar"))
                                }
                            }
                            
                            apply<org.debian.gradle.DebianHelperPlugin>()
                            
                            """, getHelperPath(), getHelperPath()));
            return initFile.getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
