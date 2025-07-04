/*
 * Copyright (c) 2024, WSO2 LLC (http://www.wso2.com).
 *
 * WSO2 LLC licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.maven.libraries;

import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.wso2.maven.CARMojo;
import org.wso2.maven.Constants;
import org.wso2.maven.MavenUtils;
import org.wso2.maven.datamapper.DataMapperException;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import static org.wso2.maven.MavenUtils.createPomFile;
import static org.wso2.maven.MavenUtils.setupInvoker;

/**
 * Resolves dependencies for connectors.
 */
public class ConnectorDependencyResolver {

    // connectionTypeMap and flag to check if connections are scanned
    private static Map<String, String> connectionTypeMap;
    private static boolean scannedConnections = false;

    /**
     * Resolves dependencies for connectors.
     *
     * @param carMojo The Mojo instance.
     * @throws Exception If an error occurs while resolving dependencies.
     */
    public static void resolveDependencies(CARMojo carMojo, MavenProject project) throws Exception {

        String extractedDir = Constants.DEFAULT_TARGET_FOLDER + File.separator + Constants.EXTRACTED_CONNECTORS;
        String libDir = Constants.DEFAULT_TARGET_FOLDER + File.separator + Constants.LIBS;

        // Ensure target directories exist
        new File(extractedDir).mkdirs();
        File libDirFile = new File(libDir);
        libDirFile.mkdirs();
        String libDirPath = libDirFile.getAbsolutePath();

        Invoker invoker = new DefaultInvoker();
        setupInvoker(invoker, project.getBasedir().getAbsolutePath());

        // Resolve connector ZIP files from pom.xml
        ArrayList<File> connectorZips = resolveConnectorZips(invoker);

        if (!MavenUtils.isConnectorPackingSupported(project)) {
            // runtime version is not 4.4.0 or higher, skip resolving dependencies
            return;
        }

        // Resolve connectors from resources folder
        List<String> directories = Arrays.asList(Constants.CONNECTORS_DIR_NAME, Constants.INBOUND_CONNECTORS_DIR_NAME);
        for (String directoryName : directories) {
            resolveConnectorZipsFromResources(connectorZips, directoryName);
        }

        Map<QName, File> dependencyFiles = new HashMap<>();
        // Extract all connector ZIP files
        for (File zipFile : connectorZips) {
            String targetExtractDir = extractedDir + File.separator + zipFile.getName().replace(".zip", "");
            if (new File(targetExtractDir).exists()) {
                carMojo.logInfo("Connector already extracted: " + zipFile.getName());
                continue;
            }
            extractZipFile(zipFile, targetExtractDir);
            QName qualifiedConnectorName = extractConnectorInfo(carMojo, targetExtractDir);
            if (qualifiedConnectorName == null) {
                carMojo.logError("Failed to extract connector information from " + zipFile.getName());
                continue;
            }
            File descriptorYaml = new File(targetExtractDir + File.separator + Constants.DESCRIPTOR_YAML);
            if (descriptorYaml.exists()) {
                carMojo.getLog().info("Found descriptor file: " + descriptorYaml.getPath());
                dependencyFiles.put(qualifiedConnectorName, descriptorYaml);
            }
        }

        if (!dependencyFiles.isEmpty()){
            for (Map.Entry<QName, File> entry : dependencyFiles.entrySet()) {
                carMojo.logInfo("Resolving dependencies for " + entry.getKey());
                resolveMavenDependencies(entry.getValue(), libDirPath, invoker, carMojo, entry.getKey().toString());
            }
        }
        carMojo.logInfo("All dependencies resolved and extracted successfully.");
    }

    /**
     * Resolves connector ZIP files from the pom.xml file.
     *
     * @param invoker The Maven Invoker.
     * @return The list of connector ZIP files.
     * @throws MavenInvocationException If an error occurs while resolving dependencies.
     */
    private static ArrayList<File> resolveConnectorZips(Invoker invoker) throws MavenInvocationException {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(new File(Constants.POM_FILE));
        request.setGoals(Collections.singletonList("dependency:copy-dependencies -DincludeTypes=zip"));
        invoker.execute(request);

        File dependenciesDir = new File(Constants.DEFAULT_TARGET_FOLDER + File.separator + Constants.DEPENDENCY);
        if (!dependenciesDir.exists()) {
            return new ArrayList<>();
        }
        ArrayList<File> connectorZips = new ArrayList<>();
        for (File file : dependenciesDir.listFiles()) {
            if (file.getName().endsWith(".zip")) {
                connectorZips.add(file);
            }
        }
        return connectorZips;
    }

