package org.debian.mavenproxy;

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
import java.io.File;
import java.io.IOException;


public class POMMapper {

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
}
