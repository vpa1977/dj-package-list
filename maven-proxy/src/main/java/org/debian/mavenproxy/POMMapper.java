package org.debian.mavenproxy;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
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

        if (newGroupId != null) {
            NodeList groupIdList = doc.getElementsByTagName("groupId");
            if (groupIdList.getLength() > 0) {
                Element groupIdElement = (Element) groupIdList.item(0);
                groupIdElement.setTextContent(newGroupId);
            }
        }

        if (newArtifactId != null) {
            NodeList artifactIdList = doc.getElementsByTagName("artifactId");
            if (artifactIdList.getLength() > 0) {
                Element artifactIdElement = (Element) artifactIdList.item(0);
                artifactIdElement.setTextContent(newArtifactId);
            }
        }

        if (newVersion != null) {
            NodeList versionList = doc.getElementsByTagName("version");
            if (versionList.getLength() > 0) {
                Element versionElement = null;
                for (int i = 0; i < versionList.getLength(); i++) {
                    Element currentVersionElement = (Element) versionList.item(i);
                    if (currentVersionElement.getParentNode().getNodeName().equals("project")) {
                        versionElement = currentVersionElement;
                        break;
                    }
                }

                if (versionElement != null) {
                    versionElement.setTextContent(newVersion);
                }
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
