package org.debian.mavenproxy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ArtifactParseUtilTest {
    @Test
    public void parseArtifact() {
        var art = ArtifactParseUtil.parse("/com/util/foo/1.0.1/test.pom");
        assertEquals("com.util", art.groupId());
        assertEquals( "foo", art.name());
        assertEquals("1.0.1", art.version() );
        assertEquals("pom", art.type() );

        art = ArtifactParseUtil.parse("/org/jetbrains/kotlin/kotlin-stdlib/2.0.21/kotlin-stdlib-2.0.21.module");
        assertEquals("org.jetbrains.kotlin", art.groupId());
        assertEquals( "kotlin-stdlib", art.name());
        assertEquals("2.0.21", art.version() );
        assertEquals("module", art.type() );
    }

    @Test
    public void testMapRequestPath() {
        String requestPath = "/commons-collections/commons-collections/3.2.2/commons-collections-3.2.2.pom";
        String result = ArtifactParseUtil.mapRequestPath(requestPath, "debian");
        assertEquals("/commons-collections/commons-collections/debian/commons-collections-debian.pom", result);

        requestPath = "commons-collections/commons-collections/3.2.2/commons-collections-3.2.2.pom";
        result = ArtifactParseUtil.mapRequestPath(requestPath, "debian");
        assertEquals("commons-collections/commons-collections/debian/commons-collections-debian.pom", result);
    }
}
