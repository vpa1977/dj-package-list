package org.debian.mavenproxy;

public record RemappedArtifact(ArtifactParseUtil.Artifact art, String originalRequestPath, String newRequestPath) {
}
