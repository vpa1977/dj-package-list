package org.debian.javapackage.dependency;

import java.util.HashSet;

public record SourcePackage(String name, HashSet<String> binaryPackages, HashSet<String> buildDeps) {
}
