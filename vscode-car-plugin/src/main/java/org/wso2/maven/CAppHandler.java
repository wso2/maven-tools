/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.DeferredParsingException;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMDocument;
import org.apache.axiom.om.OMElement;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.wso2.maven.libraries.CAppDependencyResolver;
import org.wso2.maven.model.Artifact;
import org.wso2.maven.model.ArtifactDependency;
import org.wso2.maven.model.ArtifactDetails;
import org.wso2.maven.core.model.AbstractXMLDoc;

import static org.wso2.maven.libraries.CAppDependencyResolver.getTopLevelCAppDependencies;

public class CAppHandler extends AbstractXMLDoc {
    private final String cAppName;
    private final CARMojo mojoInstance;
    private final List<ArtifactDetails> artifactTypeList;
    private final Map<String, String> apiList = new HashMap<>();
    private final Map<String, String> proxyList = new HashMap<>();

    public CAppHandler(String cAppName, CARMojo mojoInstance) {
        this.cAppName = cAppName;
        this.mojoInstance = mojoInstance;
        //initialize artifactTypeList with values
        artifactTypeList = new ArrayList<>(Arrays.asList(
                new ArtifactDetails(Constants.API_DIR_NAME, Constants.API_TYPE, Constants.SERVER_ROLE_EI),
                new ArtifactDetails(Constants.ENDPOINTS_DIR_NAME, Constants.ENDPOINT_TYPE, Constants.SERVER_ROLE_EI),
                new ArtifactDetails(Constants.INBOUND_DIR_NAME, Constants.INBOUND_TYPE, Constants.SERVER_ROLE_EI),
                new ArtifactDetails(Constants.LOCAL_ENTRIES_DIR_NAME, Constants.LOCAL_ENTRY_TYPE, Constants.SERVER_ROLE_EI),
                new ArtifactDetails(Constants.MSG_PROCESSORS_DIR_NAME, Constants.MESSAGE_PROCESSOR_TYPE, Constants.SERVER_ROLE_EI),
                new ArtifactDetails(Constants.MSG_STORES_DIR_NAME, Constants.MESSAGE_STORE_TYPE, Constants.SERVER_ROLE_EI),
                new ArtifactDetails(Constants.PROXY_SERVICES_DIR_NAME, Constants.PROXY_SERVICE_TYPE, Constants.SERVER_ROLE_EI),
                new ArtifactDetails(Constants.SEQUENCES_DIR_NAME, Constants.SEQUENCE_TYPE, Constants.SERVER_ROLE_EI),
                new ArtifactDetails(Constants.TASKS_DIR_NAME, Constants.TASK_TYPE, Constants.SERVER_ROLE_EI),
                new ArtifactDetails(Constants.TEMPLATES_DIR_NAME, Constants.TEMPLATE_TYPE, Constants.SERVER_ROLE_EI),
                new ArtifactDetails(Constants.DATASOURCE_DIR_NAME, Constants.DATASOURCE_TYPE, Constants.SERVER_ROLE_DSS),
                new ArtifactDetails(Constants.DATASERVICES_DIR_NAME, Constants.DATASERVICE_TYPE, Constants.SERVER_ROLE_DSS)
        ));
    }

    /**
     * Method to process artifacts in the artifacts folder and create corresponding files in the archive directory.
     *
     * @param artifactsFolder  path to artifacts folder
     * @param archiveDirectory path to archive directory
     * @param dependencies     list of dependencies to be added to artifacts.xml file
     * @param version          version of the project
     */
    void processArtifacts(File artifactsFolder, String archiveDirectory, List<ArtifactDependency> dependencies,
                          List<ArtifactDependency> metadataDependencies, String version) {
        if (!artifactsFolder.exists()) {
            mojoInstance.logInfo("Could not find artifacts folder in " + artifactsFolder.getAbsolutePath());
            return;
        }
        mojoInstance.logInfo("Processing artifacts in " + artifactsFolder.getAbsolutePath());
        for (ArtifactDetails artifactDetails : artifactTypeList) {
            File artifactFolder = new File(artifactsFolder, artifactDetails.getDirectory());
            processArtifactsInFolder(artifactFolder, dependencies, metadataDependencies, version, archiveDirectory,
                    artifactDetails.getServerRole(), artifactDetails.getType());
        }
    }

    /**
     * Method to process given artifact type ex: api/proxy and create corresponding files in the archive directory.
     *
     * @param artifactsDir     path to artifacts folder
     * @param dependencies     list of dependencies to be added to artifacts.xml file
     * @param version          version of the project
     * @param archiveDirectory path to archive directory
     * @param serverRole       server role of the artifact
     * @param type             type of the artifact
     */
    void processArtifactsInFolder(File artifactsDir, List<ArtifactDependency> dependencies,
                                  List<ArtifactDependency> metadataDependencies, String version, String archiveDirectory,
                                  String serverRole, String type) {
        File[] configFiles = artifactsDir.listFiles();
        if (configFiles != null) {
            for (File configFile : configFiles) {
                if (configFile.isFile()) {
                    try {
                        String fileContent = FileUtils.readFileToString(configFile);
                        OMElement configElement = getElement(fileContent);
                        String name = configElement.getAttributeValue(new QName(Constants.NAME));
                        if (Constants.LOCAL_ENTRY_TYPE.equals(type)) {
                            name = configElement.getAttributeValue(new QName(Constants.KEY));
                        }
                        if (Constants.DATASOURCE_TYPE.equals(type)) {
                            // Remove .xml extension from the file name and use it as the artifact name
                            // since the name attribute is not available in the datasource configuration
                            name = configFile.getName().substring(0, configFile.getName().length() - 4);
                        }
                        String configVersion = configElement.getAttributeValue(new QName(Constants.VERSION));
                        boolean apiHasVersion = true;
                        if (Constants.API_TYPE.equals(type)) {
                            // api version can be null
                            apiList.put(name, configVersion);

                            if (!isMetadataPresent(name, configVersion, artifactsDir, "_metadata.yaml")) {
                                writeMetadataFile(name, configElement, archiveDirectory, Constants.API, version);
                                addMetadataDependencies(metadataDependencies, configElement, Constants.API, version);
                            }
                        }
                        if (Constants.PROXY_SERVICE_TYPE.equals(type)) {
                            proxyList.put(name, configVersion);
                            if (!isMetadataPresent(name, configVersion, artifactsDir,
                                    Constants.PROXY_FILE_NAME_SUFFIX)) {
                                writeMetadataFile(name, configElement, archiveDirectory, Constants.PROXY, version);
                                addMetadataDependencies(metadataDependencies, configElement, Constants.PROXY, version);
                            }
                        }
                        if (configVersion == null) {
                            apiHasVersion = false;
                            configVersion = version;
                        }
                        if (Constants.DATASERVICE_TYPE.equals(type)) {
                            if (!isMetadataPresent(name, configVersion, artifactsDir,
                                    Constants.DATA_SERVICE_FILE_NAME_SUFFIX)) {
                                writeMetadataFile(name, configElement, archiveDirectory,
                                        Constants.DATA_SERVICE, version);
                                addMetadataDependencies(metadataDependencies, configElement,
                                        Constants.DATA_SERVICE, version);
                            }
                        }
                        String fileName;
                        String folderName = "";
                        if (Constants.API_TYPE.equals(type) && apiHasVersion) {
                            // todo : need to fix this naming convention in runtime
                            fileName = name + "_" + configVersion + "-" + configVersion;
                            folderName = name + "_" + configVersion + "_" + configVersion;
                        } else {
                            fileName = name + "-" + configVersion;
                            folderName = name + "_" + configVersion;
                        }
                        fileName = fileName.concat(Constants.DATASERVICE_TYPE.equals(type) ? ".dbs" : ".xml");
                        name = apiHasVersion ? name + "_" + configVersion : name;
                        dependencies.add(new ArtifactDependency(name, configVersion, serverRole, true));
                        writeArtifactAndFile(configFile, archiveDirectory, name, type, serverRole, configVersion,
                                fileName, folderName);
                    } catch (IOException | XMLStreamException | DeferredParsingException e) {
                        mojoInstance.logError("Error occurred while processing " + configFile.getName());
                        mojoInstance.logError(e.getMessage());
                    }
                }
            }
        }
    }

