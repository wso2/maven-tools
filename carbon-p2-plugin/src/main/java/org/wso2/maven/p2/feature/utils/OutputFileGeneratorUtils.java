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

package org.wso2.maven.p2.feature.utils;

import org.apache.maven.plugin.MojoExecutionException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.wso2.maven.p2.feature.FeatureResourceBundle;
import org.wso2.maven.p2.beans.Bundle;
import org.wso2.maven.p2.beans.ImportFeature;
import org.wso2.maven.p2.beans.IncludedFeature;
import org.wso2.maven.p2.beans.Property;
import org.wso2.maven.p2.utils.BundleUtils;
import org.wso2.maven.p2.utils.P2Utils;
import org.wso2.maven.p2.utils.PropertyReplacer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

public class OutputFileGeneratorUtils {

    public static void createPropertiesFile(FeatureResourceBundle resourceBundle, File featurePropertyFile) throws MojoExecutionException {
        Properties props = getProperties(resourceBundle);
        if (props != null && !props.isEmpty()) {
            try {
                resourceBundle.getLog().info("Generating feature properties");
                props.store(new FileOutputStream(featurePropertyFile), "Properties of " + resourceBundle.getId());
            } catch (Exception e) {
                throw new MojoExecutionException("Unable to create the feature properties", e);
            }
        }
    }

    private static Properties getProperties(FeatureResourceBundle resourceBundle) throws MojoExecutionException {
        File propertiesFile = resourceBundle.getPropertiesFile();
        Properties properties = resourceBundle.getProperties();
        if (propertiesFile != null && propertiesFile.exists()) {
            Properties props = new Properties();
            try {
                props.load(new FileInputStream(propertiesFile));
            } catch (Exception e) {
                throw new MojoExecutionException("Unable to load the given properties file", e);
            }
            if (properties != null) {
                for (Object key : properties.keySet().toArray()) {
                    props.setProperty(key.toString(), properties.getProperty(key.toString()));
                }
            }
            properties = props;
            resourceBundle.setProperties(props);
        }
        return properties;
    }

    public static void createManifestMFFile(FeatureResourceBundle resourceBundle, File featureManifestFile) throws MojoExecutionException {
        try {
            resourceBundle.getLog().info("Generating MANIFEST.MF");
            BufferedWriter out = new BufferedWriter(new FileWriter(featureManifestFile));
            out.write("Manifest-Version: 1.0\n\n");
            out.close();
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to create manifest file", e);
        }
    }

