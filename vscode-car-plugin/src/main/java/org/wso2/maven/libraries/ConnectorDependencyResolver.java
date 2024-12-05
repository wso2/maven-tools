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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.wso2.maven.MavenUtils.setupInvoker;

/**
 * Resolves dependencies for connectors.
 */
public class ConnectorDependencyResolver {

    /**
     * Resolves dependencies for connectors.
     *
     * @param carMojo The Mojo instance.
     * @throws Exception If an error occurs while resolving dependencies.
     */
    public static void resolveDependencies(CARMojo carMojo) throws Exception {

        String extractedDir = Constants.POM_FILE + File.separator + Constants.EXTRACTED_CONNECTORS;
        String libDir = Constants.DEFAULT_TARGET_FOLDER + File.separator + Constants.LIBS;

        // Ensure target directories exist
        new File(extractedDir).mkdirs();
        new File(libDir).mkdirs();

        // Resolve connector ZIP files from pom.xml
        List<File> connectorZips = resolveConnectorZips(Constants.POM_FILE);

        List<File> dependencyFiles = new ArrayList<>();
        // Extract all connector ZIP files
        for (File zipFile : connectorZips) {
            String targetExtractDir = extractedDir + File.separator + zipFile.getName().replace(".zip", "");
            extractZipFile(zipFile, targetExtractDir);
            dependencyFiles.add(new File(targetExtractDir + File.separator + Constants.DEPENDENCY_XML));
        }

        String mavenHome = MavenUtils.getMavenHome();
        Invoker invoker = new DefaultInvoker();
        setupInvoker(mavenHome, invoker);

        if (dependencyFiles != null) {
            for (File dependencyFile : dependencyFiles) {
                resolveMavenDependencies(dependencyFile, libDir, invoker, carMojo);
            }
        }
        carMojo.logInfo("All dependencies resolved and extracted successfully.");
    }

    /**
     * Resolves connector ZIP files from the pom.xml file.
     *
     * @param pomFilePath The path to the pom.xml file.
     * @return The list of connector ZIP files.
     * @throws MavenInvocationException If an error occurs while resolving dependencies.
     */
    private static List<File> resolveConnectorZips(String pomFilePath) throws MavenInvocationException {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(new File(pomFilePath));
        request.setGoals(Collections.singletonList("dependency:copy-dependencies -DincludeTypes=zip"));

        Invoker invoker = new DefaultInvoker();
        invoker.execute(request);

        File dependenciesDir = new File(Constants.DEFAULT_TARGET_FOLDER + File.separator + Constants.DEPENDENCY);
        if (!dependenciesDir.exists()) {
            throw new IllegalStateException("Dependency resolution failed, no dependencies directory found.");
        }
        List<File> connectorZips = new ArrayList<>();
        for (File file : dependenciesDir.listFiles()) {
            if (file.getName().endsWith(".zip")) {
                connectorZips.add(file);
            }
        }
        return connectorZips;
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
     * Resolves Maven dependencies from a dependency.xml file.
     *
     * @param dependencyXml The dependency.xml file.
     * @param libDir The directory to copy the dependencies to.
     * @param invoker The Maven Invoker.
     * @param carMojo The Mojo instance.
     * @throws Exception If an error occurs while resolving dependencies.
     */
    private static void resolveMavenDependencies(File dependencyXml, String libDir, Invoker invoker, CARMojo carMojo)
            throws Exception {

        if (!dependencyXml.exists()) {
            return;
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(dependencyXml);

        NodeList dependencyNodes = doc.getElementsByTagName("dependency");
        NodeList repositoryNodes = doc.getElementsByTagName("repository");
        StringBuilder repositoriesUrl = new StringBuilder();
        for (int i = 0; i < repositoryNodes.getLength(); i++) {
            if (i > 0) {
                repositoriesUrl.append(",");
            }
            Element repository = (Element) repositoryNodes.item(i);
            String url = repository.getElementsByTagName("url").item(0).getTextContent();
            repositoriesUrl.append(url);
        }
        for (int i = 0; i < dependencyNodes.getLength(); i++) {
            Element dependency = (Element) dependencyNodes.item(i);
            String groupId = dependency.getElementsByTagName("groupId").item(0).getTextContent();
            String artifactId = dependency.getElementsByTagName("artifactId").item(0).getTextContent();
            String version = dependency.getElementsByTagName("version").item(0).getTextContent();
            resolveMavenDependency(groupId, artifactId, version, invoker, carMojo, repositoriesUrl.toString());
            copyMavenDependency(groupId, artifactId, version, libDir, invoker, carMojo);
        }
    }

    /**
     * Resolves a Maven dependency.
     *
     * @param groupId The group ID of the dependency.
     * @param artifactId The artifact ID of the dependency.
     * @param version The version of the dependency.
     * @param invoker The Maven Invoker.
     * @param carMojo The Mojo instance.
     * @param repositories The repositories to use for resolving the dependency.
     * @throws LibraryResolverException If an error occurs while resolving the dependency.
     */
    private static void resolveMavenDependency(String groupId, String artifactId, String version, Invoker invoker,
                                               CARMojo carMojo, String repositories) throws LibraryResolverException {

        InvocationRequest request = new DefaultInvocationRequest();
        request.setGoals(Collections.singletonList(String.format("dependency:get -DgroupId=%s -DartifactId=%s " +
                "-Dversion=%s -DremoteRepositories=\"%s\"", groupId, artifactId, version, repositories)));
        executeRequest(request, "Failed to resolve Maven dependency: " + groupId + ":" +
                artifactId + ":" + version, invoker, carMojo);
    }

    /**
     * Copies a Maven dependency to the specified directory.
     *
     * @param groupId The group ID of the dependency.
     * @param artifactId The artifact ID of the dependency.
     * @param version The version of the dependency.
     * @param libDir The directory to copy the dependency to.
     * @param invoker The Maven Invoker.
     * @param carMojo The Mojo instance.
     * @throws LibraryResolverException If an error occurs while copying the dependency.
     */
    private static void copyMavenDependency(String groupId, String artifactId, String version, String libDir,
                                            Invoker invoker, CARMojo carMojo) throws LibraryResolverException {

        InvocationRequest request = new DefaultInvocationRequest();
        request.setGoals(Collections.singletonList(String.format(
                "dependency:copy -Dartifact=%s:%s:%s -DoutputDirectory=\"%s\" -DlocalRepositoryDirectory=\"%s\" " +
                        "-Dmaven.legacyLocalRepo=true",
                groupId, artifactId, version, libDir, libDir)));
        executeRequest(request, "Failed to copy Maven dependency: " + groupId + ":" +
                artifactId + ":" + version, invoker, carMojo);
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
}