    private boolean isMetadataPresent(String name, String configVersion, File artifactsDir, String suffix) {

        String metadataFilename;
        if (StringUtils.isBlank(configVersion)) {
            metadataFilename = name + suffix;
        } else {
            metadataFilename = name + "_" + configVersion + suffix;
        }
        File resourcesFolder = artifactsDir.toPath().getParent().getParent()
                .resolve(Constants.RESOURCES).toFile();
        File metadataFolder = new File(resourcesFolder, Constants.METADATA_DIR_NAME);
        File metaFile = new File(metadataFolder, metadataFilename);
        return metaFile.exists();
    }

    /**
     * Method to process resources folder and create corresponding files in the archive directory.
     *
     * @param resourcesFolder  path to resources folder
     * @param archiveDirectory path to archive directory
     * @param dependencies     list of dependencies to be added to artifacts.xml file
     */
    void processResourcesFolder(File resourcesFolder, String archiveDirectory, List<ArtifactDependency> dependencies,
                                List<ArtifactDependency> metadataDependencies, String version, MavenProject project) {
        if (!resourcesFolder.exists()) {
            mojoInstance.logInfo("Could not find resources folder in " + resourcesFolder.getAbsolutePath());
            return;
        }
        processConnectors(resourcesFolder, archiveDirectory, dependencies, Constants.CONNECTORS_DIR_NAME);
        if (MavenUtils.isConnectorPackingSupported(project)) {
            processConnectors(resourcesFolder, archiveDirectory, dependencies, Constants.INBOUND_CONNECTORS_DIR_NAME);
        }
        processRegistryResources(resourcesFolder, archiveDirectory, dependencies);
        processRegistryResources(new File(resourcesFolder, Constants.REGISTRY_DIR_NAME), archiveDirectory, dependencies);
        processMetadata(resourcesFolder, archiveDirectory, metadataDependencies, version);
        processPropertyFile(resourcesFolder, archiveDirectory, version, dependencies);
    }

    /**
     * Method to process connectors in the resources folder and create corresponding files in the archive directory.
     *
     * @param resourcesFolder  path to resources folder
     * @param archiveDirectory path to archive directory
     * @param dependencies     list of dependencies to be added to artifacts.xml file
     */
    void processConnectors(File resourcesFolder, String archiveDirectory, List<ArtifactDependency> dependencies, String dirName) {
        mojoInstance.logInfo("Processing connectors in " + resourcesFolder.getAbsolutePath());
        File connectorFolder = new File(resourcesFolder, dirName);
        if (!connectorFolder.exists()) {
            return;
        }
        File[] connectorFiles = connectorFolder.listFiles();
        if (connectorFiles == null) {
            return;
        }
        for (File connector : connectorFiles) {
            if (connector.isFile() && connector.getName().endsWith(".zip")) {
                String fileName = connector.getName();
                int lastIndex = fileName.lastIndexOf('-');
                String name = fileName.substring(0, lastIndex);
                // remove .zip at the end
                String version = fileName.substring(lastIndex + 1, fileName.length() - 4);
                dependencies.add(new ArtifactDependency(name, version, Constants.SERVER_ROLE_EI, true));
                writeArtifactAndFile(connector, archiveDirectory, name, Constants.CONNECTOR_TYPE,
                        Constants.SERVER_ROLE_EI, version, fileName, name + "_" + version);
            }
        }
    }

    void processPropertyFile(File resourcesFolder, String archiveDirectory, String version,
                             List<ArtifactDependency> dependencies) {
        File confFolder = new File(resourcesFolder, Constants.CONF_DIR_NAME);
        File propertyFile = new File(confFolder, Constants.PROPERTY_FILE);
        if (!propertyFile.exists()) {
            return;
        }
        mojoInstance.logInfo("Processing property file in " + confFolder.getAbsolutePath());
        writeArtifactAndFile(propertyFile, archiveDirectory, Constants.PROPERTY_FILE_NAME, Constants.PROPERTY_TYPE,
                Constants.SERVER_ROLE_EI, version, Constants.PROPERTY_FILE,
                Constants.PROPERTY_FILE_NAME + "_" + version);
        dependencies.add(new ArtifactDependency(Constants.PROPERTY_FILE_NAME, version, Constants.SERVER_ROLE_EI, true));
    }

