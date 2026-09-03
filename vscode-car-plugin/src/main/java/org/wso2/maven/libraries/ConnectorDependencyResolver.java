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

import org.apache.commons.lang.StringUtils;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    /** Regex to extract the Maven artifactId from a versioned ZIP filename.
     *  Matches release versions (e.g. "mi-connector-file-4.0.36") and qualified versions
     *  (e.g. "mi-connector-smpp-2.0.0-SNAPSHOT", "mi-connector-file-4.0.36-beta1"). */
    private static final Pattern ARTIFACT_ID_PATTERN =
            Pattern.compile("^(.+)-(\\d+(?:\\.\\d+)+(?:-[A-Za-z0-9]+)*)$");

    /**
     * Resolves dependencies for connectors.
     *
     * @param carMojo The Mojo instance.
     * @throws Exception If an error occurs while resolving dependencies.
     */
    public static void resolveDependencies(CARMojo carMojo, MavenProject project) throws Exception {

        String extractedDir = new File(project.getBasedir(),
                Constants.DEFAULT_TARGET_FOLDER + File.separator + Constants.EXTRACTED_CONNECTORS).getAbsolutePath();
        String libDir = new File(project.getBasedir(),
                Constants.DEFAULT_TARGET_FOLDER + File.separator + Constants.LIBS).getAbsolutePath();

        // Ensure target directories exist
        new File(extractedDir).mkdirs();
        File libDirFile = new File(libDir);
        libDirFile.mkdirs();
        String libDirPath = libDirFile.getAbsolutePath();

        Invoker invoker = new DefaultInvoker();
        setupInvoker(invoker, project.getBasedir().getAbsolutePath());

        // Resolve connector ZIP files from pom.xml
        ArrayList<File> connectorZips = resolveConnectorZips(invoker, project.getBasedir());

        if (!MavenUtils.isConnectorPackingSupported(project)) {
            // runtime version is not 4.4.0 or higher, skip resolving dependencies
            return;
        }

        // Resolve connectors from resources folder
        List<String> directories = Arrays.asList(Constants.CONNECTORS_DIR_NAME, Constants.INBOUND_ENDPOINTS_DIR_NAME,
                Constants.INBOUND_CONNECTORS_DIR_NAME);
        for (String directoryName : directories) {
            resolveConnectorZipsFromResources(connectorZips, directoryName, project.getBasedir());
        }

        // Load connector-config.json once for this build
        ConnectorConfig connectorConfig = ConnectorConfigReader.read(project.getBasedir().getAbsolutePath());
        if (connectorConfig != null && connectorConfig.getConnectors() != null) {
            carMojo.logInfo("connector-config.json loaded. Overrides defined for: "
                    + connectorConfig.getConnectors().keySet());
        }

        // Root-level: skip all connector dependency resolution
        if (connectorConfig != null && connectorConfig.isOmitAllDrivers()) {
            carMojo.logInfo("connector-config.json: omitAllDrivers=true — skipping all connector driver resolution.");
            return;
        }
        if (connectorConfig != null && connectorConfig.isOmitAllConnectors()) {
            carMojo.logInfo("connector-config.json: omitAllConnectors=true — skipping all connector driver resolution.");
            return;
        }

        // Extract all connector ZIP files; track QName → (descriptorFile, artifactId)
        Map<QName, File> dependencyFiles = new HashMap<>();
        Map<QName, String> connectorArtifactIds = new HashMap<>();

        for (File zipFile : connectorZips) {
            String targetExtractDir = extractedDir + File.separator + zipFile.getName().replace(".zip", "");
            if (new File(targetExtractDir).exists()) {
                carMojo.logInfo("Connector already extracted: " + zipFile.getName());
                // Still track the artifactId so overrides work on cached extractions
                QName cachedQName = extractConnectorInfo(carMojo, targetExtractDir);
                if (cachedQName != null) {
                    connectorArtifactIds.put(cachedQName, extractArtifactIdFromZipName(zipFile.getName()));
                    File descriptorYaml = new File(targetExtractDir + File.separator + Constants.DESCRIPTOR_YAML);
                    if (descriptorYaml.exists()) {
                        dependencyFiles.put(cachedQName, descriptorYaml);
                    }
                }
                continue;
            }
            extractZipFile(zipFile, targetExtractDir);
            QName qualifiedConnectorName = extractConnectorInfo(carMojo, targetExtractDir);
            if (qualifiedConnectorName == null) {
                carMojo.logError("Failed to extract connector information from " + zipFile.getName());
                continue;
            }
            String artifactId = extractArtifactIdFromZipName(zipFile.getName());
            connectorArtifactIds.put(qualifiedConnectorName, artifactId);

            File descriptorYaml = new File(targetExtractDir + File.separator + Constants.DESCRIPTOR_YAML);
            if (descriptorYaml.exists()) {
                carMojo.getLog().info("Found descriptor file: " + descriptorYaml.getPath());
                dependencyFiles.put(qualifiedConnectorName, descriptorYaml);
            }
        }

        if (!dependencyFiles.isEmpty()) {
            // Scoped to this resolveDependencies() invocation only, so one module's local-entries
            // scan is never reused by another module in the same multi-module reactor build.
            AtomicReference<Set<String>> activeConnectionTypesRef = new AtomicReference<>();
            for (Map.Entry<QName, File> entry : dependencyFiles.entrySet()) {
                String connectorArtifactId = connectorArtifactIds.getOrDefault(
                        entry.getKey(), entry.getKey().getLocalPart());

                // Skip driver resolution for connectors that are themselves omitted from the CAR
                if (connectorConfig != null && connectorConfig.getConnectors() != null) {
                    ConnectorDependencyConfig connectorCfg =
                            connectorConfig.getConnectors().get(connectorArtifactId);
                    if (connectorCfg != null && connectorCfg.isOmit()) {
                        carMojo.logInfo("connector-config.json: omit=true for connector "
                                + connectorArtifactId + " — skipping driver resolution.");
                        continue;
                    }
                }

                carMojo.logInfo("Resolving dependencies for " + entry.getKey());
                resolveMavenDependencies(entry.getValue(), libDirPath, invoker, carMojo,
                        entry.getKey().toString(), connectorArtifactId, project.getBasedir(), connectorConfig,
                        activeConnectionTypesRef);
            }
        }
        carMojo.logInfo("All dependencies resolved and extracted successfully.");
    }

    /**
     * Resolves connector ZIP files from the pom.xml file.
     *
     * @param invoker    The Maven Invoker.
     * @param projectDir The project base directory.
     * @return The list of connector ZIP files.
     * @throws MavenInvocationException If an error occurs while resolving dependencies.
     */
    private static ArrayList<File> resolveConnectorZips(Invoker invoker, File projectDir)
            throws MavenInvocationException {

        InvocationRequest request = new DefaultInvocationRequest();
        request.setBaseDirectory(projectDir);
        request.setPomFile(new File(projectDir, Constants.POM_FILE));
        request.setGoals(Collections.singletonList("dependency:copy-dependencies -DincludeTypes=zip"));
        invoker.execute(request);

        File dependenciesDir = new File(projectDir,
                Constants.DEFAULT_TARGET_FOLDER + File.separator + Constants.DEPENDENCY);
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

    private static void resolveConnectorZipsFromResources(ArrayList<File> connectorZips, String directoryName,
                                                           File projectDir) {

        File connectorsDir = new File(new File(projectDir, Constants.RESOURCES_FOLDER_PATH), directoryName);
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
     * @param zipFile   The ZIP file to extract.
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
     * @param zipEntry  The ZIP entry.
     * @return The new file.
     */
    private static File newFile(String outputDir, ZipEntry zipEntry) {

        String fileName = zipEntry.getName();
        return new File(outputDir + File.separator + fileName);
    }

    /**
     * Resolves Maven dependencies from a descriptor.yml file, applying any overrides from
     * {@code connector-config.json}.
     *
     * @param descriptorYaml      The descriptor.yml file.
     * @param libDir              The directory to copy the dependencies to.
     * @param invoker             The Maven Invoker.
     * @param carMojo             The Mojo instance.
     * @param connectorQName      The connector QName string (used as the lib subdirectory name).
     * @param connectorArtifactId The connector Maven artifactId (used to look up overrides).
     * @param projectDir          The project base directory.
     * @param overrideConfig      Parsed connector-config.json (may be null).
     * @param activeConnectionTypesRef Lazily-populated, per-module cache of active connectionTypes
     *                                 found in the local entries folder (shared across connectors
     *                                 within one resolveDependencies() call only).
     * @throws Exception If an error occurs while resolving dependencies.
     */
    private static void resolveMavenDependencies(File descriptorYaml, String libDir, Invoker invoker, CARMojo carMojo,
                                                 String connectorQName, String connectorArtifactId,
                                                 File projectDir, ConnectorConfig overrideConfig,
                                                 AtomicReference<Set<String>> activeConnectionTypesRef)
            throws Exception {

        if (!descriptorYaml.exists()) {
            return;
        }

        // Per-connector: check if this connector's drivers should be omitted
        if (overrideConfig != null && overrideConfig.getConnectors() != null) {
            ConnectorDependencyConfig connectorCfg = overrideConfig.getConnectors().get(connectorArtifactId);
            if (connectorCfg != null && connectorCfg.isOmitAllDrivers()) {
                carMojo.logInfo("connector-config.json: omitAllDrivers=true for connector "
                        + connectorArtifactId + " — skipping driver resolution.");
                return;
            }
        }

        Yaml yaml = new Yaml();
        Map<String, Object> yamlData;
        try (InputStream is = Files.newInputStream(descriptorYaml.toPath())) {
            yamlData = yaml.load(is);
        } catch (IOException e) {
            carMojo.logError("Failed to read descriptor.yml for connector " + connectorArtifactId + ": " + e.getMessage());
            return;
        }

        // Extract repositories
        List<Map<String, String>> repositories = (List<Map<String, String>>) yamlData.get(Constants.REPOSITORIES);
        List<String> repositoriesList = new ArrayList<>();
        if (repositories != null) {
            for (int i = 0; i < repositories.size(); i++) {
                repositoriesList.add(repositories.get(i).get("url"));
            }
        }

        // Extract dependencies, applying overrides from connector-config.json
        List<Map<String, String>> dependencies = (List<Map<String, String>>) yamlData.get(Constants.DEPENDENCIES);
        Set<String> dependencySet = new HashSet<>();
        if (dependencies != null) {
            for (Map<String, String> dependency : dependencies) {
                String groupId = dependency.get(Constants.GROUP_ID);
                String artifactId = dependency.get(Constants.ARTIFACT_ID);
                String version = dependency.get(Constants.VERSION);
                String connectionType = dependency.get(Constants.CONNECTION_TYPE);

                // Check connector-config.json for an override for this dependency
                DependencyOverride override = ConnectorConfigReader.findOverride(
                        overrideConfig, connectorArtifactId, connectionType, groupId, artifactId);

                if (override != null) {
                    if (override.isOmit()) {
                        carMojo.logInfo("Omitting dependency per connector-config.json: "
                                + groupId + ":" + artifactId + ":" + version
                                + " (connector: " + connectorArtifactId + ")");
                        continue;
                    }
                    // Local JAR override — copy directly, skip Maven resolution entirely
                    if (!StringUtils.isBlank(override.getLocalPath())) {
                        copyLocalJar(override, libDir, connectorQName, connectorArtifactId, carMojo);
                        continue;
                    }
                    // Apply coordinate overrides; bypass the connectionType gating below since
                    // the user has explicitly declared this dependency in connector-config.json.
                    if (!StringUtils.isBlank(override.getGroupId())) {
                        groupId = override.getGroupId();
                    }
                    if (!StringUtils.isBlank(override.getArtifactId())) {
                        artifactId = override.getArtifactId();
                    }
                    if (!StringUtils.isBlank(override.getVersion())) {
                        version = override.getVersion();
                    }
                    carMojo.logInfo("Applying connector-config.json override for connector "
                            + connectorArtifactId + ": " + groupId + ":" + artifactId + ":" + version);
                } else if (connectionType != null) {
                    // No explicit override — filter by whether the connectionType is active in local entries.
                    if (activeConnectionTypesRef.get() == null) {
                        carMojo.logInfo("Scanning local entries folder for connections.");
                        activeConnectionTypesRef.set(scanLocalEntriesForConnections(
                                new File(projectDir, Constants.LOCAL_ENTRIES_FOLDER_PATH).getAbsolutePath(), carMojo));
                    }
                    Set<String> activeConnectionTypes = activeConnectionTypesRef.get();

                    if (activeConnectionTypes == null || !activeConnectionTypes.contains(connectionType)) {
                        carMojo.logInfo("Skipping dependency: " + groupId + ":" + artifactId + ":" + version
                                + " as the connectionType: " + connectionType
                                + " is not found in the local entries.");
                        continue;
                    }
                }

                dependencySet.add(groupId + ":" + artifactId + ":" + version);
            }
        }

        // Handle additional dependencies configured for a connector
        List<DependencyOverride> allOverrides =
                ConnectorConfigReader.getOverrides(overrideConfig, connectorArtifactId);
        if (allOverrides != null) {
            for (DependencyOverride addition : allOverrides) {
                if (!addition.isAdditionalDependency()) {
                    continue;
                }
                if (!StringUtils.isBlank(addition.getLocalPath())) {
                    copyLocalJar(addition, libDir, connectorQName, connectorArtifactId, carMojo);
                    continue;
                }
                if (StringUtils.isBlank(addition.getGroupId()) || StringUtils.isBlank(addition.getArtifactId())
                        || StringUtils.isBlank(addition.getVersion())) {
                    throw new LibraryResolverException(
                            "Additional dependency for connector " + connectorArtifactId
                            + " must specify groupId, artifactId and version (or a localPath). Got "
                            + addition.getGroupId() + ":" + addition.getArtifactId() + ":"
                            + addition.getVersion());
                }
                carMojo.logInfo("Adding dependency from connector-config.json for connector "
                        + connectorArtifactId + ": " + addition.getGroupId() + ":"
                        + addition.getArtifactId() + ":" + addition.getVersion());
                dependencySet.add(addition.getGroupId() + ":" + addition.getArtifactId() + ":"
                        + addition.getVersion());
            }
        }

        List<String> dependenciesList = new ArrayList<>(dependencySet);
        resolveAndCopyDependencies(dependenciesList, repositoriesList, libDir, invoker, carMojo, connectorQName,
                connectorArtifactId);
    }

    /**
     * Copies a local JAR declared via localPath in connector-config.json directly into the
     * connector's lib directory, bypassing Maven resolution.
     *
     * @param override            the override/addition entry carrying the localPath
     * @param libDir              the lib directory root
     * @param connectorQName      the connector QName string
     * @param connectorArtifactId the connector Maven artifactId
     * @param carMojo             the Mojo instance
     * @throws Exception if the local JAR does not exist or cannot be copied
     */
    private static void copyLocalJar(DependencyOverride override, String libDir, String connectorQName,
                                     String connectorArtifactId, CARMojo carMojo) throws Exception {

        File localJar = new File(override.getLocalPath());
        if (!localJar.isAbsolute() || !localJar.getName().toLowerCase(Locale.ROOT).endsWith(".jar")
                || !localJar.exists() || !localJar.isFile()) {
            throw new LibraryResolverException(
                    "connector-config.json specifies localPath for " + connectorArtifactId
                    + " but it is not an absolute path to an existing JAR file: " + override.getLocalPath());
        }
        File targetDir = new File(libDir + File.separator + connectorQName);
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw new LibraryResolverException("Failed to create directory: " + targetDir.getAbsolutePath());
        }
        File dest = new File(targetDir, localJar.getName());
        Files.copy(localJar.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        carMojo.logInfo("Copied local driver JAR per connector-config.json: "
                + localJar.getAbsolutePath() + " → " + dest.getAbsolutePath());
    }

    private static void resolveAndCopyDependencies(List<String> dependencies, List<String> repositories,
                                                   String libDir, Invoker invoker, CARMojo carMojo,
                                                   String connectorName, String connectorArtifactId)
            throws LibraryResolverException {

        File targetDir = new File(libDir + File.separator + connectorName);
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw new LibraryResolverException("Failed to create directory: " + targetDir.getAbsolutePath());
        }
        File tmpPomsDir = new File(new File(libDir).getParentFile(), Constants.TMP_POMS_DIR_NAME);
        if (!tmpPomsDir.exists() && !tmpPomsDir.mkdirs()) {
            throw new LibraryResolverException("Failed to create directory: " + tmpPomsDir.getAbsolutePath());
        }
        try {
            carMojo.logInfo("dependecies    " + dependencies.toString());
            File tempPom = createPomFile(dependencies, repositories, tmpPomsDir, connectorArtifactId);

            InvocationRequest request = new DefaultInvocationRequest();
            request.setBaseDirectory(tempPom.getAbsoluteFile().getParentFile());
            request.setPomFile(tempPom.getAbsoluteFile());
            request.setGoals(Collections.singletonList("dependency:copy-dependencies"));
            Properties properties = new Properties();
            properties.setProperty("excludeTransitive", "true");
            properties.setProperty("outputDirectory", libDir + File.separator + connectorName);
            request.setProperties(properties);

            executeRequest(request, "Failed to resolve and copy dependencies", invoker, carMojo);
        } catch (IOException e) {
            throw new LibraryResolverException("Failed to create temporary pom.xml", e);
        }
    }

    /**
     * Executes a Maven invocation request and logs any errors.
     *
     * @param request      The Maven invocation request to execute.
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
     * Scans the local entries folder and returns the set of connectionType values that are
     * actively used (i.e. have a matching {@code *.init} element with a {@code connectionType} child).
     *
     * @param folderPath path to the local-entries artifact folder
     * @param carMojo    the Mojo instance (for logging)
     * @return set of active connectionType strings (case-sensitive, as written in the XML)
     */
    private static Set<String> scanLocalEntriesForConnections(String folderPath, CARMojo carMojo)
            throws Exception {

        Set<String> active = new HashSet<>();

        File localEntriesFolder = new File(folderPath);
        if (!localEntriesFolder.exists()) {
            return active;
        }

        File[] localEntries = localEntriesFolder.listFiles();
        if (localEntries == null) {
            return active;
        }

        for (File localEntry : localEntries) {
            if (!localEntry.isFile() || !localEntry.getName().endsWith(".xml")) {
                continue;
            }
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(localEntry);
            Element root = doc.getDocumentElement();

            NodeList initNodes = root.getElementsByTagName("*");
            for (int i = 0; i < initNodes.getLength(); i++) {
                Element element = (Element) initNodes.item(i);
                if (element.getNodeName().endsWith(".init")) {
                    NodeList ctNodes = element.getElementsByTagName("connectionType");
                    if (ctNodes.getLength() > 0) {
                        String connectionType = ctNodes.item(0).getTextContent().trim();
                        if (!connectionType.isEmpty()) {
                            active.add(connectionType);
                            carMojo.getLog().info("Found active connectionType: " + connectionType
                                    + " in " + localEntry.getName());
                        }
                    }
                    break;
                }
            }
        }

        return active;
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

    /**
     * Extracts the Maven artifactId from a connector ZIP filename by stripping the version suffix.
     * <p>
     * Example: {@code "mi-connector-file-4.0.36.zip"} → {@code "mi-connector-file"}.
     * Falls back to the name-without-extension if the pattern does not match.
     *
     * @param zipFileName the ZIP filename (with or without the .zip extension)
     * @return the artifactId portion of the filename
     */
    public static String extractArtifactIdFromZipName(String zipFileName) {

        String name = zipFileName.endsWith(Constants.ZIP_EXTENSION)
                ? zipFileName.substring(0, zipFileName.length() - Constants.ZIP_EXTENSION.length())
                : zipFileName;
        Matcher matcher = ARTIFACT_ID_PATTERN.matcher(name);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return name;
    }

}
