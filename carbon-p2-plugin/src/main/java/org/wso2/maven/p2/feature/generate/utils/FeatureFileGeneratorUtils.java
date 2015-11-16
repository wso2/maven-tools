/*
 *  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.maven.p2.feature.generate.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.wso2.maven.p2.exceptions.MissingRequiredPropertyException;
import org.wso2.maven.p2.feature.generate.Advice;
import org.wso2.maven.p2.feature.generate.Bundle;
import org.wso2.maven.p2.feature.generate.Feature;
import org.wso2.maven.p2.feature.generate.FeatureResourceBundle;
import org.wso2.maven.p2.utils.BundleUtils;
import org.wso2.maven.p2.utils.P2Utils;
import org.wso2.maven.p2.utils.PropertyReplacer;
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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

/**
 * Generate output files that are needed to generate a particular feature.
 */
public class FeatureFileGeneratorUtils {

    private static final String DEFAULT_ENCODING = "UTF-8";
    private static final String LINE_SEPARATOR = System.lineSeparator();

    /**
     * Generates the feature property file.
     *
     * @param resourceBundle      containing the project resources
     * @param featurePropertyFile File Object representing the feature property file
     * @throws IOException
     */
    public static void createPropertiesFile(FeatureResourceBundle resourceBundle, File featurePropertyFile)
            throws IOException, MissingRequiredPropertyException {
        Properties props = getProperties(resourceBundle);
        if (props != null && !props.isEmpty()) {
            try (OutputStream propertyFileStream = new FileOutputStream(featurePropertyFile)) {
                resourceBundle.getLog().info("Generating feature properties");
                props.store(propertyFileStream, "Properties of " + resourceBundle.getId());
            } catch (IOException e) {
                throw new IOException("Unable to create the feature.properties file", e);
            }
        }
    }

    /**
     * Merge properties passed into the maven plugin as properties and via the properties file.
     *
     * @param resourceBundle containing the project resources
     * @return Properties object containing properties passed in to the tool as properties and via the properties file
     * @throws IOException
     */
    private static Properties getProperties(FeatureResourceBundle resourceBundle) throws IOException,
            MissingRequiredPropertyException {
        Properties props = resourceBundle.getProperties();
        Properties propertiesFromFiles = getMergedPropertiesFromFiles(resourceBundle);

        if (props != null) {
            for (Object key : props.keySet().toArray()) {
                propertiesFromFiles.setProperty(key.toString(), props.getProperty(key.toString()));
            }
        }
        resourceBundle.setProperties(propertiesFromFiles);
        return propertiesFromFiles;
    }

    private static Properties getMergedPropertiesFromFiles(FeatureResourceBundle resourceBundle) throws IOException,
            MissingRequiredPropertyException {
        Properties props = new Properties();
        File propertyFileFromResourceDir = resourceBundle.getPropertyFileInResourceDir();
        URL propertyFileFromConfig = resourceBundle.getPropertyFile();
        if (propertyFileFromResourceDir.exists()) {
            InputStream propertyFileStream = new FileInputStream(propertyFileFromResourceDir);
            props.load(propertyFileStream);
        }
        if (propertyFileFromConfig != null) {
            InputStream propertyFileStream = propertyFileFromConfig.openStream();
            props.load(propertyFileStream);
        }
        if (!props.containsKey("copyright") && !props.containsKey("license")) {
            throw new MissingRequiredPropertyException("Mandatory properties are not specified in the property files");
        }
        return props;
    }

