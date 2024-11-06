/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.maven.handler;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.wso2.maven.DockerMojo;
import org.wso2.maven.metadata.Application;
import org.wso2.maven.metadata.Artifact;
import org.wso2.maven.metadata.RegistryInfo;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import static org.wso2.maven.handler.HandlerConstants.RESOURCES_FOLDER;

public class RegistryResourceHandler implements ArtifactHandler {
    public static final String SYSTEM_PREFIX = "/_system/";
    public static final String METADATA_MEDIA_TYPE_KEY = "mediaType";
    private static final String URL_SEPARATOR = "/";
    private final DockerMojo dockerMojo;
    private final Path tmpCarbonHomeDir;

    public RegistryResourceHandler(DockerMojo dockerMojo, Path tmpCarbonHomeDir) {
        this.dockerMojo = dockerMojo;
        this.tmpCarbonHomeDir = tmpCarbonHomeDir;
    }
    @Override
    public void copyArtifacts(Application application) {
        Path destArtifactDirPath = tmpCarbonHomeDir.resolve(HandlerConstants.REGISTRY_RESOURCES_FOLDER);
        try {
            Files.createDirectories(destArtifactDirPath);
        } catch (IOException e) {
            dockerMojo.logError(String.format("Error occurred while creating the directory: %s. error: %s",
                    destArtifactDirPath, e.getMessage()));
        }

        application.getApplicationArtifact().getDependencies().forEach(dependency -> {
            if (HandlerConstants.REGISTRY_RESOURCE_TYPE.equals(dependency.getArtifact().getType())) {
                deployArtifacts(dependency.getArtifact(), application.getAppName());
            }
        });
    }

    private void deployArtifacts(Artifact artifact, String appName) {
        if (HandlerUtils.validateArtifact(artifact) && artifact.getFile() != null) {
            RegistryInfo registryInfo = buildRegistryInfo(artifact, appName);
            writeRegistryResourceFiles(registryInfo);
            writeRegistryCollections(registryInfo);
        }
    }

    private void writeRegistryCollections(RegistryInfo registryInfo) {
        // write collections
        List<RegistryInfo.Collection> collections = registryInfo.getCollections();
        collections.forEach(collection -> {
            String filePath = registryInfo.getExtractedPath() + File.separator + RESOURCES_FOLDER
                    + File.separator + collection.getDirectory();

            // check whether the file exists
            File file = new File(filePath);
            if (!file.exists()) {
                dockerMojo.logError("Specified file to be written as a resource is " + "not found at : " + filePath);
                return;
            }
            addCollectionToRegistry(collection, file);
        });
    }

    private void addCollectionToRegistry(RegistryInfo.Collection collection, File file) {
        String targetPath = resolveRegistryPath(collection.getPath());
        String collectionName = getResourceName(targetPath);
        Properties properties = collection.getProperties();
        if (!targetPath.endsWith(URL_SEPARATOR)) {
            targetPath += URL_SEPARATOR;
        }
        String parentPath = targetPath.substring(0, targetPath.lastIndexOf(URL_SEPARATOR));
        try {
            File collectionFile = new File(new URI(targetPath));
            if (!collectionFile.exists() && !collectionFile.mkdirs()) {
                throw new IOException("Unable to create collection: " + collection.getPath());
            }
            if (properties != null && !properties.isEmpty()) {
                Path propertiesPath = Paths.get(parentPath, collectionName + ".collections.properties");
                properties.store(Files.newOutputStream(propertiesPath), "properties");
            }
        } catch (URISyntaxException | IOException e) {
            dockerMojo.logError(String.format(
                    "Error occurred while adding registry collection: %s, error: %s", collectionName, e.getMessage()));
        }
        writeToFile(targetPath, collection.getDirectory(), null, collection.getProperties(), file);
    }

    private void writeRegistryResourceFiles(RegistryInfo registryInfo) {
        // write resources
        List<RegistryInfo.Resource> resources = registryInfo.getResources();
        resources.forEach(resource -> {
            String filePath = registryInfo.getExtractedPath() + File.separator + RESOURCES_FOLDER
                    + File.separator + resource.getFileName();
            // check whether the file exists
            File file = new File(filePath);
            if (!file.exists()) {
                dockerMojo.logError("Specified file to be written as a resource is " + "not found at : " + filePath);
                return;
            }
            addResourceToRegistry(resource, file);
        });
    }

    private void addResourceToRegistry(RegistryInfo.Resource resource, File file) {
        String targetPath = resolveRegistryPath(resource.getPath());
        String mediaType = resource.getMediaType();
        String fileName = resource.getFileName();
        Properties properties = resource.getProperties();
        Properties metadata = null;
        if (mediaType != null) {
            metadata = new Properties();
            metadata.setProperty(METADATA_MEDIA_TYPE_KEY, mediaType);
        }
        writeToFile(targetPath, fileName, metadata, properties, file);
    }

    private void writeToFile(String targetPath, String fileName, Properties metadata, Properties properties, File file) {
        Path destArtifactDirPath = tmpCarbonHomeDir.resolve(HandlerConstants.REGISTRY_RESOURCES_FOLDER).resolve(targetPath);
        try {
            Files.createDirectories(destArtifactDirPath);
            Path destArtifactPath = destArtifactDirPath.resolve(fileName);
            Files.copy(file.toPath(), destArtifactPath);
            dockerMojo.getLog().debug("Copying from: " + file + " to: " + destArtifactPath);
            if (metadata != null) {
                Path metadataDirPath = destArtifactPath.resolveSibling(".metadata");
                Files.createDirectories(metadataDirPath);
                Path metadataPath = metadataDirPath.resolve(fileName + ".meta");
                metadata.store(Files.newOutputStream(metadataPath), "metadata");
            }
            if (properties != null) {
                Path propertiesPath = destArtifactPath.resolveSibling(fileName + ".properties");
                properties.store(Files.newOutputStream(propertiesPath), "properties");
            }
        } catch (IOException e) {
            dockerMojo.logError(String.format(
                    "Error occurred while adding registry resource: %s, error: %s", fileName, e.getMessage()));
        }
    }