    public static void createP2Inf(FeatureResourceBundle resourceBundle, File p2InfFile) throws MojoExecutionException {
        BufferedWriter out = null;
        List<String> p2infStringList = null;
        try {
            ArrayList<Property> list = resourceBundle.getProcessedAdviceProperties();

            if (p2InfFile.exists()) {
                p2infStringList = readAdviceFile(p2InfFile.getAbsolutePath());
                resourceBundle.getLog().info("Updating Advice file (p2.inf)");
            } else {
                resourceBundle.getLog().info("Generating Advice file (p2.inf)");
            }

            out = new BufferedWriter(new FileWriter(p2InfFile.getAbsolutePath()));
            //re-writing the already availabled p2.inf lines
            Properties properties = new Properties();
            properties.setProperty("feature.version", BundleUtils.getOSGIVersion(resourceBundle.getVersion()));
            if (p2infStringList != null && p2infStringList.size() > 0) {
                for (String str : p2infStringList) {
                    out.write(PropertyReplacer.replaceProperties(str, properties) + "\n"); // writing the strings after replacing ${feature.version}
                }
            }
            if (list.size() == 0) {
                return;    // finally block will take care of output stream closing.
            }
            int nextIndex = P2Utils.getLastIndexOfProperties(p2InfFile) + 1;
            for (Object category : list) {
                Property cat = (Property) category;
                out.write("\nproperties." + nextIndex + ".name=" + cat.getKey());
                out.write("\nproperties." + nextIndex + ".value=" + cat.getValue());
                nextIndex++;
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to create/open p2.inf file", e);
        } finally {
            if (out != null)
                try {
                    out.close();
                } catch (IOException e) {
                    throw new MojoExecutionException("Unable to finalize p2.inf file", e);
                }
        }
    }

    private static List<String> readAdviceFile(String absolutePath) throws MojoExecutionException {
        List<String> stringList = new ArrayList<String>();
        String inputLine = null;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(absolutePath));
            while ((inputLine = br.readLine()) != null) {
                stringList.add(inputLine);
            }
        } catch (FileNotFoundException e) {
            throw new MojoExecutionException("Unable to create/open p2.inf file", e);
        } catch (IOException e) {
            throw new MojoExecutionException("Error while reading from p2.inf file", e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    throw new MojoExecutionException("Unable to finalize p2.inf file", e);
                }
            }
        }
        return stringList;
    }

    public static void createFeatureXml(FeatureResourceBundle resourceBundle, File featureXmlFile) throws MojoExecutionException {
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

        ArrayList<Object> processedMissingPlugins = getMissingPlugins(resourceBundle, document);
        ArrayList<Object> processedMissingImportPlugins = getMissingImportPlugins(resourceBundle, document);
        ArrayList<Object> processedMissingImportFeatures = getMissingImportFeatures(resourceBundle, document);
        ArrayList<IncludedFeature> includedFeatures = resourceBundle.getProcessedIncludedFeatures();

        if (processedMissingPlugins != null) {
            for (Iterator<Object> iterator = processedMissingPlugins.iterator(); iterator.hasNext(); ) {
                Bundle bundle = (Bundle) iterator.next();
                Element plugin = document.createElement("plugin");
                plugin.setAttribute("id", bundle.getBundleSymbolicName());
                plugin.setAttribute("version", bundle.getBundleVersion());
                plugin.setAttribute("unpack", "false");
                rootElement.appendChild(plugin);
            }
        }

        if (processedMissingImportPlugins != null || processedMissingImportFeatures != null) {
            NodeList requireNodes = document.getElementsByTagName("require");
            Node require;
            if (requireNodes == null || requireNodes.getLength() == 0) {
                require = document.createElement("require");
                rootElement.appendChild(require);
            } else {
                require = requireNodes.item(0);
            }

            if (processedMissingImportPlugins != null) {
                for (Iterator<Object> iterator = processedMissingImportPlugins.iterator(); iterator.hasNext(); ) {
                    Bundle bundle = (Bundle) iterator.next();
                    Element plugin = document.createElement("import");
                    plugin.setAttribute("plugin", bundle.getBundleSymbolicName());
                    plugin.setAttribute("version", bundle.getBundleVersion());
                    plugin.setAttribute("match", P2Utils.getMatchRule(bundle.getCompatibility()));
                    require.appendChild(plugin);
                }
            }
            if (processedMissingImportFeatures != null) {
                for (Object processedMissingImportFeature : processedMissingImportFeatures) {
                    ImportFeature feature = (ImportFeature) processedMissingImportFeature;
                    if (!feature.isOptional()) {
                        Element plugin = document.createElement("import");
                        plugin.setAttribute("feature", feature.getFeatureId());
                        plugin.setAttribute("version", feature.getFeatureVersion());
                        if (P2Utils.isPatch(feature.getCompatibility())) {
                            plugin.setAttribute("patch", "true");
                        } else {
                            plugin.setAttribute("match", P2Utils.getMatchRule(feature.getCompatibility()));
                        }
                        require.appendChild(plugin);
                    }
                }
            }
        }

        if (includedFeatures != null) {
            for (IncludedFeature includedFeature : includedFeatures) {
                Element includeElement = document.createElement("includes");
                includeElement.setAttribute("id", includedFeature.getFeatureID());
                includeElement.setAttribute("version", includedFeature.getFeatureVersion());
                includeElement.setAttribute("optional", Boolean.toString(includedFeature.isOptional()));
                rootElement.appendChild(includeElement);
            }
        }

        if (processedMissingImportFeatures != null) {
            for (Object processedMissingImportFeature : processedMissingImportFeatures) {
                ImportFeature feature = (ImportFeature) processedMissingImportFeature;
                if (feature.isOptional()) {
                    Element includeElement = document.createElement("includes");
                    includeElement.setAttribute("id", feature.getFeatureId());
                    includeElement.setAttribute("version", feature.getFeatureVersion());
                    includeElement.setAttribute("optional", Boolean.toString(feature.isOptional()));
                    rootElement.appendChild(includeElement);
                }
            }
        }

        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer;
            transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(featureXmlFile);
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(source, result);
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to create feature manifest", e);
        }
    }

    private static Document getManifestDocument(File manifest) throws MojoExecutionException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder;
        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e1) {
            throw new MojoExecutionException("Unable to load feature manifest", e1);
        }
        Document document;
        if (manifest != null && manifest.exists()) {
            try {
                document = documentBuilder.parse(new FileInputStream(manifest));
            } catch (Exception e) {
                throw new MojoExecutionException("Unable to load feature manifest", e);
            }
        } else {
            document = documentBuilder.newDocument();
        }
        return document;
    }

    private static ArrayList<Object> getMissingPlugins(FeatureResourceBundle resourceBundle, Document document) throws MojoExecutionException {
        HashMap<String, Bundle> missingPlugins = new HashMap<String, Bundle>();
        ArrayList<Bundle> processedBundlesList = resourceBundle.getProcessedBundles();
        if (processedBundlesList == null || processedBundlesList.size() == 0) {
            return new ArrayList<Object>();
        }
        for (Bundle bundle : processedBundlesList) {
            missingPlugins.put(bundle.getArtifactId(), bundle);
        }

        NodeList existingPlugins = document.getDocumentElement().getElementsByTagName("plugin");

        for (int i = 0; i < existingPlugins.getLength(); i++) {
            Node node = existingPlugins.item(i);
            Node namedItem = node.getAttributes().getNamedItem("id");
            if (namedItem != null && namedItem.getTextContent() != null && missingPlugins.containsKey(namedItem.getTextContent())) {
                missingPlugins.remove(namedItem.getTextContent());
            }
        }

//        return returnArrayList(missingPlugins.values().toArray());
        return new ArrayList<Object>(missingPlugins.values());
    }

    //TODO Duplicate code in getMissingImportPlugins and getMissingImportFeatures method. extract and create a single method.
    private static ArrayList<Object> getMissingImportPlugins(FeatureResourceBundle resourceBundle, Document document) throws MojoExecutionException {
        HashMap<String, Bundle> missingImportPlugins = new HashMap<String, Bundle>();
        ArrayList<Bundle> processedImportBundlesList = resourceBundle.getProcessedImportBundles();

        if (processedImportBundlesList == null) {
            return null;
        }
        for (Bundle bundle : processedImportBundlesList) {
            missingImportPlugins.put(bundle.getArtifactId(), bundle);
        }

        NodeList requireNodeList = document.getDocumentElement().getElementsByTagName("require");
        if (requireNodeList == null || requireNodeList.getLength() == 0) {
//            return returnArrayList(missingImportPlugins.values().toArray());
            return new ArrayList<Object>(missingImportPlugins.values());
        }

        Node requireNode = requireNodeList.item(0);
        if (requireNode instanceof Element) {
            Element requireElement = (Element) requireNode;
            NodeList importNodes = requireElement.getElementsByTagName("import");
            if (importNodes == null) {
//                return returnArrayList(missingImportPlugins.values().toArray());
                return new ArrayList<Object>(missingImportPlugins.values());
            }
            for (int i = 0; i < importNodes.getLength(); i++) {
                Node node = importNodes.item(i);
                Node namedItem = node.getAttributes().getNamedItem("plugin");
                if (namedItem != null && namedItem.getTextContent() != null && missingImportPlugins.containsKey(namedItem.getTextContent())) {
                    missingImportPlugins.remove(namedItem.getTextContent());
                }
            }
        }
//        return returnArrayList(missingImportPlugins.values().toArray());
        return new ArrayList<Object>(missingImportPlugins.values());
    }

    private static ArrayList<Object> getMissingImportFeatures(FeatureResourceBundle resourceBundle, Document document) throws MojoExecutionException {
        HashMap<String, ImportFeature> missingImportFeatures = new HashMap<String, ImportFeature>();
        ArrayList<ImportFeature> processedImportFeaturesList = resourceBundle.getProcessedImportFeatures(); //processedImportFeatures;
        if (processedImportFeaturesList == null) {
            return null;
        }
        for (ImportFeature feature : processedImportFeaturesList) {
            missingImportFeatures.put(feature.getFeatureId(), feature);
        }

        NodeList requireNodeList = document.getDocumentElement().getElementsByTagName("require");
        if (requireNodeList == null || requireNodeList.getLength() == 0) {
//            return returnArrayList(missingImportFeatures.values().toArray());
            return new ArrayList<Object>(missingImportFeatures.values());
        }
        Node requireNode = requireNodeList.item(0);
        if (requireNode instanceof Element) {
            Element requireElement = (Element) requireNode;
            NodeList importNodes = requireElement.getElementsByTagName("import");
            if (importNodes == null) {
//                return returnArrayList(missingImportFeatures.values().toArray());
                return new ArrayList<Object>(missingImportFeatures.values());
            }
            for (int i = 0; i < importNodes.getLength(); i++) {
                Node node = importNodes.item(i);
                Node namedItem = node.getAttributes().getNamedItem("feature");
                if (namedItem != null && namedItem.getTextContent() != null && missingImportFeatures.containsKey(namedItem.getTextContent())) {
                    missingImportFeatures.remove(namedItem.getTextContent());
                }
            }
        }
        return new ArrayList<Object>(missingImportFeatures.values());
        //return returnArrayList(missingImportFeatures.values().toArray());
    }

//    private static ArrayList<Object> returnArrayList(Object[] arr) {
//        ArrayList<Object> arrayList = new ArrayList<Object>();
//        for (Object object : arr) {
//            arrayList.add(object);
//        }
//        return arrayList;
//    }
}
