package org.debian.mavenproxy;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
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
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class POMMapper {

    public static Map<ArtifactParseUtil.Artifact, String> getOriginalVersions(File inputFile)  throws IOException, ParserConfigurationException, SAXException {
        // debian stores original dependency version in project/properties/debian.<groupId>.artifactId.originalVersion
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(inputFile);
        Node project = doc.getElementsByTagName("project").item(0);
        Node propertyNode = firstChildNode(project, "properties");
        NodeList nl = propertyNode.getChildNodes();
        HashMap<ArtifactParseUtil.Artifact, String> arts = new HashMap<>();
        for (int i = 0; i < nl.getLength(); ++i) {
            Node n = nl.item(i);
            if ("debian.originalVersion".equals(n.getNodeName())) {
                continue; // we have already got this pom, no need to record
            }
            if (n.getNodeName().endsWith(".originalVersion")) {
                ArtifactParseUtil.Artifact art = parseOriginalVersion(n.getNodeName());
                String originalVersion = n.getTextContent();
                arts.put(art, originalVersion);
            }
        }
        return arts;
    }

    public static void mapPom(
            File inputFile,
            File outputFile,
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
        StreamResult result = new StreamResult(outputFile);

        transformer.transform(source, result);
    }

    private static ArtifactParseUtil.Artifact parseOriginalVersion(String nodeName) {
        if (!nodeName.startsWith("debian.")) {
            throw new RuntimeException(nodeName + " should start with debian.");
        }
        nodeName = nodeName.substring("debian.".length());
        nodeName = nodeName.substring(0, nodeName.lastIndexOf("."));
        String groupId = nodeName.substring(0, nodeName.lastIndexOf("."));
        String artifactId = nodeName.substring(nodeName.lastIndexOf(".") + 1);
        return new ArtifactParseUtil.Artifact(groupId, artifactId, "debian", "pom");
    }

    private static Node firstChildNode(Node n, String nodeName) {
        NodeList nl = n.getChildNodes();
        for (int i = 0 ;i < nl.getLength() ; ++i) {
            if (nodeName.equals(nl.item(i).getNodeName())) {
                return nl.item(i);
            }
        }
        return null;
    }
}
