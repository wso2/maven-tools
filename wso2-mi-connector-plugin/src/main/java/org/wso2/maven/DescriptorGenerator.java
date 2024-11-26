/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.maven;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

public class DescriptorGenerator {

    /**
     * Goes through the pom.xml file and generates the dependency.xml for the connector
     *
     * @return the generated descriptor
     */
    public static void generateDescriptor(ConnectorMojo connectorMojo) {

        String pomFilePath = "pom.xml";
        String outputFilePath = Constants.DEFAULT_TARGET_FOLDER + File.separator + Constants.CLASSES +
                Constants.DEPENDENCY_XML;
        try {
            // Parse the POM file and load properties
            Map<String, String> properties = parsePomProperties(pomFilePath);

            // Parse dependencies and generate dependency.xml
            generateDependencyXml(pomFilePath, outputFilePath, properties);
            connectorMojo.getLog().info("Dependency file generated at: " + outputFilePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Parses the <properties> section of the POM file and loads them into a map.
     *
     * @param pomFilePath Path to the pom.xml file.
     * @return A map of properties (key-value pairs).
     * @throws Exception If parsing fails or the file is missing.
     */
    private static Map<String, String> parsePomProperties(String pomFilePath) throws Exception {

        Map<String, String> propertiesMap = new HashMap<>();

        File pomFile = new File(pomFilePath);
        if (!pomFile.exists()) {
            throw new Exception("POM file not found at: " + pomFilePath);
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(pomFile);

        // Normalize the XML structure
        document.getDocumentElement().normalize();

        // Get the <properties> element
        NodeList propertiesNodeList = document.getElementsByTagName("properties");
        if (propertiesNodeList.getLength() > 0) {
            Node propertiesNode = propertiesNodeList.item(0);

            if (propertiesNode.getNodeType() == Node.ELEMENT_NODE) {
                Element propertiesElement = (Element) propertiesNode;

                // Iterate over child nodes of <properties>
                NodeList childNodes = propertiesElement.getChildNodes();
                for (int i = 0; i < childNodes.getLength(); i++) {
                    Node child = childNodes.item(i);

                    if (child.getNodeType() == Node.ELEMENT_NODE) {
                        String key = child.getNodeName();
                        String value = child.getTextContent().trim();
                        propertiesMap.put(key, value);
                    }
                }
            }
        }

        return propertiesMap;
    }

    /**
     * Generates the dependency.xml file by parsing dependencies from the POM file.
     *
     * @param pomFilePath    Path to the pom.xml file.
     * @param outputFilePath Path to the output dependency.xml file.
     * @param properties     Map of resolved properties.
     * @throws Exception If an error occurs during parsing or writing.
     */
    private static void generateDependencyXml(String pomFilePath, String outputFilePath, Map<String, String> properties)
            throws Exception {

        File pomFile = new File(pomFilePath);
        if (!pomFile.exists()) {
            throw new Exception("POM file not found at: " + pomFilePath);
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(pomFile);

        // Normalize the XML structure
        document.getDocumentElement().normalize();

        // Get the <dependency> elements
        NodeList dependencyNodes = document.getElementsByTagName("dependency");

        StringBuilder xmlBuilder = new StringBuilder();
        xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xmlBuilder.append("<descriptor>\n");
        xmlBuilder.append("    <dependencies>\n");

        for (int i = 0; i < dependencyNodes.getLength(); i++) {
            Node dependencyNode = dependencyNodes.item(i);
            if (dependencyNode.getNodeType() == Node.ELEMENT_NODE) {
                Element dependencyElement = (Element) dependencyNode;
                String groupId = getTextContent(dependencyElement, "groupId");
                String artifactId = getTextContent(dependencyElement, "artifactId");
                String version = getTextContent(dependencyElement, "version");
                String scope = getTextContent(dependencyElement, "scope");

                // Filter dependencies with runtime scope
                if ("runtime".equals(scope)) {
                    // Resolve version if it contains a placeholder
                    if (version.startsWith("${") && version.endsWith("}")) {
                        String propertyKey = version.substring(2, version.length() - 1);
                        version = properties.get(propertyKey);

                        if (version == null) {
                            throw new Exception(
                                    "Version placeholder '" + propertyKey + "' is not defined in the POM properties.");
                        }
                    }

                    xmlBuilder.append("        <dependency>\n");
                    xmlBuilder.append("            <groupId>").append(groupId).append("</groupId>\n");
                    xmlBuilder.append("            <artifactId>").append(artifactId).append("</artifactId>\n");
                    xmlBuilder.append("            <version>").append(version).append("</version>\n");
                    xmlBuilder.append("        </dependency>\n");
                }
            }
        }
        xmlBuilder.append("    </dependencies>\n");
        xmlBuilder.append("    <repositories>\n");

        // Get the <repository> elements
        NodeList repositoryNodes = document.getElementsByTagName("repository");
        for (int i = 0; i < repositoryNodes.getLength(); i++) {
            Node repositoryNode = repositoryNodes.item(i);
            if (repositoryNode.getNodeType() == Node.ELEMENT_NODE) {
                Element repositoryElement = (Element) repositoryNode;
                String id = getTextContent(repositoryElement, "id");
                String url = getTextContent(repositoryElement, "url");

                xmlBuilder.append("        <repository>\n");
                xmlBuilder.append("            <id>").append(id).append("</id>\n");
                xmlBuilder.append("            <url>").append(url).append("</url>\n");
                xmlBuilder.append("        </repository>\n");
            }
        }
        xmlBuilder.append("    </repositories>\n");
        xmlBuilder.append("</descriptor>\n");

        // Write the XML to the output file
        try (FileWriter writer = new FileWriter(outputFilePath)) {
            writer.write(xmlBuilder.toString());
        }
    }

    /**
     * Utility method to get the text content of an XML element.
     *
     * @param parent  The parent element.
     * @param tagName The tag name of the child element.
     * @return The text content, or an empty string if not found.
     */
    private static String getTextContent(Element parent, String tagName) {

        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent().trim();
        }
        return "";
    }
}