    /**
     * Method to process registry resources in the resources folder and create corresponding files in the archive directory.
     *
     * @param resourcesFolder  path to resources folder
     * @param archiveDirectory path to archive directory
     * @param dependencies     list of dependencies to be added to artifacts.xml file
     */
    void processRegistryResources(File resourcesFolder, String archiveDirectory, List<ArtifactDependency> dependencies) {
        mojoInstance.logInfo("Processing registry resources in " + resourcesFolder.getAbsolutePath());
        File artifactFile = new File(resourcesFolder, Constants.ARTIFACT_XML);
        if (!artifactFile.exists()) {
            return;
        }
        try {
            String artifactXmlFileAsString = FileUtils.readFileToString(artifactFile);
            OMElement artifactsElement = getElement(artifactXmlFileAsString);
            List<OMElement> artifactChildElements = getChildElements(artifactsElement, Constants.ARTIFACT);
            for (OMElement artifact : artifactChildElements) {
                String name = artifact.getAttributeValue(new QName(Constants.NAME));
                String version = artifact.getAttributeValue(new QName(Constants.VERSION));
                String commonPath = Paths.get(archiveDirectory, name + "_" + version).toString();
                if (artifact.getFirstChildWithName(new QName(Constants.ITEM)) != null) {
                    OMElement item = getFirstChildWithName(artifact, Constants.ITEM);
                    String fileName = item.getFirstChildWithName(new QName(Constants.FILE)).getText();
                    String path = item.getFirstChildWithName(new QName(Constants.PATH)).getText();
                    File registryResource;
                    if (path.startsWith(Constants.GOV_MI_RESOURCES_PREFIX)) {
                        path = path.substring(Constants.GOV_MI_RESOURCES_PREFIX.length());
                        registryResource = new File(resourcesFolder, path + "/" + fileName);
                    } else if (path.startsWith(Constants.GOV_REG_PREFIX)) {
                        path = path.substring(Constants.GOV_REG_PREFIX.length());
                        registryResource = new File(resourcesFolder, Constants.GOV_FOLDER + path + "/" + fileName);
                    } else {
                        path = path.substring(Constants.CONF_REG_PREFIX.length());
                        registryResource = new File(resourcesFolder, Constants.CONF_FOLDER + path + "/" + fileName);
                    }
                    if (!registryResource.exists()) {
                        mojoInstance.logError("Registry resource " + path + "/" + fileName + " does not exist");
                        continue;
                    }
                    org.wso2.developerstudio.eclipse.utils.file.FileUtils.copy(registryResource,
                            new File(Paths.get(archiveDirectory, name + "_" + version, Constants.RESOURCES,
                                    fileName).toString()));
                    OMElement infoElement = getElement(Constants.RESOURCES, Constants.EMPTY_STRING);
                    infoElement.addChild(item);
                    org.wso2.developerstudio.eclipse.utils.file.FileUtils.createFile(
                            new File(commonPath, Constants.REG_INFO_FILE), serialize(infoElement));
                } else if (artifact.getFirstChildWithName(new QName(Constants.COLLECTION)) != null) {
                    OMElement collection = getFirstChildWithName(artifact, Constants.COLLECTION);
                    String directory = collection.getFirstChildWithName(new QName(Constants.DIRECTORY)).getText();
                    String path = collection.getFirstChildWithName(new QName(Constants.PATH)).getText();
                    File registryResource;
                    if (path.startsWith(Constants.GOV_MI_RESOURCES_PREFIX)) {
                        path = path.substring(Constants.GOV_MI_RESOURCES_PREFIX.length());
                        registryResource = new File(resourcesFolder, path);
                    } else if (path.startsWith(Constants.GOV_REG_PREFIX)) {
                        path = path.substring(Constants.GOV_REG_PREFIX.length());
                        registryResource = new File(resourcesFolder, Constants.GOV_FOLDER + path);
                    } else {
                        path = path.substring(Constants.CONF_REG_PREFIX.length());
                        registryResource = new File(resourcesFolder, Constants.CONF_FOLDER + path);
                    }
                    if (!registryResource.exists()) {
                        mojoInstance.logError("Registry resource " + path + " does not exist");
                        continue;
                    }

                    File destFile = new File(Paths.get(archiveDirectory, name + "_" + version, Constants.RESOURCES,
                            directory).toString());
                    destFile.mkdirs();
                    FileUtils.copyDirectory(registryResource, destFile);
                    OMElement infoElement = getElement(Constants.RESOURCES, Constants.EMPTY_STRING);
                    infoElement.addChild(collection);
                    org.wso2.developerstudio.eclipse.utils.file.FileUtils.createFile(
                            new File(commonPath, Constants.REG_INFO_FILE), serialize(infoElement));
                }
                dependencies.add(new ArtifactDependency(name, version, Constants.SERVER_ROLE_EI, true));
                Artifact artifactObject = new Artifact();
                artifactObject.setName(name);
                artifactObject.setType(Constants.REG_RESOURCE_TYPE);
                artifactObject.setVersion(version);
                artifactObject.setServerRole(Constants.SERVER_ROLE_EI);
                artifactObject.setFile(Constants.REG_INFO_FILE);

                String artifactDataAsString = createArtifactData(artifactObject);
                org.wso2.developerstudio.eclipse.utils.file.FileUtils.createFile(
                        new File(commonPath, Constants.ARTIFACT_XML), artifactDataAsString);

            }
        } catch (IOException | XMLStreamException | MojoExecutionException e) {
            mojoInstance.logError("Error occurred while processing registry resources");
            mojoInstance.logError(e.getMessage());
        }
    }

