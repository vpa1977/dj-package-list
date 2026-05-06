package org.debian.javapackage.dependency;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;

/*
Package: abseil
Format: 3.0 (quilt)
Binary: libabsl-dev, libabsl20230802
Architecture: any
Version: 20230802.1-4.2
Priority: optional
Section: misc
Maintainer: Benjamin Barenblat <bbaren@debian.org>
Standards-Version: 4.6.2
Build-Depends: cmake (>= 3.13), debhelper-compat (= 12), dpkg-dev (>= 1.22.5), googletest (>= 1.12), tzdata
Testsuite: autopkgtest
Testsuite-Triggers: cmake, g++, libgmock-dev, libgtest-dev, make, pkg-config
Homepage: https://abseil.io/
Description: extensions to the C++ standard library
 Abseil is an open-source collection of C++ library code designed to augment the
 C++ standard library. The Abseil library code is collected from Google's C++
 codebase and has been extensively tested and used in production. In some cases,
 Abseil provides pieces missing from the C++ standard; in others, Abseil
 provides alternatives to the standard for special needs.
Vcs-Browser: https://salsa.debian.org/debian/abseil

 */
public class SourceListParser {

    private static Packages readSources(String distribution, String[] pockets) throws IOException {

        Packages sources = new Packages(HashMap.newHashMap(1), HashMap.newHashMap(1));
        for (var pocket : pockets) {
            for (var p : new String[]{distribution, distribution + "-updates", distribution + "-security", distribution + "-proposed"}) {
                var main = URI.create(String.format("https://archive.ubuntu.com/ubuntu/dists/%s/%s/source/Sources.gz", distribution, pocket));
                var url = main.toURL();
                try (InputStream is = url.openStream(); GZIPInputStream gz = new GZIPInputStream(is); ) {
                    Packages filePackages = read(gz);
                    sources.append(filePackages);
                }
            }
        }
        return sources;
    }