    private static void resolveConnectorZipsFromResources(ArrayList<File> connectorZips, String directoryName) {

        File connectorsDir = new File(Constants.RESOURCES_FOLDER_PATH, directoryName);
        if (!connectorsDir.exists()) {
            return;
        }

        for (File file : Objects.requireNonNull(connectorsDir.listFiles())) {
            if (file.getName().endsWith(".zip")) {
                connectorZips.add(file);
            }
        }
    }

    /**
     * Extracts a ZIP file to the specified directory.
     *
     * @param zipFile The ZIP file to extract.
     * @param outputDir The directory to extract the ZIP file to.
     * @throws IOException If an error occurs while extracting the ZIP file.
     */
    private static void extractZipFile(File zipFile, String outputDir) throws IOException {
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            File newFile = newFile(outputDir, zipEntry);
            if (zipEntry.isDirectory()) {
                newFile.mkdirs();
            } else {
                new File(newFile.getParent()).mkdirs();
                try (FileOutputStream fos = new FileOutputStream(newFile)) {
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
            }
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
    }

    /**
     * Creates a new file from a ZIP entry.
     *
     * @param outputDir The output directory.
     * @param zipEntry The ZIP entry.
     * @return The new file.
     */
    private static File newFile(String outputDir, ZipEntry zipEntry) {
        String fileName = zipEntry.getName();
        return new File(outputDir + File.separator + fileName);
    }

    /**
     * Resolves Maven dependencies from a descriptor.yml file.
     *
     * @param descriptorYaml The descriptor.yml file.
     * @param libDir The directory to copy the dependencies to.
     * @param invoker The Maven Invoker.
     * @param carMojo The Mojo instance.
     * @param connectorName The connector name.
     * @throws Exception If an error occurs while resolving dependencies.
     */
    private static void resolveMavenDependencies(File descriptorYaml, String libDir, Invoker invoker, CARMojo carMojo,
                                                 String connectorName) throws Exception {

        if (!descriptorYaml.exists()) {
            return;
        }

        Yaml yaml = new Yaml();
        Map<String, Object> yamlData = yaml.load(Files.newInputStream(descriptorYaml.toPath()));

        // Extract repositories
        List<Map<String, String>> repositories = (List<Map<String, String>>) yamlData.get(Constants.REPOSITORIES);
        List<String> repositoriesList = new ArrayList<>();
        if (repositories != null) {
            for (int i = 0; i < repositories.size(); i++) {
                repositoriesList.add(repositories.get(i).get("url"));
            }
        }

        // Extract dependencies
        List<Map<String, String>> dependencies = (List<Map<String, String>>) yamlData.get(Constants.DEPENDENCIES);
        Set<String> dependencySet = new HashSet<>();
        if (dependencies != null) {
            for (Map<String, String> dependency : dependencies) {
                String groupId = dependency.get(Constants.GROUP_ID);
                String artifactId = dependency.get(Constants.ARTIFACT_ID);
                String version = dependency.get(Constants.VERSION);

                // check if connectionType is provided
                if (dependency.containsKey(Constants.CONNECTION_TYPE)) {
                    String connectionType = dependency.get(Constants.CONNECTION_TYPE);

                    // scan local entries folder for connections if not already scanned
                    if (!scannedConnections) {
                        carMojo.logInfo("Scanning local entries folder for connections.");
                        connectionTypeMap = scanLocalEntriesForConnections(Constants.LOCAL_ENTRIES_FOLDER_PATH, carMojo);
                        scannedConnections = true;
                    }

                    // skip the dependency if connectionType is not used
                    if (connectionTypeMap == null || !connectionTypeMap.containsKey(connectionType)) {
                        carMojo.logInfo("Skipping dependency: " + groupId + ":" + artifactId + ":" + version
                                + " as the connectionType: " + connectionType + " is not found in the local entries.");
                        continue;
                    }
                }

                dependencySet.add(groupId + ":" + artifactId + ":" + version);
            }
        }

        List<String> dependenciesList = new ArrayList<>(dependencySet);
        resolveAndCopyDependencies(dependenciesList, repositoriesList, libDir, invoker, carMojo, connectorName);
    }

    private static void resolveAndCopyDependencies(List<String> dependencies, List<String> repositories,
                                                  String libDir, Invoker invoker, CARMojo carMojo, String connectorName)
            throws LibraryResolverException {

        File targetDir = new File(libDir + File.separator + connectorName);
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw new LibraryResolverException("Failed to create directory: " + targetDir.getAbsolutePath());
        }
        try {
            File tempPom = createPomFile(dependencies, repositories);

            InvocationRequest request = new DefaultInvocationRequest();
            request.setPomFile(tempPom);
            request.setGoals(Collections.singletonList(String.format("dependency:copy-dependencies " +
                    "-DexcludeTransitive=true -DoutputDirectory=%s", libDir + File.separator + connectorName)));

            executeRequest(request, "Failed to resolve and copy dependencies", invoker, carMojo);
            if (!tempPom.delete()) {
                carMojo.getLog().warn("Failed to delete temporary pom.xml: " + tempPom.getAbsolutePath());
            }
        } catch (IOException e) {
            throw new LibraryResolverException("Failed to create temporary pom.xml", e);
        }
    }

