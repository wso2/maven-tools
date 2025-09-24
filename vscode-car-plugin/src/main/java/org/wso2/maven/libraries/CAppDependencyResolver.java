/*
 * Copyright (c) 2025, WSO2 LLC (http://www.wso2.com).
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
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.wso2.maven.CARMojo;
import org.wso2.maven.CAppDependency;
import org.wso2.maven.Constants;
import org.wso2.maven.model.ArtifactDependency;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import static org.apache.commons.io.FileUtils.copyFile;
import static org.wso2.maven.MavenUtils.createPomFile;
import static org.wso2.maven.MavenUtils.setupInvoker;

/**
 * Utility class for resolving CApp (Carbon Application) dependencies in a Maven project.
 */
public class CAppDependencyResolver {

    /**
     * Resolves CApp (Carbon Application) dependencies for the given Maven project.
     * Executes the Maven dependency copy, checks for fat CAR packaging, extracts dependent CApp files,
     * copies their contents to the archive directory, handles config properties merging, and updates artifact dependencies.
     *
     * @param carMojo      The CARMojo instance used for logging and project context.
     * @param project      The Maven project for which dependencies are being resolved.
     * @param archiveDir   The directory where dependencies should be extracted/copied.
     * @param dependencies The list of artifact dependencies to update.
     */
    public static void resolveDependencies(CARMojo carMojo, MavenProject project, String archiveDir,
                                           List<ArtifactDependency> dependencies,
                                           List<ArtifactDependency> metaDependencies) throws Exception {

        File dependenciesDir = new File(Constants.DEFAULT_TARGET_FOLDER + File.separator + Constants.DEPENDENCY);
        executeDependencyCopy(project.getBasedir(), new File(project.getBasedir(), Constants.POM_FILE), dependenciesDir);
        boolean fatCarEnabled = CAppDependencyResolver.isFatCarEnabled(project);
        if (fatCarEnabled) {
            ArrayList<File> cAppFiles = getResolvedDependentCAppFiles(project.getBasedir(), dependenciesDir,
                    project.getArtifactId(), project.getVersion(), carMojo);
            String dependencyDir = archiveDir + File.separator + Constants.DEPENDENCIES;
            for (File cappFile : cAppFiles) {
                copyFile(cappFile, new File(dependencyDir, cappFile.getName()));
            }
        }
    }

    /**
     * Checks if the fat.car.enable property is set to true in the Maven project's properties.
     *
     * @param project The Maven project to check for the fat.car.enable property.
     * @return true if fat.car.enable is set to true, false otherwise.
     */
    public static boolean isFatCarEnabled(MavenProject project) {

        String fatCarEnabled = project.getProperties().getProperty(Constants.FAT_CAR_ENABLE_PROPERTY);
        return Boolean.parseBoolean(fatCarEnabled);
    }

    /**
     * Handles the `config.properties` file by copying it from the source directory to the target directory.
     * If the target directory already contains a `config.properties` file, the method merges the contents
     * of the source and target files.
     *
     * @param srcDir    The source directory containing the `config.properties` file.
     * @param targetDir The target directory where the `config.properties` file will be copied or merged.
     * @throws IOException If an error occurs during file operations.
     */
    public static void handleConfigPropertiesFile(File srcDir, File targetDir) throws IOException {

        File srcConfigFile = new File(srcDir, Constants.CONFIG_PROPERTIES_FILE);
        File targetConfigFile = new File(targetDir, Constants.CONFIG_PROPERTIES_FILE);

        if (!srcConfigFile.exists()) {
            return;
        }
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        if (!targetConfigFile.exists()) {
            targetConfigFile.createNewFile();
        }
        mergePropertiesFiles(srcConfigFile, targetConfigFile);
    }

