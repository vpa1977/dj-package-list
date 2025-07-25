package org.debian.mavenproxy.request;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ContentTypesTest {

    @Test
    public void testExtensions() {
        ContentTypes tp = new ContentTypes();
        assertEquals("application/octet-stream", tp.determineContentType(""));
        assertEquals("application/octet-stream", tp.determineContentType("message"));
        assertEquals("application/octet-stream", tp.determineContentType(null));
        assertEquals("application/java-archive", tp.determineContentType("foo.jar"));
        assertEquals("text/plain", tp.determineContentType("foo.jar.sha1"));
    }
}