    /**
     * Method to process metadata in the resources folder and create corresponding files in the archive directory.
     *
     * @param resourcesFolder  path to resources folder
     * @param archiveDirectory path to archive directory
     */
    void processMetadata(File resourcesFolder, String archiveDirectory, List<ArtifactDependency> metadataDependencies,
                         String version) {
        mojoInstance.logInfo("Processing metadata in " + resourcesFolder.getAbsolutePath());
        File metadataFolder = new File(resourcesFolder, Constants.METADATA_DIR_NAME);
        if (apiList.size() > 0) {
            for (Map.Entry<String, String> entry : apiList.entrySet()) {
                String apiName = entry.getKey();
                String apiVersion = entry.getValue();
                String swaggerFilename = apiName + "_swagger.yaml";
                String metadataFilename = apiName + "_metadata.yaml";
                boolean apiVersionExists = true;
                if (apiVersion != null) {
                    swaggerFilename = apiName + "_" + apiVersion + "_swagger.yaml";
                    metadataFilename = apiName + "_" + apiVersion + "_metadata.yaml";
                } else {
                    apiVersion = version;
                    apiVersionExists = false;
                }
                File metaFile = new File(metadataFolder, metadataFilename);
                if (metaFile.exists()) {
                    String folderName = Constants.METADATA_DIR_NAME + "/" + apiName + "_metadata_" + apiVersion;
                    String fileName = apiName + "_metadata-" + apiVersion + ".yaml";
                    if (apiVersionExists) {
                        fileName = apiName + "_" + apiVersion + "_metadata-" + apiVersion + ".yaml";
                        folderName = Constants.METADATA_DIR_NAME + "/" + apiName + "_" + apiVersion + "_metadata_"
                                + apiVersion;
                        String name = apiName + "_" + apiVersion + "_metadata";
                        metadataDependencies.add(new ArtifactDependency(name, apiVersion, Constants.SERVER_ROLE_EI, true));
                        writeArtifactAndFile(metaFile, archiveDirectory, name, Constants.METADATA_TYPE,
                                Constants.SERVER_ROLE_EI, apiVersion, fileName, folderName);
                    } else {
                        metadataDependencies.add(new ArtifactDependency(apiName + "_metadata", version,
                                Constants.SERVER_ROLE_EI, true));
                        writeArtifactAndFile(metaFile, archiveDirectory, apiName + "_metadata",
                                Constants.METADATA_TYPE, Constants.SERVER_ROLE_EI, apiVersion, fileName, folderName);
                    }
                }
                if (isApiDefinitionPresent(apiName, apiVersion, apiVersionExists, metadataDependencies)) {
                    continue;
                }
                File swaggerFile = new File(metadataFolder, swaggerFilename);
                if (swaggerFile.exists()) {
                    String folderName = Constants.METADATA_DIR_NAME + "/" + apiName + "_swagger_" + apiVersion;
                    String fileName = apiName + "_swagger-" + apiVersion + ".yaml";
                    if (apiVersionExists) {
                        fileName = apiName + "_" + apiVersion + "_swagger-" + apiVersion + ".yaml";
                        folderName = Constants.METADATA_DIR_NAME + "/" + apiName + "_" + apiVersion + "_swagger_"
                                + apiVersion;
                        String name = apiName + "_" + apiVersion + "_swagger";
                        metadataDependencies.add(new ArtifactDependency(name, apiVersion, Constants.SERVER_ROLE_EI, true));
                        writeArtifactAndFile(swaggerFile, archiveDirectory, name, Constants.METADATA_TYPE,
                                Constants.SERVER_ROLE_EI, apiVersion, fileName, folderName);
                    } else {
                        metadataDependencies.add(new ArtifactDependency(apiName + "_swagger", apiVersion,
                                Constants.SERVER_ROLE_EI, true));
                        writeArtifactAndFile(swaggerFile, archiveDirectory, apiName + "_swagger",
                                Constants.METADATA_TYPE, Constants.SERVER_ROLE_EI, apiVersion, fileName, folderName);
                    }
                }
            }
        }
        if (proxyList.size() > 0) {
            for (Map.Entry<String, String> entry : proxyList.entrySet()) {
                String proxyName = entry.getKey();
                String proxyVersion = entry.getValue();
                if (proxyVersion == null) {
                    proxyVersion = version;
                }
                File metaFile = new File(metadataFolder, proxyName + "_proxy_metadata.yaml");
                if (metaFile.exists()) {
                    writeArtifactAndFile(metaFile, archiveDirectory, proxyName + "_proxy_metadata",
                            Constants.METADATA_TYPE, Constants.SERVER_ROLE_EI, proxyVersion, proxyName +
                                    "_proxy_metadata-" + proxyVersion + ".yaml", Constants.METADATA_DIR_NAME + "/" +
                                    proxyName + "_proxy_metadata_" + proxyVersion);
                    metadataDependencies.add(new ArtifactDependency(proxyName + "_proxy_metadata", proxyVersion,
                            Constants.SERVER_ROLE_EI, true));
                }
            }
        }
    }

    /**
     * Method to write artifact.xml and file to the archive directory.
     *
     * @param configFile       configuration file to write to the archive
     * @param archiveDirectory path to archive directory
     * @param name             name of the artifact
     * @param type             type of the artifact
     * @param serverRole       server role of the artifact
     * @param configVersion    version of the artifact
     * @param fileName         name of the file
     * @param folderName       name of the folder
     */
    private void writeArtifactAndFile(File configFile, String archiveDirectory, String name, String type,
                                      String serverRole, String configVersion, String fileName, String folderName) {

        writeArtifactAndFile(configFile, archiveDirectory, name, type, serverRole, configVersion, fileName,
                folderName, null);
    }

    /**
     * Method to write artifact.xml and file to the archive directory.
     *
     * @param configFile       configuration file to write to the archive
     * @param archiveDirectory path to archive directory
     * @param name             name of the artifact
     * @param type             type of the artifact
     * @param serverRole       server role of the artifact
     * @param configVersion    version of the artifact
     * @param fileName         name of the file
     * @param folderName       name of the folder
     * @param connectorName    name of the connector
     */
    private void writeArtifactAndFile(File configFile, String archiveDirectory, String name, String type,
                                      String serverRole, String configVersion, String fileName, String folderName,
                                      String connectorName) {

        Artifact artifactObject = new Artifact();
        artifactObject.setName(name);
        artifactObject.setType(type);
        artifactObject.setVersion(configVersion);
        artifactObject.setServerRole(serverRole);
        artifactObject.setFile(fileName);
        if (connectorName != null) {
            artifactObject.setConnector(connectorName);
        }
        try {
            String artifactDataAsString = createArtifactData(artifactObject);
            org.wso2.developerstudio.eclipse.utils.file.FileUtils.createFile(
                    new File(Paths.get(archiveDirectory, folderName).toString(), Constants.ARTIFACT_XML),
                    artifactDataAsString);
            org.wso2.developerstudio.eclipse.utils.file.FileUtils.copy(configFile,
                    new File(Paths.get(archiveDirectory, folderName, fileName).toString()));
        } catch (IOException | MojoExecutionException e) {
            mojoInstance.logError("Error occurred while creating " + fileName);
            mojoInstance.logError(e.getMessage());
        }
    }