    /**
     * Merges the contents of two properties files.
     * The method reads the source and target files, combines their unique lines,
     * and writes the merged content back to the target file.
     *
     * @param sourceFile The source properties file to merge from.
     * @param targetFile The target properties file to merge into.
     */
    public static void mergePropertiesFiles(File sourceFile, File targetFile) throws IOException {

        List<String> mergedLines = new ArrayList<>();

        try (BufferedReader sourceReader = new BufferedReader(new FileReader(sourceFile));
             BufferedReader targetReader = new BufferedReader(new FileReader(targetFile))) {

            // Read and add lines from the source file
            String line;
            while ((line = sourceReader.readLine()) != null) {
                if (!line.trim().isEmpty() && !mergedLines.contains(line)) {
                    mergedLines.add(line);
                }
            }

            // Read and add lines from the target file
            while ((line = targetReader.readLine()) != null) {
                if (!line.trim().isEmpty() && !mergedLines.contains(line)) {
                    mergedLines.add(line);
                }
            }
        } catch (IOException e) {
            throw new IOException("Error reading properties files: " + e.getMessage(), e);
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile))) {
            for (String mergedLine : mergedLines) {
                writer.write(mergedLine);
                writer.newLine();
            }
        } catch (IOException e) {
            throw new IOException("Error writing merged properties file: " + e.getMessage(), e);
        }
    }

    /**
     * Updates the list of artifact dependencies by reading and parsing the specified artifact dependency file.
     * Parses the XML file, extracts dependency information, and adds new dependencies to the provided list if they do not already exist.
     * Logs an error if a dependency already exists or if there is an issue reading the file.
     *
     * @param artifactDependencyFile The XML file containing artifact dependency definitions.
     * @param artifactDependencies   The list to update with new artifact dependencies.
     * @param carMojo                The CARMojo instance used for logging.
     */
    public static void updateArtifactDependencies(File artifactDependencyFile,
                                                  List<ArtifactDependency> artifactDependencies, CARMojo carMojo)
            throws Exception {

        if (artifactDependencyFile.exists()) {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(artifactDependencyFile);
                document.getDocumentElement().normalize();

                NodeList artifactsNodes = document.getElementsByTagName(Constants.ARTIFACTS);
                for (int i = 0; i < artifactsNodes.getLength(); i++) {
                    Element artifactsElement = (Element) artifactsNodes.item(i);
                    NodeList artifactNodes = artifactsElement.getElementsByTagName(Constants.ARTIFACT);
                    for (int j = 0; j < artifactNodes.getLength(); j++) {
                        Element artifactElement = (Element) artifactNodes.item(j);
                        // Extract attributes from the artifact element
                        NodeList dependencyNodes = artifactElement.getElementsByTagName(Constants.DEPENDENCY);
                        for (int k = 0; k < dependencyNodes.getLength(); k++) {
                            Element dependencyElement = (Element) dependencyNodes.item(k);
                            String artifact = dependencyElement.getAttribute(Constants.ARTIFACT);
                            String version = dependencyElement.getAttribute(Constants.VERSION);
                            final String include = dependencyElement.getAttribute(Constants.INCLUDE);
                            String serverRole = dependencyElement.getAttribute(Constants.SERVER_ROLE);
                            ArtifactDependency artifactDependency =
                                    new ArtifactDependency(artifact, version, serverRole,
                                            Boolean.parseBoolean(include));

                            // Check if the dependency already exists
                            boolean exists = artifactDependencyExists(artifactDependencies, artifactDependency);
                            if (!exists) {
                                artifactDependencies.add(artifactDependency);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                throw new Exception("Error while reading artifacts XML file: " + artifactDependencyFile.getAbsolutePath(), e);
            }
        } else {
            throw new Exception("Artifacts XML file not found: " + artifactDependencyFile.getAbsolutePath());
        }
    }

    /**
     * Checks if a given artifact dependency already exists in the list of artifact dependencies.
     *
     * @param dependencies       The list of existing artifact dependencies.
     * @param artifactDependency The artifact dependency to check for existence.
     * @return `true` if the artifact dependency exists, `false` otherwise.
     */
    private static boolean artifactDependencyExists(List<ArtifactDependency> dependencies,
                                                    ArtifactDependency artifactDependency) {

        for (ArtifactDependency dep : dependencies) {
            if (dep.getArtifact().equals(artifactDependency.getArtifact()) &&
                    dep.getVersion().equals(artifactDependency.getVersion()) &&
                    dep.getServerRole().equals(artifactDependency.getServerRole()) &&
                    dep.getInclude().equals(artifactDependency.getInclude())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Unzips the given .zip file into the specified extraction directory.
     *
     * @param zipFile    The .zip file to be extracted.
     * @param extractDir The directory where the contents of the .zip file will be extracted.
     * @throws IOException If an error occurs during file extraction.
     */
    public static void unzipFile(File zipFile, File extractDir) throws IOException {

        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipFile.toPath()))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                File extractedFile = new File(extractDir, entry.getName());
                if (entry.isDirectory()) {
                    extractedFile.mkdirs();
                } else {
                    try (FileOutputStream outputStream = new FileOutputStream(extractedFile)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = zipInputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, length);
                        }
                    }
                }
                zipInputStream.closeEntry();
            }
        }
    }

    /**
     * Executes the Maven goal `dependency:copy-dependencies` to copy all dependencies of type `car`
     * to the target directory. This method uses the Maven Invoker API to programmatically invoke
     * the Maven goal.
     *
     */
     static void executeDependencyCopy(File projectDir, File pomFile, File outputDir)
             throws MavenInvocationException, MojoExecutionException {

        Invoker invoker = new DefaultInvoker();
        setupInvoker(invoker, projectDir.getPath());
        DefaultInvocationRequest request = new DefaultInvocationRequest();
        request.setBaseDirectory(projectDir);
        request.setGoals(Collections.singletonList(
                String.format("-f %s dependency:copy-dependencies -DincludeTypes=car -DoutputDirectory=%s",
                        pomFile.getAbsolutePath(), outputDir.getAbsolutePath())));
        invoker.execute(request);
    }

    /**
     * Retrieves a list of resolved dependent CApp \(.car\) files from the dependencies directory.
     * This method scans the dependencies directory for .car files, processes them,
     * and collects all their dependencies recursively, avoiding cycles.
     *
     * @param projectDir      The project directory
     * @param dependenciesDir The directory containing dependency .car files.
     * @param carMojo         The CARMojo instance used for logging and project context.
     * @return An ArrayList of resolved dependent CApp files.
     */
    public static ArrayList<File> getResolvedDependentCAppFiles(File projectDir, File dependenciesDir,
                                                                String artifactId, String version, CARMojo carMojo)
            throws Exception {

        if (!dependenciesDir.exists()) {
            return new ArrayList<>();
        }

        ArrayList<File> cAppFiles = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        // Add the current project artifact to the visited set
        visited.add(artifactId + Constants.HYPHEN + version);  // e.g., "my-service-1.0.0"

        for (File file : Objects.requireNonNull(dependenciesDir.listFiles())) {
            if (file.getName().endsWith(Constants.CAR_EXTENSION)) {
                cAppFiles.add(file);
            }
        }

        for (File file : Objects.requireNonNull(dependenciesDir.listFiles())) {
            if (file.getName().endsWith(Constants.CAR_EXTENSION)) {
                // Extract artifactId-version from filename (e.g., my-service-1.0.0.car)
                String baseName = file.getName().replace(Constants.CAR_EXTENSION, StringUtils.EMPTY);
                visited.add(baseName);  // e.g., "my-service-1.0.0"

                collectDependentCAppFiles(projectDir, dependenciesDir, file, cAppFiles, visited, carMojo);
            }
        }
        return cAppFiles;
    }

    /**
     * Recursively collects dependent CAPP files from the given .car file.
     * This method reads the descriptor.xml file inside the .car file, extracts
     * dependency information, and resolves each dependency recursively.
     *
     * @param projectDir The project directory where the .car file is located.
     * @param dependenciesDir The directory containing dependency .car files.
     * @param carFile   The .car file to process.
     * @param cAppFiles The list to collect resolved CAPP files.
     * @param visited   A set to track already processed dependencies to avoid cycles.
     * @param carMojo   The `CARMojo` instance used for logging and project context.
     */
    public static void collectDependentCAppFiles(File projectDir, File dependenciesDir, File carFile,
                                                 ArrayList<File> cAppFiles, Set<String> visited, CARMojo carMojo)
            throws Exception {

        try (ZipFile zipFile = new ZipFile(carFile)) {
            ZipEntry descriptorEntry = zipFile.getEntry(Constants.DESCRIPTOR_XML);
            if (descriptorEntry == null) {
                return;
            }

            InputStream inputStream = zipFile.getInputStream(descriptorEntry);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);
            document.getDocumentElement().normalize();

            NodeList dependencyNodes = document.getElementsByTagName(Constants.DEPENDENCY);
            carMojo.logInfo("Processing " + dependencyNodes.getLength() + " dependencies in " + carFile.getName());
            for (int i = 0; i < dependencyNodes.getLength(); i++) {
                Element dependencyElement = (Element) dependencyNodes.item(i);
                String groupId = dependencyElement.getAttribute(Constants.GROUP_ID);
                String artifactId = dependencyElement.getAttribute(Constants.ARTIFACT_ID);
                String version = dependencyElement.getAttribute(Constants.VERSION);

                carMojo.logInfo(
                        "Resolving dependency: " + groupId + Constants.COLON + artifactId + Constants.COLON + version);
                if (StringUtils.isNotEmpty(artifactId) && StringUtils.isNotEmpty(version)) {
                    String key = artifactId + Constants.HYPHEN + version;
                    if (visited.contains(key)) {
                        continue; // Skip already processed dependency
                    }
                    visited.add(key);

                    File dependentCarFile = findCarFileInDependencies(dependenciesDir, artifactId, version);
                    if (dependentCarFile != null && !cAppFiles.contains(dependentCarFile)) {
                        cAppFiles.add(dependentCarFile);
                        collectDependentCAppFiles(projectDir, dependenciesDir, dependentCarFile, cAppFiles, visited,
                                carMojo);
                    } else {
                        // Try fetching from local Maven repo and copy to dependency folder
                        File copiedFile =
                                fetchCarFileFromMavenRepo(projectDir, dependenciesDir, groupId, artifactId, version, carMojo);
                        if (copiedFile != null) {
                            cAppFiles.add(copiedFile);
                            collectDependentCAppFiles(projectDir, dependenciesDir, copiedFile, cAppFiles, visited,
                                    carMojo);
                        } else {
                            throw new Exception("Could not find .car in maven repository for groupId: " + groupId +
                                    ", artifactId: " + artifactId + ", version: " + version);
                        }
                    }
                }
            }
        }
    }

    /**
     * Finds a \`.car\` file in the given dependencies directory that matches the specified groupId, artifactId, and version.
     *
     * @param dependenciesDir The directory containing dependency \`.car\` files.
     * @param artifactId      The artifact ID of the dependency.
     * @param version         The version of the dependency.
     * @return The matching \`.car\` file if found, otherwise null.
     */
    public static File findCarFileInDependencies(File dependenciesDir, String artifactId, String version) {

        for (File file : Objects.requireNonNull(dependenciesDir.listFiles())) {
            if (file.getName().endsWith(Constants.CAR_EXTENSION)) {
                if (file.getName().equals(artifactId + Constants.HYPHEN + version + Constants.CAR_EXTENSION)) {
                    return file;
                }
            }
        }
        return null;
    }

    /**
     * Attempts to fetch a \`.car\` file for the specified groupId, artifactId, and version from the local Maven repository.
     * If found, copies it to the dependencies directory and returns the copied file.
     *
     * @param projectDir     The project directory where the Maven project is located.
     * @param dependenciesDir The directory where the dependency \`.car\` file should be copied.
     * @param groupId         The group ID of the dependency.
     * @param artifactId      The artifact ID of the dependency.
     * @param version         The version of the dependency.
     * @return The copied \`.car\` file if found and copied, otherwise null.
     */
    public static File fetchCarFileFromMavenRepo(File projectDir, File dependenciesDir, String groupId,
                                                 String artifactId, String version, CARMojo carMojo) throws Exception {

        File tempPomFile = createPomFile(
                Collections.singletonList(groupId + Constants.COLON + artifactId + Constants.COLON + version + Constants.COLON + Constants.CAR_TYPE),
                Collections.<String>emptyList());

        try {
            executeDependencyCopy(projectDir, tempPomFile, dependenciesDir);
            File fetchedCarFile = new File(dependenciesDir, artifactId + Constants.HYPHEN + version + Constants.CAR_EXTENSION);
            if (fetchedCarFile.exists()) {
                return fetchedCarFile;
            }
            if (!tempPomFile.delete()) {
                carMojo.getLog().warn("Failed to delete temporary pom.xml: " + tempPomFile.getAbsolutePath());
            }
        } catch (MavenInvocationException e) {
            throw new Exception("Error while fetching .car from Maven repo: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Retrieves the list of top-level CApp dependencies from the given Maven project.
     * Only dependencies of type `car` are included in the result.
     *
     * @param project The Maven project to extract dependencies from.
     * @return A list of `CAppDependency` objects representing top-level CApp dependencies.
     */
    public static List<CAppDependency> getTopLevelCAppDependencies(MavenProject project) {

        List<CAppDependency> cAppDependencies = new ArrayList<>();
        for (Object depObj : project.getDependencies()) {
            if (depObj instanceof Dependency) {
                Dependency dep = (Dependency) depObj;
                if (Constants.CAR_TYPE.equals(dep.getType())) {
                    CAppDependency cAppDependency =
                            new CAppDependency(dep.getGroupId(), dep.getArtifactId(), dep.getVersion());
                    cAppDependencies.add(cAppDependency);
                }
            }
        }
        return cAppDependencies;
    }
}