    /*
     * a map of
     * src:<source-package-name> -> source package
     * <binary-package-name> -> source package
     */
    private static Packages read(InputStream is) {
        HashMap<String, SourcePackage> sourcePackages = new HashMap<>();
        HashMap<String, SourcePackage> binaryPackages = new HashMap<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(is))) {
            String line = null;
            String packageName = null;
            String version = null;
            HashSet<String> binaryPackageNames = null;
            HashSet<String> buildDependencies = null;
            while ((line = r.readLine()) != null) {
                if (line.startsWith("Package:")) {
                    if (packageName != null) {
                        add(sourcePackages, binaryPackages, packageName, version, binaryPackageNames, buildDependencies);
                    }
                    packageName = line.substring("Package: ".length()).trim();
                    binaryPackageNames = new HashSet<>();
                    buildDependencies = new HashSet<>();
                }
                if (line.startsWith("Version:")) {
                    version = line.substring("Version: ".length()).trim();
                }
                if (line.startsWith("Build-Depends")) {
                    addDependencies(line, buildDependencies);
                }
                if (line.startsWith("Binary")) {
                    addDependencies(line, binaryPackageNames);
                }
            }
            add(sourcePackages, binaryPackages, packageName, version, binaryPackageNames, buildDependencies);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new Packages(sourcePackages, binaryPackages);
    }

    private static void add(HashMap<String, SourcePackage> sourcePackages, HashMap<String, SourcePackage> binaryPackages, String packageName, String version, HashSet<String> binaryPackageNames, HashSet<String> buildDependencies) {
        SourcePackage sp = new SourcePackage(packageName, version, binaryPackageNames, buildDependencies);
        var existing = sourcePackages.get(packageName);
        if (existing != null) {
            if (existing.version().compareTo(version) < 0) {
                sourcePackages.put(packageName, sp);
                for (var p : binaryPackageNames) {
                    binaryPackages.put(p, sp);
                }
            }
        } else {
            sourcePackages.put(packageName, sp);
            for (var p : binaryPackageNames) {
                binaryPackages.put(p, sp);
            }
        }
    }

    private static void addDependencies(String line, HashSet<String> dependencies) {
        line = line.substring(line.indexOf(":") + 1);
        StringTokenizer tk = new StringTokenizer(line, ",");
        while (tk.hasMoreTokens()) {
            String nextPackage = tk.nextToken().trim();
            if (nextPackage.indexOf(' ') != -1) nextPackage = nextPackage.substring(0, nextPackage.indexOf(' ')).trim();
            if (nextPackage.indexOf(':') != -1) nextPackage = nextPackage.substring(0, nextPackage.indexOf(':')).trim();
            dependencies.add(nextPackage);
        }
    }

    private HashSet<String> findBinaryReverseDependencies(Collection<String> packages, ReverseDependencies reverseDependencies) {
        HashSet<String> binaryReverseDependencies = new HashSet<>();
        for (var p : packages) {
            for (var sourcePackage : reverseDependencies.getReverseDependencies(p)) {
                binaryReverseDependencies.addAll(sourcePackage.binaryPackages());
            }
        }
        return binaryReverseDependencies;
    }

    public ArrayList<String> findSourceDependencies(String[] packages, String distribution, String[] pockets) throws IOException {
        var sources = readSources(distribution, pockets);
        var reverseDependencies = new ReverseDependencies(sources);
        var sourcePackages = new HashSet<String>();
        for (var p : packages) {
            for (var sp : reverseDependencies.getReverseDependencies(p)) {
                sourcePackages.add(sp.name());
            }
        }
        return new ArrayList<>(sourcePackages);
    }

    /**
     * Finds all reverse-dependencies of the package set
     */
    public ArrayList<Dependencies> findBinaryReverseDependencies(String[] packages, String distribution, String[] pockets) throws IOException {
        var sources = readSources(distribution, pockets);
        return getDependencies(packages, sources);
    }

    private ArrayList<Dependencies> getDependencies(String[] packages, Packages sources) {
        var reverseDependencies = new ReverseDependencies(sources);

        ArrayList<Dependencies> result = new ArrayList<>();

        HashSet<String> allDependencies = new HashSet<>();
        HashSet<String> dependencies = findBinaryReverseDependencies(Arrays.asList(packages), reverseDependencies);
        allDependencies.addAll(dependencies);
        int size = 0;
        int distance = 0;
        result.add(new Dependencies(dependencies, distance));

        while (allDependencies.size() > size) {
            size = allDependencies.size();
            dependencies = findBinaryReverseDependencies(dependencies, reverseDependencies);
            result.add(new Dependencies(new HashSet<>(dependencies.stream().filter(x -> !allDependencies.contains(x)).toList()), ++distance));
            allDependencies.addAll(dependencies);
        }
        return result;
    }

    public List<String> findBuiltWith(String[] packages, String suite, String[] pockets) throws IOException {
        var sources = readSources(suite, pockets);
        final HashSet<String> wanted = new HashSet<>(Arrays.asList(packages));
        return sources.sourcePackages.values().stream().filter( p ->
                p.buildDeps().stream().anyMatch(wanted::contains))
                .map(SourcePackage::binaryPackages).flatMap(Set::stream)
                .map( x -> x + "=" + sources.binaryPackages.get(x).version())
                .toList();

    }

    /**
     * @sourcePackages set of dependant source packages
     * @distance distance from the original set of dependencies.
     * 0 - immediate dependencies
     */
    public record Dependencies(HashSet<String> binaryPackages, int distance) {
    }

    public record Packages(HashMap<String, SourcePackage> sourcePackages,
                           HashMap<String, SourcePackage> binaryPackages) {
        public void append(Packages other) {
            sourcePackages.putAll(other.sourcePackages);
            binaryPackages.putAll(other.binaryPackages);
        }
    }

    public class ReverseDependencies {
        private final HashMap<String, Set<SourcePackage>> reverseDepends = new HashMap<>();

        public ReverseDependencies(Packages p) {
            for (var pack : p.sourcePackages.values()) {
                for (var buildDep : pack.buildDeps()) {
                    addReverseDependency(buildDep, pack);
                }
            }
        }

        private void addReverseDependency(String buildDep, SourcePackage pack) {
            var reverse = reverseDepends.get(buildDep);
            if (reverse == null) {
                reverseDepends.put(buildDep, new HashSet<>(Collections.singletonList(pack)));
                return;
            }
            reverse.add(pack);
        }

        public Set<SourcePackage> getReverseDependencies(String binaryPackage) {
            var deps = reverseDepends.get(binaryPackage);
            if (deps == null) {
                return new HashSet<>();
            }
            return deps;
        }
    }


}