    /**
     * Method to process API definitions in the resources folder and create corresponding files in the archive directory.
     *
     * @param resourcesFolder      path to resources folder
     * @param archiveDirectory     path to archive directory
     * @param metadataDependencies list of dependencies to be added to artifacts.xml file
     */
    void processAPIDefinitions(File resourcesFolder, String archiveDirectory, List<ArtifactDependency> metadataDependencies,
                               String projectVersion) {

        mojoInstance.logInfo("Processing API definitions in " + resourcesFolder.getAbsolutePath());
        File metadataFolder = new File(resourcesFolder, Constants.API_DEFINITION_DIR);
        if (!apiList.isEmpty()) {
            for (Map.Entry<String, String> entry : apiList.entrySet()) {
                String apiName = entry.getKey();
                String apiVersion = entry.getValue();
                String swaggerFilename = apiName + ".yaml";
                boolean apiVersionExists = true;
                if (apiVersion == null) {
                    apiVersion = projectVersion;
                    apiVersionExists = false;
                } else {
                    swaggerFilename = apiName + "_v" + apiVersion + ".yaml";
                }
                File swaggerFile = new File(metadataFolder, swaggerFilename);
                if (swaggerFile.exists()) {
                    String folderName;
                    String fileName;
                    if (apiVersionExists) {
                        fileName = apiName + "_" + apiVersion + "_swagger-" + apiVersion + ".yaml";
                        folderName = Constants.METADATA_DIR_NAME + "/" + apiName + "_" + apiVersion + "_swagger_"
                                + apiVersion;
                        String name = apiName + "_" + apiVersion + "_swagger";
                        metadataDependencies.add(new ArtifactDependency(name, apiVersion, Constants.SERVER_ROLE_EI, true));
                        writeArtifactAndFile(swaggerFile, archiveDirectory, name, Constants.METADATA_TYPE,
                                Constants.SERVER_ROLE_EI, apiVersion, fileName, folderName);
                    } else {
                        folderName = Constants.METADATA_DIR_NAME + "/" + apiName + "_swagger_" + apiVersion;
                        fileName = apiName + "_swagger-" + apiVersion + ".yaml";
                        String name = apiName + "_swagger";
                        metadataDependencies.add(new ArtifactDependency(name, apiVersion,
                                Constants.SERVER_ROLE_EI, true));
                        writeArtifactAndFile(swaggerFile, archiveDirectory, name,
                                Constants.METADATA_TYPE, Constants.SERVER_ROLE_EI, apiVersion, fileName, folderName);
                    }
                }
            }
        }
    }

    /**
     * Method to write metadata file to the archive directory.
     *
     * @param name             name of the artifact
     * @param apiElement       OMElement of the artifact
     * @param archiveDirectory path to archive directory
     * @param artifactType     artifact type
     */
    private void writeMetadataFile(String name, OMElement apiElement, String archiveDirectory, String artifactType,
                                   String projectVersion) {
        boolean isProxy = artifactType.equalsIgnoreCase(Constants.PROXY);
        boolean isDataService = artifactType.equalsIgnoreCase(Constants.DATA_SERVICE);
        try {
            String version = null;
            OMAttribute versionAtt = apiElement.getAttribute(new QName(Constants.VERSION));
            if (versionAtt != null) {
                version = versionAtt.getAttributeValue();
            }
            String artifactName;
            String metadataFolder;
            String metadataFileName;
            if (version != null) {
                artifactName = name + "_" + version + "_metadata";
                metadataFolder = name + "_" + version + "_metadata_" + version;
                metadataFileName = name + "_" + version + "_metadata-" + version + ".yaml";
            } else {
                if (isProxy) {
                    name = name + Constants.PROXY_WITH_UNDERSCORE;
                } else if (isDataService) {
                    name = name + Constants.DATA_SERVICE_WITH_UNDERSCORE;
                }
                artifactName = name + "_metadata";
                metadataFolder = name + "_metadata_" + projectVersion;
                metadataFileName = name + "_metadata-" + projectVersion + ".yaml";
            }
            Artifact artifactObject = new Artifact();
            artifactObject.setName(artifactName);
            artifactObject.setType(Constants.METADATA_TYPE);
            artifactObject.setVersion(getAPIVersion(apiElement, projectVersion));
            artifactObject.setServerRole(Constants.SERVER_ROLE_EI);
            artifactObject.setFile(metadataFileName);

            String artifactDataAsString = createArtifactData(artifactObject);
            String metadata = Paths.get(archiveDirectory, "metadata", metadataFolder).toString();
            org.wso2.developerstudio.eclipse.utils.file.FileUtils.createFile(
                    new File(metadata, Constants.ARTIFACT_XML),
                    artifactDataAsString);
            if (isProxy) {
                org.wso2.developerstudio.eclipse.utils.file.FileUtils.createFile(
                        new File(metadata, metadataFileName),
                        getProxyMetadataPropertiesAsString(apiElement, projectVersion));
            } else {
                if (isDataService) {
                    org.wso2.developerstudio.eclipse.utils.file.FileUtils.createFile(
                            new File(metadata, metadataFileName),
                            getAPIMetadataPropertiesAsString(apiElement, projectVersion, true));
                } else {
                    org.wso2.developerstudio.eclipse.utils.file.FileUtils.createFile(
                            new File(metadata, metadataFileName),
                            getAPIMetadataPropertiesAsString(apiElement, projectVersion, false));
                }
            }
        } catch (IOException | MojoExecutionException e) {
            mojoInstance.logError("Error occurred while creating metadata file");
            mojoInstance.logError(e.getMessage());
        }
    }

    /**
     * Method to get metadata properties as a string for an API.
     *
     * @param apiElement     OMElement of the API
     * @param projectVersion version of the project
     * @return metadata properties as a string
     */
    private String getAPIMetadataPropertiesAsString(OMElement apiElement, String projectVersion, boolean isDataService) {

        String version = getAPIVersion(apiElement, projectVersion);
        String key;
        if (isDataService) {
            key = apiElement.getAttributeValue(new QName(Constants.NAME)) +
                    Constants.PROXY_WITH_UNDERSCORE + "-" + version;
        } else {
            key = apiElement.getAttributeValue(new QName(Constants.NAME)) + "-" + version;
        }
        String name = apiElement.getAttributeValue(new QName(Constants.NAME));
        OMAttribute descriptionAtr = apiElement.getAttribute(new QName(Constants.DESCRIPTION));

        StringBuilder builder = new StringBuilder();
        // Creating the YAML file
        builder.append("---\n");
        builder.append("key: \"").append(key).append("\"\n");
        builder.append("name: \"").append(name).append("\"\n");
        builder.append("displayName: \"").append(name).append("\"\n");
        if (descriptionAtr != null) {
            builder.append("description: \"").append(descriptionAtr.getAttributeValue()).append("\"\n");
        }
        builder.append("version: \"").append(version).append("\"\n");
        if (isDataService) {
            builder.append("serviceUrl: \"").append(getProxyServiceURL(apiElement.
                    getAttributeValue(new QName(Constants.NAME)))).append("\"\n");
        } else {
            builder.append("serviceUrl: \"").append(getAPIURL(apiElement)).append("\"\n");
        }
        builder.append("definitionType: \"OAS3\"\n");
        builder.append("securityType: \"BASIC\"\n");
        builder.append("mutualSSLEnabled: false\n");
        return builder.toString();
    }

