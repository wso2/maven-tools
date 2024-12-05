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

import org.apache.maven.plugin.AbstractMojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.File;

/**
 * @goal generate
 * @phase compile
 */
public class ConnectorMojo extends AbstractMojo {

    private String connectorName;
    private String packageName;

    public void execute() throws MojoExecutionException {

        populateConnectorData();
        ConnectorXmlGenerator.generateConnectorXml(connectorName, packageName, this);
        ComponentXmlGenerator.generateComponentXmls(this);
        DescriptorGenerator.generateDescriptor(this);
    }

    private void populateConnectorData() throws MojoExecutionException {

        String pomFilePath = "pom.xml"; // Update with your pom.xml path
        try {
            // Parse the POM file
            File pomFile = new File(pomFilePath);
            if (!pomFile.exists()) {
                throw new MojoExecutionException("POM file not found at: " + pomFilePath);
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(pomFile);

            // Normalize the XML structure
            document.getDocumentElement().normalize();

            // Get connector.name property
            this.connectorName = getProperty(document, "connector.name");
            if (connectorName == null || connectorName.isEmpty()) {
                throw new MojoExecutionException("The property 'connector.name' is not defined in the POM file.");
            }

            // Get groupId and artifactId to construct package name
            String groupId = getTagValue(document, "groupId");

            if (groupId == null) {
                throw new MojoExecutionException("The 'groupId' is not defined in the POM file.");
            }

            // Generate package name
            this.packageName = groupId;

        } catch (Exception e) {
            throw new MojoExecutionException("Populating connector data failed", e);
        }
    }

    /**
     * Gets the value of a property from the POM file's <properties> section.
     *
     * @param document     The XML document of the POM file.
     * @param propertyName The name of the property to retrieve.
     * @return The value of the property, or null if not found.
     */
    private static String getProperty(Document document, String propertyName) {

        NodeList propertiesNodeList = document.getElementsByTagName("properties");
        if (propertiesNodeList.getLength() > 0) {
            Node propertiesNode = propertiesNodeList.item(0);
            if (propertiesNode.getNodeType() == Node.ELEMENT_NODE) {
                Element propertiesElement = (Element) propertiesNode;
                NodeList childNodes = propertiesElement.getChildNodes();
                for (int i = 0; i < childNodes.getLength(); i++) {
                    Node child = childNodes.item(i);
                    if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals(propertyName)) {
                        return child.getTextContent().trim();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Gets the value of a specific tag from the POM file.
     *
     * @param document The XML document of the POM file.
     * @param tagName  The name of the tag to retrieve.
     * @return The value of the tag, or null if not found.
     */
    private static String getTagValue(Document document, String tagName) {

        NodeList nodeList = document.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            Node node = nodeList.item(0);
            return node.getTextContent().trim();
        }
        return null;
    }
}