    /**
     * Executes a Maven invocation request and logs any errors.
     *
     * @param request The Maven invocation request to execute.
     * @param errorMessage The error message to log if the execution fails.
     * @throws DataMapperException if the execution encounters an exception.
     */
    private static void executeRequest(InvocationRequest request, String errorMessage,
                                Invoker invoker, CARMojo carMojo) throws LibraryResolverException {

        try {
            InvocationResult result = invoker.execute(request);
            if (result.getExitCode() != 0) {
                carMojo.logError(errorMessage);
                if (result.getExecutionException() != null) {
                    carMojo.logError(result.getExecutionException().getMessage());
                }
                throw new LibraryResolverException(errorMessage);
            }
        } catch (MavenInvocationException e) {
            throw new LibraryResolverException(errorMessage, e);
        }
    }

    /**
     * Scans the local entries folder for connections and returns a map of
     * connection types.
     *
     * @param carMojo The Mojo instance.
     * @return The map of connection types.
     * @throws Exception If an error occurs while scanning the local entries folder.
     */
    private static Map<String, String> scanLocalEntriesForConnections(String folderPath, CARMojo carMojo)
            throws Exception {
        Map<String, String> connectionTypeMap = new HashMap<>();

        File localEntriesFolder = new File(folderPath);
        if (!localEntriesFolder.exists()) {
            return connectionTypeMap;
        }

        File[] localEntries = localEntriesFolder.listFiles();
        if (localEntries == null) {
            return connectionTypeMap;
        }

        for (File localEntry : localEntries) {
            if (localEntry.isFile() && localEntry.getName().endsWith(".xml")) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(localEntry);
                Element root = doc.getDocumentElement();

                String connectorName = "", connectionType = "";
                NodeList initNodes = root.getElementsByTagName("*");
                for (int i = 0; i < initNodes.getLength(); i++) {
                    Element element = (Element) initNodes.item(i);
                    String nodeName = element.getNodeName();
                    if (nodeName.endsWith(".init")) {
                        connectorName = nodeName.substring(0, nodeName.indexOf(".init"));
                        NodeList ctNodes = element.getElementsByTagName("connectionType");
                        if (ctNodes.getLength() > 0) {
                            connectionType = ctNodes.item(0).getTextContent();
                            connectionTypeMap.put(connectionType, connectorName);

                            break;
                        }
                    }
                }

                carMojo.getLog().info("Found local entry file: " + localEntry.getPath() + " with connectionType: "
                        + connectionType + " and connectorName: " + connectorName);
            }
        }

        return connectionTypeMap;
    }


    public static QName extractConnectorInfo(CARMojo carMojo, String filePath) {

        try {
            File xmlFile = new File(filePath + File.separator + Constants.CONNECTOR_XML);
            if (!xmlFile.exists()) {
                return null;
            }
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(xmlFile);
            document.getDocumentElement().normalize();

            Element componentElement = (Element) document.getElementsByTagName(Constants.COMPONENT).item(0);
            if (componentElement != null) {
                String name = componentElement.getAttribute(Constants.NAME);
                String packageName = componentElement.getAttribute(Constants.PACKAGE);
                QName qName = new QName(packageName, name);
                return qName;
            } else {
                return null;
            }
        } catch (Exception e) {
            carMojo.logError("Error occurred while extracting connector information: " + e.getMessage());
            return null;
        }
    }
}