    private String resolveRegistryPath(String path) {
        if (path != null && path.startsWith(SYSTEM_PREFIX)) {
            return path.substring(SYSTEM_PREFIX.length());
        }
        return path;
    }

    private String getResourceName(String path) {
        if (path != null) {
            String correctedPath = path;
            if (path.endsWith(URL_SEPARATOR)) {
                correctedPath = path.substring(0, path.lastIndexOf(URL_SEPARATOR));
            }
            return correctedPath.substring(correctedPath.lastIndexOf(URL_SEPARATOR) + 1);
        }
        return "";
    }

    private RegistryInfo buildRegistryInfo(Artifact artifact, String appName) {
        String regInfoFilepath = artifact.getExtractedPath() + File.separator + artifact.getFile();
        File regConfigFile = new File(regInfoFilepath);
        RegistryInfo registryInfo = null;

        if (regConfigFile.exists()) {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            try (FileInputStream fis = new FileInputStream(regConfigFile)) {
                XMLStreamReader reader = factory.createXMLStreamReader(fis);
                OMElement documentElement = OMXMLBuilderFactory.createStAXOMBuilder(reader).getDocumentElement();
                registryInfo = populateRegistryInfo(documentElement);
            } catch (Exception e) {
                dockerMojo.logError(String.format("Error occurred while reading the registry resource file: %s, error: %s", regInfoFilepath, e.getMessage()));
            }

            if (registryInfo != null) {
                registryInfo.setAppName(appName);
                registryInfo.setExtractedPath(artifact.getExtractedPath());
                registryInfo.setParentArtifactName(artifact.getName());
                registryInfo.setConfigFileName(artifact.getFile());
            }
        }  else {
            dockerMojo.logError("Registry resource file not found: " + regInfoFilepath);
        }
        return registryInfo;
    }

    private RegistryInfo populateRegistryInfo(OMElement documentElement) {
        if (documentElement == null) {
            return null;
        }
        // Parse the registry-info.xml file and populate the RegistryInfo object
        RegistryInfo registryInfo = new RegistryInfo();

        // read Item elements under Resources
        Iterator itemItr = documentElement.getChildrenWithLocalName(RegistryInfo.ITEM);
        while (itemItr.hasNext()) {
            OMElement itemElement = (OMElement) itemItr.next();

            registryInfo.addResource(readChildText(itemElement, RegistryInfo.PATH),
                    readChildText(itemElement, RegistryInfo.FILE),
                    readChildText(itemElement, RegistryInfo.REGISTRY_TYPE),
                    readChildText(itemElement, RegistryInfo.MEDIA_TYPE),
                    readProperties(itemElement));
        }

        // read Collection elements under Resources
        Iterator collectionItr = documentElement.getChildrenWithLocalName(RegistryInfo.COLLECTION);
        while (collectionItr.hasNext()) {
            OMElement collectionElement = (OMElement) collectionItr.next();
            registryInfo.addCollection(readChildText(collectionElement, RegistryInfo.PATH),
                    readChildText(collectionElement, RegistryInfo.DIRECTORY),
                    readChildText(collectionElement, RegistryInfo.REGISTRY_TYPE),
                    readProperties(collectionElement));
        }

        // read Association elements under Resources
        Iterator associationItr = documentElement.getChildrenWithLocalName(RegistryInfo.ASSOCIATION);
        while (associationItr.hasNext()) {
            OMElement associationElement = (OMElement) associationItr.next();
            registryInfo.addAssociation(readChildText(associationElement, RegistryInfo.SOURCE_PATH),
                    readChildText(associationElement, RegistryInfo.TARGET_PATH),
                    readChildText(associationElement, RegistryInfo.TYPE),
                    readChildText(associationElement, RegistryInfo.REGISTRY_TYPE));
        }

        // read Dump elements under Resources
        Iterator dumpItr = documentElement.getChildrenWithLocalName(RegistryInfo.DUMP);
        while (dumpItr.hasNext()) {
            OMElement dumpElement = (OMElement) dumpItr.next();
            registryInfo.addDump(readChildText(dumpElement, RegistryInfo.PATH),
                    readChildText(dumpElement, RegistryInfo.FILE),
                    readChildText(dumpElement, RegistryInfo.REGISTRY_TYPE));
        }
        return registryInfo;
    }

    private static String readChildText(OMElement element, String name) {
        if (element == null) {
            return null;
        }
        OMElement temp = element.getFirstChildWithName(new QName(name));
        if (temp != null) {
            return temp.getText();
        }
        return null;
    }

    private static Properties readProperties(OMElement artifactEle) {
        // read the properties
        Properties props = new Properties();
        OMElement properties = artifactEle.getFirstChildWithName(new QName("properties"));
        if (properties == null) {
            return null;
        }
        Iterator itr = properties.getChildrenWithLocalName("property");
        while (itr.hasNext()) {
            OMElement depElement = (OMElement) itr.next();
            String key = depElement.getAttribute(new QName("key")).getAttributeValue();
            String value = depElement.getAttribute(new QName("value")).getAttributeValue();
            props.setProperty(key, value);
        }
        if (props.isEmpty()) {
            return null;
        }
        return props;
    }

}
