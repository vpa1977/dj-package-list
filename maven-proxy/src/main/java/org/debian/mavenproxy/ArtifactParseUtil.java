package org.debian.mavenproxy;

public class ArtifactParseUtil {
    public record Artifact(String groupId, String name, String version, String type) {}
    public static Artifact parse(String requestPath){
        String path = requestPath.startsWith("/") ? requestPath.substring(1) : requestPath;
        int fileLocation = path.lastIndexOf("/");
        path = path.substring(0, fileLocation);
        fileLocation = path.lastIndexOf("/");

        int typeLocation = requestPath.lastIndexOf(".");
        String type = "jar";
        if (typeLocation > 0) {
            type = requestPath.substring(typeLocation+1);
        }

        String version;
        if (fileLocation > 0) {
            version = path.substring(fileLocation + 1);
        } else {
            throw new RuntimeException("Invalid artifact request path "+ requestPath + " no version");
        }
        path = path.substring(0, fileLocation);
        fileLocation = path.lastIndexOf("/");

        String artifactId;
        if (fileLocation > 0) {
            artifactId = path.substring(fileLocation + 1);
        } else {
            throw new RuntimeException("Invalid artifact request path "+ requestPath + " no artifact Id");
        }
        return new Artifact( path.substring(0, fileLocation).replace("/", "."), artifactId, version, type);
    }
}