    /**
     * Method to get metadata properties as a string for a proxy service.
     *
     * @param proxyElement   OMElement of the proxy service
     * @param projectVersion version of the project
     * @return metadata properties as a string
     */
    private String getProxyMetadataPropertiesAsString(OMElement proxyElement, String projectVersion) {

        String key = proxyElement.getAttributeValue(new QName(Constants.NAME)) + "_proxy-" + projectVersion;
        String name = proxyElement.getAttributeValue(new QName(Constants.NAME));
        OMAttribute descriptionAtr = proxyElement.getAttribute(new QName(Constants.DESCRIPTION));

        StringBuilder builder = new StringBuilder();
        // Creating the YAML file
        builder.append("---\n");
        builder.append("key: \"").append(key).append("\"\n");
        builder.append("name: \"").append(name).append("\"\n");
        builder.append("displayName: \"").append(name).append("\"\n");
        if (descriptionAtr != null) {
            builder.append("description: \"").append(descriptionAtr.getAttributeValue()).append("\"\n");
        }
        builder.append("version: \"").append(projectVersion).append("\"\n");
        builder.append("serviceUrl: \"").append(getProxyServiceURL(name)).append("\"\n");
        builder.append("definitionType: \"WSDL1\"\n");
        builder.append("securityType: \"BASIC\"\n");
        builder.append("mutualSSLEnabled: false\n");
        return builder.toString();
    }

    /**
     * Method to add metadata dependencies to the list.
     *
     * @param metadataDependencies list of metadata dependencies
     * @param configElement        OMElement of the artifact
     * @param artifactType         artifact type
     * @param projectVersion       version of the project
     */
    private void addMetadataDependencies(List<ArtifactDependency> metadataDependencies, OMElement configElement,
                                         String artifactType, String projectVersion) {

        String name = configElement.getAttributeValue(new QName(Constants.NAME));
        OMAttribute versionAtt = configElement.getAttribute(new QName(Constants.VERSION));
        String dependencyName;
        String version;
        if (versionAtt != null) {
            version = versionAtt.getAttributeValue();
            dependencyName = name + "_" + version + "_metadata";
        } else {
            version = projectVersion;
            if (artifactType.equalsIgnoreCase(Constants.PROXY)) {
                name = name + Constants.PROXY_WITH_UNDERSCORE;
            } else if (artifactType.equalsIgnoreCase(Constants.DATA_SERVICE)) {
                name = name + Constants.DATA_SERVICE_WITH_UNDERSCORE ;
            }
            dependencyName = name + "_metadata";
        }
        metadataDependencies.add(new ArtifactDependency(dependencyName, version,
                Constants.SERVER_ROLE_EI, true));
    }

    /**
     * Method to get API version from the API element.
     *
     * @param apiElement     OMElement of the API
     * @param projectVersion version of the project
     * @return API version if available, project version otherwise
     */
    private String getAPIVersion(OMElement apiElement, String projectVersion) {

        OMAttribute versionAtt = apiElement.getAttribute(new QName(Constants.VERSION));
        if (versionAtt != null) {
            return versionAtt.getAttributeValue();
        }
        return projectVersion;
    }

    /**
     * Method to get the service URL of a Proxy Service.
     *
     * @param name Name of the Proxy Service
     * @return service URL
     */
    private String getProxyServiceURL(String name) {

        return "https://{MI_HOST}:{MI_PORT}" + "/services/" + name;
    }

    /**
     * Method to get the API URL from the API element.
     *
     * @param apiElement OMElement of the API
     * @return API URL
     */
    private String getAPIURL(OMElement apiElement) {

        String contextAtt = apiElement.getAttribute(new QName("context")).getAttributeValue();
        OMAttribute versionTypeAtt = apiElement.getAttribute(new QName("version-type"));
        OMAttribute versionAtt = apiElement.getAttribute(new QName(Constants.VERSION));
        if (versionTypeAtt != null) {
            String versionType = versionTypeAtt.getAttributeValue();
            if ("url".equals(versionType)) {
                return "https://{MI_HOST}:{MI_PORT}" + contextAtt + "/" + versionAtt.getAttributeValue();
            }
        }
        return "https://{MI_HOST}:{MI_PORT}" + contextAtt;
    }

    /**
     * Create artifact data from a given Artifact object.
     *
     * @param artifact: Artifact object
     * @return serialized <artifact>content</artifact> element
     */
    private String createArtifactData(Artifact artifact) throws MojoExecutionException {
        OMElement artifactElement = getElement(Constants.ARTIFACT, Constants.EMPTY_STRING);
        artifactElement = addAttribute(artifactElement, Constants.NAME, artifact.getName());
        artifactElement = addAttribute(artifactElement, Constants.VERSION, artifact.getVersion());
        artifactElement = addAttribute(artifactElement, Constants.TYPE, artifact.getType());
        artifactElement = addAttribute(artifactElement, Constants.SERVER_ROLE, artifact.getServerRole());
        if (artifact.getConnector() != null) {
            artifactElement = addAttribute(artifactElement, Constants.CONNECTOR, artifact.getConnector());
        }
        OMElement fileChildElement = getElement(Constants.FILE, artifact.getFile());
        artifactElement.addChild(fileChildElement);

        return serialize(artifactElement);
    }

