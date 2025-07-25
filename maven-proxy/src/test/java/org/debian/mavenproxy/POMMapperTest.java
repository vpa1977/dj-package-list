package org.debian.mavenproxy;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class POMMapperTest {

    @Test
    public void testParseOriginalVersion() throws Exception {
        File input = Files.createTempFile("input", ".pom").toFile();
        Files.writeString(input.toPath(), """
<project xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>parentNode</groupId>
    </parent>
    <groupId>org.debian</groupId>
    <artifactId>maven-proxy</artifactId>
    <version>1.0.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.release>21</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <httpclient.version>4.5.14</httpclient.version>
        <httpcomponents.version>4.4.16</httpcomponents.version>
        <sqlite.jdbc.version>3.46.1.3</sqlite.jdbc.version>
        <slf4j.version>1.7.32</slf4j.version>
        <debian.com.google.code.findbugs.jsr305.originalVersion>3.0.2</debian.com.google.code.findbugs.jsr305.originalVersion>
        <debian.com.google.code.gson.gson.originalVersion>2.9.1</debian.com.google.code.gson.gson.originalVersion>
    </properties>

    <dependencies>
        <!-- Apache HttpComponents for HTTP Server and Client -->
        <dependency>
            <groupId>org.apache.httpcomponents</groupId>
            <artifactId>httpclient</artifactId>
            <version>${httpclient.version}</version>
        </dependency>
    </dependencies>
</project>                
                """);

        var ret = POMMapper.getOriginalVersions(input);
        assertEquals("3.0.2", ret.get(new Artifact("com.google.code.findbugs", "jsr305", "debian", "pom")));
        assertEquals("2.9.1", ret.get(new Artifact("com.google.code.gson", "gson", "debian", "pom")));
    }

    @Test
    public void testReplaceInPom() throws Exception {
        File input = Files.createTempFile("input", ".pom").toFile();
        File output = Files.createTempFile("output", ".pom").toFile();
        Files.writeString(input.toPath(), """
<project xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>parentNode</groupId>
    </parent>
    <groupId>org.debian</groupId>
    <artifactId>maven-proxy</artifactId>
    <version>1.0.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.release>21</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <httpclient.version>4.5.14</httpclient.version>
        <httpcomponents.version>4.4.16</httpcomponents.version>
        <sqlite.jdbc.version>3.46.1.3</sqlite.jdbc.version>
        <slf4j.version>1.7.32</slf4j.version>
    </properties>

    <dependencies>
        <!-- Apache HttpComponents for HTTP Server and Client -->
        <dependency>
            <groupId>org.apache.httpcomponents</groupId>
            <artifactId>httpclient</artifactId>
            <version>${httpclient.version}</version>
        </dependency>
    </dependencies>
</project>                
                """);
        POMMapper.mapPom(input, output, "foo", "bar", "foobar");
        String result = Files.readString(output.toPath());

        assertTrue(result.contains("<groupId>foo</groupId>"));
        assertTrue(result.contains("<artifactId>bar</artifactId>"));
        assertTrue(result.contains("<version>foobar</version>"));
        assertTrue(result.contains("<groupId>org.apache.httpcomponents</groupId>"));
        assertTrue(result.contains("<artifactId>httpclient</artifactId>"));
        assertTrue(result.contains("<version>${httpclient.version}</version>"));
        assertTrue(result.contains("<groupId>parentNode</groupId>"));
    }
}
