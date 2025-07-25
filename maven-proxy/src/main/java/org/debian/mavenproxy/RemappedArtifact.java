package org.debian.mavenproxy;

public record RemappedArtifact(Artifact art, String originalRequestPath, String newRequestPath) {
}