    /**
     * Creates the `descriptor.xml` file in the specified archive directory.
     * This file contains the dependencies of the Composite Application (CApp).
     *
     * @param archiveDirectory The path to the archive directory where the `descriptor.xml` file will be created.
     * @param project The Maven project for which the dependency descriptor is generated.
     */
    public void createDependencyDescriptorFile(String archiveDirectory, MavenProject project) {

        String projectIdentifier = generateProjectIdentifier(project.getGroupId(), project.getArtifactId(), project.getVersion());
        String deploymentType = project.getProperties().getProperty(Constants.DEPLOYMENT_TYPE, "");
        OMElement projectElement;
        boolean fatCarEnabled = CAppDependencyResolver.isFatCarEnabled(project);
        List<CAppDependency> cAppDependencies = getTopLevelCAppDependencies(project);
        projectElement = createDependencyDescriptorXml(projectIdentifier, deploymentType, cAppDependencies, fatCarEnabled);
        try {
            String descriptorXmlFileDataAsString = serialize(projectElement);
            org.wso2.developerstudio.eclipse.utils.file.FileUtils.createFile(
                    new File(archiveDirectory, Constants.DESCRIPTOR_XML), descriptorXmlFileDataAsString);
        } catch (MojoExecutionException | IOException e) {
            mojoInstance.logError("Error occurred while creating descriptor.xml file");
            mojoInstance.logError(e.getMessage());
        }
    }

    /**
     * Generates a project identifier by concatenating the groupId, artifactId, and version with underscores.
     *
     * @param groupId    the group ID of the project
     * @param artifactId the artifact ID of the project
     * @param version    the version of the project
     * @return the constructed project name in the format groupId_artifactId_version
     */
    private String generateProjectIdentifier(String groupId, String artifactId, String version) {

        return groupId + Constants.DOUBLE_UNDERSCORE + artifactId + Constants.DOUBLE_UNDERSCORE + version;
    }

    /**
     * Creates the dependency descriptor XML element for the given project and its CApp dependencies.
     *
     * @param projectName      the name of the project
     * @param deploymentType   the deployment type of the project
     * @param cAppDependencies the list of CApp dependencies
     * @return OMElement representing the dependency descriptor XML
     */
    OMElement createDependencyDescriptorXml(String projectName, String deploymentType,
                                            List<CAppDependency> cAppDependencies, boolean fatCarEnabled) {

        OMElement projectElement = getElement(Constants.PROJECT, Constants.EMPTY_STRING);
        OMElement idElement = getElement(Constants.ID, projectName);
        projectElement.addChild(idElement);
        OMElement deploymentTypeElement = getElement(Constants.DEPLOYMENT_TYPE, deploymentType);
        projectElement.addChild(deploymentTypeElement);

        OMElement fatCarEnabledElement = getElement(Constants.FAT_CAR_ENABLED, fatCarEnabled ? "true" : "false");
        projectElement.addChild(fatCarEnabledElement);

        OMElement dependenciesElement = getElement(Constants.DEPENDENCIES, Constants.EMPTY_STRING);
        for (CAppDependency cAppDependency : cAppDependencies) {
            OMElement dependencyElement = getElement(Constants.DEPENDENCY, Constants.EMPTY_STRING);
            dependencyElement = addAttribute(dependencyElement, Constants.GROUP_ID, cAppDependency.getGroupId());
            dependencyElement = addAttribute(dependencyElement, Constants.ARTIFACT_ID, cAppDependency.getArtifactId());
            dependencyElement = addAttribute(dependencyElement, Constants.VERSION, cAppDependency.getVersion());
            dependencyElement = addAttribute(dependencyElement, Constants.TYPE, Constants.CAR_TYPE);
            dependenciesElement.addChild(dependencyElement);
        }
        projectElement.addChild(dependenciesElement);
        return projectElement;
    }

    /**
     * Create artifacts.xml file including metadata of each artifact in WSO2-ESB project.
     *
     * @param archiveDirectory: path to archive directory
     * @param dependencies      to be added to artifacts.xml file
     * @param project:          wso2 esb project
     */
    void createDependencyArtifactsXmlFile(String archiveDirectory, List<ArtifactDependency> dependencies,
                                          List<ArtifactDependency> metaDependencies, MavenProject project) {
        /*
         * Create artifacts.xml file content.
         * Create artifact element.
         * Create corresponding dependency elements.
         * */
        OMElement artifactsElement = getElement(Constants.ARTIFACTS, Constants.EMPTY_STRING);
        OMElement artifactElement = getElement(Constants.ARTIFACT, Constants.EMPTY_STRING);

        artifactElement = addAttribute(artifactElement, Constants.NAME, project.getArtifactId());
        artifactElement = addAttribute(artifactElement, Constants.VERSION, project.getVersion());
        artifactElement = addAttribute(artifactElement, Constants.TYPE, Constants.CAPP_TYPE);
        if (project.getProperties().containsKey(Constants.MAIN_SEQUENCE)) {
            artifactElement = addAttribute(artifactElement, Constants.MAIN_SEQUENCE,
                    project.getProperties().getProperty(Constants.MAIN_SEQUENCE));
        }

        for (ArtifactDependency dependency : dependencies) {
            OMElement dependencyElement = getElement(Constants.DEPENDENCY, Constants.EMPTY_STRING);
            dependencyElement = addAttribute(dependencyElement, Constants.ARTIFACT, dependency.getArtifact());
            dependencyElement = addAttribute(dependencyElement, Constants.VERSION, dependency.getVersion());
            dependencyElement = addAttribute(dependencyElement, Constants.INCLUDE, dependency.getInclude().toString());
            if (dependency.getServerRole() != null) {
                dependencyElement = addAttribute(dependencyElement, Constants.SERVER_ROLE, dependency.getServerRole());
            }
            artifactElement.addChild(dependencyElement);
        }
        artifactsElement.addChild(artifactElement);

        try {
            // Create artifacts.xml file in archive file.
            String artifactsXmlFileDataAsString = serialize(artifactsElement);
            org.wso2.developerstudio.eclipse.utils.file.FileUtils.createFile(new File(archiveDirectory, "artifacts.xml"),
                    artifactsXmlFileDataAsString);
        } catch (MojoExecutionException | IOException e) {
            mojoInstance.logError("Error occurred while creating artifacts.xml file");
            mojoInstance.logError(e.getMessage());
        }

        for (ArtifactDependency dependency : metaDependencies) {
            OMElement dependencyElement = getElement(Constants.DEPENDENCY, Constants.EMPTY_STRING);
            dependencyElement = addAttribute(dependencyElement, Constants.ARTIFACT, dependency.getArtifact());
            dependencyElement = addAttribute(dependencyElement, Constants.VERSION, dependency.getVersion());
            dependencyElement = addAttribute(dependencyElement, Constants.INCLUDE, dependency.getInclude().toString());
            if (dependency.getServerRole() != null) {
                dependencyElement = addAttribute(dependencyElement, Constants.SERVER_ROLE, dependency.getServerRole());
            }
            artifactElement.addChild(dependencyElement);
        }
        artifactsElement.addChild(artifactElement);

        try {
            // Create metadata.xml file in archive file.
            String artifactsXmlFileDataAsString = serialize(artifactsElement);
            org.wso2.developerstudio.eclipse.utils.file.FileUtils.createFile(new File(archiveDirectory, "metadata.xml"),
                    artifactsXmlFileDataAsString);
        } catch (MojoExecutionException | IOException e) {
            mojoInstance.logError("Error occurred while creating metadata.xml file");
            mojoInstance.logError(e.getMessage());
        }
    }