    /**
     * Creates manifest file for a feature.
     *
     * @param resourceBundle      containing the project resources
     * @param featureManifestFile File Object representing the manifest file
     * @throws IOException
     */
    public static void createManifestMFFile(FeatureResourceBundle resourceBundle, File featureManifestFile)
            throws IOException {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(featureManifestFile), DEFAULT_ENCODING);
             PrintWriter pw = new PrintWriter(writer)) {
            resourceBundle.getLog().info("Generating MANIFEST.MF");
            pw.print("Manifest-Version: 1.0" + LINE_SEPARATOR + LINE_SEPARATOR);
        } catch (IOException e) {
            throw new IOException("Unable to create manifest file", e);
        }
    }

    /**
     * Generates the P2Inf file.
     *
     * @param resourceBundle containing the project resources
     * @param p2InfFile      File object representing the p2inf file
     * @throws IOException
     */
    public static void createP2Inf(FeatureResourceBundle resourceBundle, File p2InfFile) throws IOException {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(p2InfFile.getAbsolutePath()), DEFAULT_ENCODING);
             PrintWriter pw = new PrintWriter(writer)) {
            List<Advice> list = resourceBundle.getAdviceFileContent();
            List<String> p2infStringList = null;

            if (p2InfFile.exists()) {
                p2infStringList = readAdviceFile(p2InfFile.getAbsolutePath());
                resourceBundle.getLog().info("Updating Advice file (p2.inf)");
            } else {
                resourceBundle.getLog().info("Generating Advice file (p2.inf)");
            }

            Properties properties = new Properties();
            properties.setProperty("feature.version", BundleUtils.getOSGIVersion(resourceBundle.getVersion()));
            if (p2infStringList != null && p2infStringList.size() > 0) {
                for (String str : p2infStringList) {
                    // writing the strings after replacing ${feature.version}
                    pw.write(PropertyReplacer.replaceProperties(str, properties) + LINE_SEPARATOR);
                }
            }
            if (list.size() != 0) {
                int nextIndex = P2Utils.getLastIndexOfProperties(p2InfFile) + 1;
                for (Advice category : list) {
                    pw.write(LINE_SEPARATOR + "properties." + nextIndex + ".name=" + category.getName());
                    pw.write(LINE_SEPARATOR + "properties." + nextIndex + ".value=" + category.getValue());
                    nextIndex++;
                }
            }
        } catch (UnsupportedEncodingException e) {
            resourceBundle.getLog().error("Unable to read p2.inf file. Unsupported encoding in existing file.");
            throw e;
        } catch (IOException e) {
            throw new IOException("Unable to create/open p2.inf file", e);
        }
    }

    /**
     * Read a given advice file in the pom.xml and return the items in the advice file in a String list.
     *
     * @param absolutePath Path to the advice file
     * @return List&lt;String&gt; containing items in the given advice file
     * @throws IOException
     */
    private static List<String> readAdviceFile(String absolutePath) throws IOException {
        List<String> stringList = new ArrayList<>();
        String inputLine;

        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(absolutePath), DEFAULT_ENCODING);
             BufferedReader br = new BufferedReader(reader)) {
            while ((inputLine = br.readLine()) != null) {
                stringList.add(inputLine);
            }
        } catch (IOException e) {
            throw new IOException("Error while reading from p2.inf file", e);
        }
        return stringList;
    }

    /**
     * Generates the feature.xml file for a feature.
     *
     * @param resourceBundle containing the project resources
     * @param featureXmlFile File object representing the feature xml file
     * @throws TransformerException
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    public static void createFeatureXml(FeatureResourceBundle resourceBundle, File featureXmlFile)
            throws TransformerException, IOException, SAXException, ParserConfigurationException {
        resourceBundle.getLog().info("Generating feature manifest");
        Document document = getManifestDocument(resourceBundle.getManifest());
        Element rootElement = document.getDocumentElement();
        if (rootElement == null) {
            rootElement = document.createElement("feature");
            document.appendChild(rootElement);
        }
        if (!rootElement.hasAttribute("id")) {
            rootElement.setAttribute("id", resourceBundle.getId());
        }
        if (!rootElement.hasAttribute("label")) {
            rootElement.setAttribute("label", resourceBundle.getLabel());
        }
        if (!rootElement.hasAttribute("version")) {
            rootElement.setAttribute("version", BundleUtils.getOSGIVersion(resourceBundle.getVersion()));
        }
        if (!rootElement.hasAttribute("provider-name")) {
            rootElement.setAttribute("provider-name", resourceBundle.getProviderName());
        }

        NodeList descriptionTags = rootElement.getElementsByTagName("description");
        if (descriptionTags.getLength() == 0) {
            Node description = document.createElement("description");
            description.setTextContent(resourceBundle.getDescription());
            rootElement.appendChild(description);
        }

        NodeList copyrightTags = rootElement.getElementsByTagName("copyright");

        if (copyrightTags.getLength() == 0) {
            Node copyright = document.createElement("copyright");
            copyright.setTextContent(resourceBundle.getCopyright());
            rootElement.appendChild(copyright);
        }

        NodeList licenseTags = rootElement.getElementsByTagName("license");
        if (licenseTags.getLength() == 0) {
            Node license = document.createElement("license");
            ((Element) license).setAttribute("url", resourceBundle.getLicenceUrl());
            license.setTextContent(resourceBundle.getLicence());
            rootElement.appendChild(license);
        }

        ArrayList<Bundle> processedMissingPlugins = getMissingPlugins(resourceBundle.getBundles(), document);

        List<Feature> missingImportFeatures = getMissingImportFeatures(resourceBundle.
                getImportFeatures(), document, "feature");
        List<Feature> includedFeatures = resourceBundle.getIncludeFeatures();

        //region Updating feature.xml with missing plugins
        for (Bundle bundle : processedMissingPlugins) {
            Element plugin = document.createElement("plugin");
            plugin.setAttribute("id", bundle.getSymbolicName());
            plugin.setAttribute("version", bundle.getBundleVersion());
            plugin.setAttribute("unpack", "false");
            rootElement.appendChild(plugin);
        }
        //endregion

        //region Updating feature.xml with missing  import plugins and features
        NodeList requireNodes = document.getElementsByTagName("require");
        Node require;
        if (requireNodes == null || requireNodes.getLength() == 0) {
            require = document.createElement("require");
            rootElement.appendChild(require);
        } else {
            require = requireNodes.item(0);
        }

        for (Feature feature : missingImportFeatures) {
            if (!feature.isOptional()) {
                Element plugin = document.createElement("import");
                plugin.setAttribute("feature", feature.getId());
                plugin.setAttribute("version", feature.getVersion());
                if (P2Utils.isPatch(feature.getCompatibility())) {
                    plugin.setAttribute("patch", "true");
                } else {
                    plugin.setAttribute("match", P2Utils.getMatchRule(feature.getCompatibility()));
                }
                require.appendChild(plugin);
            }
        }

        if (includedFeatures != null) {
            for (Feature includedFeature : includedFeatures) {
                Element includeElement = document.createElement("includes");
                includeElement.setAttribute("id", includedFeature.getId());
                includeElement.setAttribute("version", includedFeature.getVersion());
                includeElement.setAttribute("optional", Boolean.toString(includedFeature.isOptional()));
                rootElement.appendChild(includeElement);
            }
        }

        for (Feature feature : missingImportFeatures) {
            if (feature.isOptional()) {
                Element includeElement = document.createElement("includes");
                includeElement.setAttribute("id", feature.getId());
                includeElement.setAttribute("version", feature.getVersion());
                includeElement.setAttribute("optional", Boolean.toString(feature.isOptional()));
                rootElement.appendChild(includeElement);
            }
        }
        //endregion

        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer;
            transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(featureXmlFile);
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(source, result);
        } catch (TransformerConfigurationException e) {
            throw new TransformerConfigurationException("Unable to create feature manifest", e);
        }
    }

    /**
     * If a manifest file is given, parse the manifest file and return the Document object representing the file.
     * Generates a new Document otherwise.
     *
     * @param manifest java.io.File pointing an existing manifest file.
     * @return Document object representing a given manifest file or a newly generated manifest file
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    private static Document getManifestDocument(File manifest) throws ParserConfigurationException,
            SAXException, IOException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder;
        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new ParserConfigurationException("Unable to load feature manifest");
        }
        Document document;
        if (manifest != null && manifest.exists()) {
            try (InputStream manifestFileStream = new FileInputStream(manifest)) {
                document = documentBuilder.parse(manifestFileStream);
            } catch (SAXException e) {
                throw new SAXException("Unable to parse feature manifest", e);
            } catch (IOException e) {
                throw new IOException("Unable to load feature manifest", e);
            }
        } else {
            document = documentBuilder.newDocument();
        }
        return document;
    }

    /**
     * Cross check plugins given in the manifest file against the plugins configured in the pom.xml file. Returns a
     * list of bundles found in the pom.xml but not in the manifest file.
     *
     * @param bundles  list of bundles configured in the pom.xml
     * @param document Document representing the give manifest
     * @return ArrayList&lt;Bundle&gt; missing plugins
     */
    private static ArrayList<Bundle> getMissingPlugins(List<Bundle> bundles, Document document) {
        if (bundles == null || bundles.size() == 0) {
            return new ArrayList<>();
        }
        HashMap<String, Bundle> missingPlugins = new HashMap<>();
        for (Bundle bundle : bundles) {
            missingPlugins.put(bundle.getArtifactId(), bundle);
        }

        NodeList existingPlugins = document.getDocumentElement().getElementsByTagName("plugin");

        if (existingPlugins != null) {
            for (int i = 0; i < existingPlugins.getLength(); i++) {
                Node node = existingPlugins.item(i);
                Node namedItem = node.getAttributes().getNamedItem("id");
                if (namedItem != null && namedItem.getTextContent() != null &&
                        missingPlugins.containsKey(namedItem.getTextContent())) {
                    missingPlugins.remove(namedItem.getTextContent());
                }
            }
        }

        return new ArrayList<>(missingPlugins.values());
    }

    /**
     * Cross check import bundles/import features in the given manifest file against the plugins configured in the
     * pom.xml file. Returns a list of import bundles/import features found in the pom.xml but not in the manifest file.
     *
     * @param processedImportItemsList list of import plugins/import features configured in the pom.xml
     * @param document                 Document representing the give manifest
     * @param itemType                 String type, either "feature" or "plugin"
     * @return ArrayList<Feature>
     */
    private static ArrayList<Feature> getMissingImportFeatures(List<Feature> processedImportItemsList, Document document,
                                                               String itemType) {
        if (processedImportItemsList == null) {
            return new ArrayList<>();
        }
        HashMap<String, Feature> missingImportItems = new HashMap<>();
        for (Feature item : processedImportItemsList) {
            missingImportItems.put(item.getId(), item);
        }
        NodeList requireNodeList = document.getDocumentElement().getElementsByTagName("require");
        if (requireNodeList == null || requireNodeList.getLength() == 0) {
            return new ArrayList<>(missingImportItems.values());
        }

        Node requireNode = requireNodeList.item(0);
        if (requireNode instanceof Element) {
            Element requireElement = (Element) requireNode;
            NodeList importNodes = requireElement.getElementsByTagName("import");
            //Findbugs finds the following code as an unnecessary null check
//            if (importNodes == null) {
//                return new ArrayList<T>(missingImportItems.values());
//            }
            for (int i = 0; i < importNodes.getLength(); i++) {
                Node node = importNodes.item(i);
                Node namedItem = node.getAttributes().getNamedItem(itemType);
                if (namedItem != null && namedItem.getTextContent() != null &&
                        missingImportItems.containsKey(namedItem.getTextContent())) {
                    missingImportItems.remove(namedItem.getTextContent());
                }
            }
        }
        return new ArrayList<>(missingImportItems.values());
    }
}
