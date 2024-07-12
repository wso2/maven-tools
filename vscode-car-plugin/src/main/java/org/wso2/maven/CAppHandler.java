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
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.DeferredParsingException;
import org.apache.axiom.om.OMDocument;
import org.apache.axiom.om.OMElement;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.wso2.maven.model.Artifact;
import org.wso2.maven.model.ArtifactDependency;
import org.wso2.maven.model.ArtifactDetails;
import org.wso2.maven.core.model.AbstractXMLDoc;

class CAppHandler extends AbstractXMLDoc {
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
                          String version) {
        if (!artifactsFolder.exists()) {
            mojoInstance.logInfo("Could not find artifacts folder in " + artifactsFolder.getAbsolutePath());
            return;
        }
        mojoInstance.logInfo("Processing artifacts in " + artifactsFolder.getAbsolutePath());
        for (ArtifactDetails artifactDetails : artifactTypeList) {
            File artifactFolder = new File(artifactsFolder, artifactDetails.getDirectory());
            processArtifactsInFolder(artifactFolder, dependencies, version, archiveDirectory,
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
    void processArtifactsInFolder(File artifactsDir, List<ArtifactDependency> dependencies, String version,
                                  String archiveDirectory, String serverRole, String type) {
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
                        }
                        if (configVersion == null) {
                            apiHasVersion = false;
                            configVersion = version;
                        }
                        if (Constants.PROXY_SERVICE_TYPE.equals(type)) {
                            proxyList.put(name, configVersion);
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

    /**
     * Method to process resources folder and create corresponding files in the archive directory.
     *
     * @param resourcesFolder  path to resources folder
     * @param archiveDirectory path to archive directory
     * @param dependencies     list of dependencies to be added to artifacts.xml file
     */
    void processResourcesFolder(File resourcesFolder, String archiveDirectory, List<ArtifactDependency> dependencies,
                                List<ArtifactDependency> metadataDependencies, String version) {
        if (!resourcesFolder.exists()) {
            mojoInstance.logInfo("Could not find resources folder in " + resourcesFolder.getAbsolutePath());
            return;
        }
        processConnectors(resourcesFolder, archiveDirectory, dependencies);
        processRegistryResources(resourcesFolder, archiveDirectory, dependencies);
        processMetadata(resourcesFolder, archiveDirectory, metadataDependencies, version);
    }

    /**
     * Method to process connectors in the resources folder and create corresponding files in the archive directory.
     *
     * @param resourcesFolder  path to resources folder
     * @param archiveDirectory path to archive directory
     * @param dependencies     list of dependencies to be added to artifacts.xml file
     */
    void processConnectors(File resourcesFolder, String archiveDirectory, List<ArtifactDependency> dependencies) {
        mojoInstance.logInfo("Processing connectors in " + resourcesFolder.getAbsolutePath());
        File connectorFolder = new File(resourcesFolder, Constants.CONNECTORS_DIR_NAME);
        File[] connectorFiles = connectorFolder.listFiles();
        if (connectorFiles == null) {
            return;
        }
        for (File connector : connectorFiles) {
            if (connector.isFile()) {
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

    /**
     * Method to process registry resources in the resources folder and create corresponding files in the archive directory.
     *
     * @param resourcesFolder  path to resources folder
     * @param archiveDirectory path to archive directory
     * @param dependencies     list of dependencies to be added to artifacts.xml file
     */
    void processRegistryResources(File resourcesFolder, String archiveDirectory, List<ArtifactDependency> dependencies) {
        mojoInstance.logInfo("Processing registry resources in " + resourcesFolder.getAbsolutePath());
        File registryFolder = new File(resourcesFolder, Constants.REGISTRY_DIR_NAME);
        File artifactFile = new File(registryFolder, Constants.ARTIFACT_XML);
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
                    if (path.startsWith(Constants.GOV_REG_PREFIX)) {
                        path = path.substring(Constants.GOV_REG_PREFIX.length());
                        registryResource = new File(registryFolder, Constants.GOV_FOLDER + path + "/" + fileName);
                    } else {
                        path = path.substring(Constants.CONF_REG_PREFIX.length());
                        registryResource = new File(registryFolder, Constants.CONF_FOLDER + path + "/" + fileName);
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
                    if (path.startsWith(Constants.GOV_REG_PREFIX)) {
                        path = path.substring(Constants.GOV_REG_PREFIX.length());
                        registryResource = new File(registryFolder, Constants.GOV_FOLDER + path);
                    } else {
                        path = path.substring(Constants.CONF_REG_PREFIX.length());
                        registryResource = new File(registryFolder, Constants.CONF_FOLDER + path);
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
                File swaggerFile = new File(metadataFolder, swaggerFilename);
                File metaFile = new File(metadataFolder, metadataFilename);
                if (swaggerFile.exists() && metaFile.exists()) {
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

                    folderName = Constants.METADATA_DIR_NAME + "/" + apiName + "_swagger_" + apiVersion;
                    fileName = apiName + "_swagger-" + apiVersion + ".yaml";
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
        Artifact artifactObject = new Artifact();
        artifactObject.setName(name);
        artifactObject.setType(type);
        artifactObject.setVersion(configVersion);
        artifactObject.setServerRole(serverRole);
        artifactObject.setFile(fileName);
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
        OMElement fileChildElement = getElement(Constants.FILE, artifact.getFile());
        artifactElement.addChild(fileChildElement);

        return serialize(artifactElement);
    }

    /**
     * Create artifacts.xml file including meta data of each artifact in WSO2-ESB project.
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
            dependencies.add(new ArtifactDependency(project.getArtifactId(), project.getVersion(),
                    Constants.SERVER_ROLE_EI, true));
            writeArtifactAndFile(jarFile, project.getBasedir().toString() + File.separator +
                            Constants.TEMP_TARGET_DIR_NAME, project.getArtifactId(), Constants.CLASS_MEDIATOR_TYPE,
                    Constants.SERVER_ROLE_EI, project.getVersion(), jarName, project.getArtifactId() + "_" +
                            project.getVersion());
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

    @Override
    protected String getDefaultName() {
        return null;
    }
}