    /**
     * Method to process class mediators in the project and create corresponding files in the archive directory.
     *
     * @param dependencies list of dependencies to be added to artifacts.xml file
     * @param project      VSCode maven project
     */
    void processClassMediators(List<ArtifactDependency> dependencies, MavenProject project) {
        String jarName = project.getArtifactId() + "-" + project.getVersion() + ".jar";
        File jarFile = new File(Paths.get(project.getBasedir().toString(), "target", jarName).toString());
        if (jarFile.exists()) {
            dependencies.add(new ArtifactDependency(project.getArtifactId() + Constants.CLASS_MEDIATORS
                    , project.getVersion(), Constants.SERVER_ROLE_EI, true));
            writeArtifactAndFile(jarFile, project.getBasedir().toString() + File.separator +
                            Constants.TEMP_TARGET_DIR_NAME, project.getArtifactId() + Constants.CLASS_MEDIATORS,
                    Constants.CLASS_MEDIATOR_TYPE, Constants.SERVER_ROLE_EI, project.getVersion(), jarName,
                    project.getArtifactId() + Constants.CLASS_MEDIATORS + "_" + project.getVersion());
            // delete the jar file after copying to the CAPP
            jarFile.delete();
        }
    }

    /**
     * Method to process lib dependencies which are inside deployment/lib/ folder in the project and add to dependencies
     *
     * @param dependencies list of dependencies to be added to artifacts.xml file
     * @param project      VSCode maven project
     */
    void processConnectorLibDependencies(List<ArtifactDependency> dependencies, MavenProject project) {

        // process connector dependencies
        File connectorDepFolder = new File(Paths.get(project.getBasedir().toString(),
                Constants.DEFAULT_TARGET_FOLDER, Constants.DEPENDENCY).toString());
        if (connectorDepFolder.exists()) {
            File[] dependencyFiles = connectorDepFolder.listFiles();
            if (dependencyFiles != null) {
                for (File dependencyFile : dependencyFiles) {
                    if (dependencyFile.isFile() && dependencyFile.getName().endsWith(Constants.ZIP_EXTENSION)) {
                        if (dependencyFile.getName().contains(Constants.INBOUND_CONNECTORS_PREFIX) &&
                                !MavenUtils.isConnectorPackingSupported(project)) {
                            continue;
                        }
                        String fileName = dependencyFile.getName();
                        int lastIndex = fileName.lastIndexOf('-');
                        String name = fileName.substring(0, lastIndex);
                        // remove .zip at the end
                        String version = fileName.substring(lastIndex + 1,
                                fileName.length() - Constants.ZIP_EXTENSION.length());
                        dependencies.add(new ArtifactDependency(name, version, Constants.SERVER_ROLE_EI, true));
                        writeArtifactAndFile(dependencyFile, project.getBasedir().toString() + File.separator +
                                Constants.TEMP_TARGET_DIR_NAME, name, Constants.CONNECTOR_TYPE,
                                Constants.SERVER_ROLE_EI, version, fileName, name + "_" + version);
                    }
                }
            }
        }

        if (!MavenUtils.isConnectorPackingSupported(project)) {
            mojoInstance.getLog().info("The connector dependencies are not bundled as the runtime version " +
                    "is less than " + Constants.RUNTIME_VERSION_440);
            return;
        }

        // Process library dependencies of connectors
        File libFolder = new File(project.getBasedir(), Constants.DEFAULT_TARGET_FOLDER + File.separator + Constants.LIBS);

        if (!libFolder.exists()) {
            return; // Exit if the lib folder does not exist
        }

        File[] connectorDeps = libFolder.listFiles();
        if (connectorDeps == null) {
            return; // Exit if no files/folders exist inside lib folder
        }

        for (File connectorDir : connectorDeps) {
            if (!connectorDir.isDirectory()) {
                continue; // Skip if not a directory
            }

            String connectorName = connectorDir.getName();
            File[] libFiles = connectorDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".jar");
                }
            }); // Filter only JAR files

            if (libFiles == null) {
                continue;
            }

            for (File libFile : libFiles) {
                String fileNameWithoutExt = libFile.getName().substring(0, libFile.getName().length() - 4);
                
                // Create a unique name for the dependency by appending connector name as a prefix
                String uniqueDependencyName = connectorName + "_" + fileNameWithoutExt;
            
                // Add dependency to the list with the unique name
                dependencies.add(new ArtifactDependency(
                        uniqueDependencyName, project.getVersion(), Constants.SERVER_ROLE_EI, true));
            
                // Write artifact and file details using the unique dependency name
                writeArtifactAndFile(libFile, project.getBasedir() + File.separator + Constants.TEMP_TARGET_DIR_NAME,
                        uniqueDependencyName, Constants.CONNECTOR_DEPENDENCY_TYPE, Constants.SERVER_ROLE_EI,
                        project.getVersion(), libFile.getName(), connectorName + "_" +
                        fileNameWithoutExt, connectorName);
            }
        }
    }

    @Override
    protected void deserialize(OMElement documentElement) throws Exception {

    }

    @Override
    protected String serialize() throws Exception {
        return null;
    }

    private String serialize(OMElement element) throws MojoExecutionException {
        OMDocument document = factory.createOMDocument();
        document.addChild(element);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            prettify(element, outputStream);
        } catch (Exception e) {
            throw new MojoExecutionException("Error serializing", e);
        }
        return outputStream.toString();
    }

    private boolean isApiDefinitionPresent(String apiName, String apiVersion, boolean apiVersionExists,
                                           List<ArtifactDependency> metadataDependencies) {
        apiName = apiVersionExists ? (apiName + "_" + apiVersion + "_swagger") : (apiName + "_swagger");
        for (ArtifactDependency dependency : metadataDependencies) {
            if (dependency.getArtifact().equals(apiName) && dependency.getVersion().equals(apiVersion)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected String getDefaultName() {
        return null;
    }
}
