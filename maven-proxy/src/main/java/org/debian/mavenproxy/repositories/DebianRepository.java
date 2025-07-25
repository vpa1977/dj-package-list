package org.debian.mavenproxy.repositories;

import org.debian.maven.repo.Dependency;
import org.debian.maven.repo.DependencyRuleSet;
import org.debian.mavenproxy.Artifact;
import org.debian.mavenproxy.RuleParser;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

public class DebianRepository extends AbstractRepository {

    private final DependencyRuleSet ignoreRuleSet;
    private final DependencyRuleSet replaceRuleSet;

    public DebianRepository(String localRepositoryBasePath, List<String> replaceRules, List<String> ignoreRules) {
        super(localRepositoryBasePath);
        this.ignoreRuleSet = RuleParser.parseRules(ignoreRules, "ignore");
        this.replaceRuleSet = RuleParser.parseRules(replaceRules, "replace");
    }

    @Override
    public RepositoryContent getArtifact(String groupId, String artifactId, String version, String fileName) throws FileNotFoundException {
        if (fileName.endsWith(".sha1")) {
            throw new RuntimeException("Unable to request sha1 from Debian Repository");
        }
        String ext = fileName.substring(fileName.lastIndexOf(".")+1);

        if (!ignoreRuleSet.findMatchingRules(new Dependency(groupId, artifactId, ext, version)).isEmpty()) {
            if (ext.equals("pom")) {
                return new RepositoryContent(new Artifact("org.debian", "placeholder", "1.0", "pom"), new ByteArrayInputStream(getPom(groupId, artifactId, version)));
            } else if (ext.equals("jar")) {
                return new RepositoryContent(new Artifact("org.debian", "placeholder", "1.0", "jar"), new ByteArrayInputStream(getJarBytes()));
            }
            throw new RuntimeException("Unknown file type "+ ext);
        }

        Path testPath = Path.of(getBase(), groupId.replace(".", "/"), artifactId);
        File[] children = testPath.toFile().listFiles();
        if (children == null) {
            throw new FileNotFoundException(testPath.toString());
        }
        for (File f : children){
            // improve this later, for now we just use the first one
            String foundVersion = f.getName(); // do not do anything yet

            String newFileName = artifactId + "-"+ version + "." + ext;
            Path requestedPath = f.toPath().resolve(newFileName);
            if ("pom".equals(ext)) {
                try {
                    return new RepositoryContent(new Artifact(groupId, artifactId, foundVersion, ext), new ByteArrayInputStream(mapPom(requestedPath.toFile(), groupId, artifactId, version)));
                }
                catch (IOException | ParserConfigurationException | SAXException| TransformerException e) {
                    throw new RuntimeException(e.getMessage());
                }
            } else  if ("jar".equals(ext)) {
                return new RepositoryContent(new Artifact(groupId, artifactId, foundVersion, ext), new FileInputStream(requestedPath.toFile()));
            } else {
                throw new RuntimeException("Unsupported extension "+ ext + " file "+ f);
            }
        }
        throw new FileNotFoundException(testPath.toString());
    }

    public static byte[] mapPom(
            File inputFile,
            String newGroupId,
            String newArtifactId,
            String newVersion) throws IOException, ParserConfigurationException, SAXException, TransformerException {

        if (!inputFile.exists()) {
            throw new IOException("Input POM file not found: " + inputFile);
        }

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(inputFile);

        doc.getDocumentElement().normalize();
        Node project = doc.getElementsByTagName("project").item(0);
        NodeList nodes = project.getChildNodes();
        for (int i = 0; i < nodes.getLength() ; ++i) {
            Node n = nodes.item(i);
            if (n.getNodeName().equals("groupId")) {
                n.setTextContent(newGroupId);
            }
            if (n.getNodeName().equals("artifactId")) {
                n.setTextContent(newArtifactId);
            }
            if (n.getNodeName().equals("version")) {
                n.setTextContent(newVersion);
            }
        }
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        DOMSource source = new DOMSource(doc);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        StreamResult result = new StreamResult(bos);

        transformer.transform(source, result);
        return bos.toByteArray();
    }

    private static byte[] getJarBytes() {
        return new byte[] {80, 75, 3, 4, 10, 0, 0, 8, 0, 0, -121, 105, -7, 90, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 9, 0, 4, 0, 77, 69, 84, 65, 45, 73, 78, 70, 47, -2, -54, 0, 0, 80, 75, 3, 4, 20, 0, 8, 8, 8, 0, -121, 105, -7, 90, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 20, 0, 0, 0, 77, 69, 84, 65, 45, 73, 78, 70, 47, 77, 65, 78, 73, 70, 69, 83, 84, 46, 77, 70, -13, 77, -52, -53, 76, 75, 45, 46, -47, 13, 75, 45, 42, -50, -52, -49, -77, 82, 48, -44, 51, -32, -27, 114, 46, 74, 77, 44, 73, 77, -47, 117, -86, -76, 82, 48, 50, -43, 77, 77, 84, -48, 8, 77, 42, -51, 43, 41, -43, -28, -27, -30, -27, 2, 0, 80, 75, 7, 8, -27, 100, -39, 120, 55, 0, 0, 0, 53, 0, 0, 0, 80, 75, 3, 4, 20, 0, 8, 8, 8, 0, -125, 105, -7, 90, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 9, 0, 0, 0, 101, 109, 112, 116, 121, 46, 116, 120, 116, 51, -28, 2, 0, 80, 75, 7, 8, 83, -4, 81, 103, 4, 0, 0, 0, 2, 0, 0, 0, 80, 75, 1, 2, 10, 0, 10, 0, 0, 8, 0, 0, -121, 105, -7, 90, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 9, 0, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 77, 69, 84, 65, 45, 73, 78, 70, 47, -2, -54, 0, 0, 80, 75, 1, 2, 20, 0, 20, 0, 8, 8, 8, 0, -121, 105, -7, 90, -27, 100, -39, 120, 55, 0, 0, 0, 53, 0, 0, 0, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 43, 0, 0, 0, 77, 69, 84, 65, 45, 73, 78, 70, 47, 77, 65, 78, 73, 70, 69, 83, 84, 46, 77, 70, 80, 75, 1, 2, 20, 0, 20, 0, 8, 8, 8, 0, -125, 105, -7, 90, 83, -4, 81, 103, 4, 0, 0, 0, 2, 0, 0, 0, 9, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -92, 0, 0, 0, 101, 109, 112, 116, 121, 46, 116, 120, 116, 80, 75, 5, 6, 0, 0, 0, 0, 3, 0, 3, 0, -76, 0, 0, 0, -33, 0, 0, 0, 0, 0};
    }
    private static byte[] getPom(String groupId, String artifactId, String versionId) {
        return String.format("""
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<groupId>%s</groupId>
	<artifactId>%s</artifactId>
	<version>%s</version>
	<packaging>jar</packaging>
</project>
                """, groupId, artifactId, versionId).getBytes(StandardCharsets.UTF_8);
    }
}
