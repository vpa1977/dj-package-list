package org.debian.mavenproxy;

import java.util.HashMap;

class ContentTypes {
    private final HashMap<String, String> contentTypes = new HashMap<>();

    public ContentTypes() {
        contentTypes.put(".ear", "application/java-archive");
        contentTypes.put(".jar", "application/java-archive");
        contentTypes.put(".war", "application/java-archive");
        contentTypes.put(".pom", "application/xml");
        contentTypes.put(".xml", "application/xml");
        contentTypes.put(".sha1", "text/plain");
        contentTypes.put(".md5", "text/plain");
    }

    public String determineContentType(String filename) {
        if (filename == null) {
            return getContentType("");
        }
        int where = filename.lastIndexOf(".");
        if (where > 0) {
            return getContentType(filename.substring(where));
        }
        return getContentType("");
    }

    public String getContentType(String extension) {
        var type = contentTypes.get(extension);
        if (type == null) {
            return "application/octet-stream";
        }
        return type;
    }
}
