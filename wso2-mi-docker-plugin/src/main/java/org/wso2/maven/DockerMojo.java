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

package org.wso2.maven;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.project.MavenProject;
import org.apache.commons.lang.StringUtils;
import org.wso2.maven.handler.HandlerFactory;
import org.wso2.maven.metadata.Application;
import org.wso2.maven.metadata.Artifact;
import org.wso2.maven.metadata.Dependency;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.wso2.maven.Constants.API_TYPE;
import static org.wso2.maven.Constants.CAR_FILE_EXTENSION;
import static org.wso2.maven.Constants.SWAGGER_SUBSTRING;

/**
 * Goal which touches a timestamp file.
 *
 * @goal docker-image
 * @phase package
 **/
public class DockerMojo extends AbstractMojo {
    /**
     * The Maven Project Object
     *
     * @parameter expression="${project}"
     * @required
     */
    MavenProject project;

    /**
     * The location of the archive file
     *
     * @parameter expression="${archiveLocation}"
     */
    String archiveLocation;

    /**
     * The name of the archive file
     *
     * @parameter expression="${archiveName}"
     */
    String archiveName;

    /**
     * The version of the WSO2 Micro Integrator
     *
     * @parameter expression="${miVersion}"
     */
    String miVersion;

    private static final String TMP_DIRECTORY_PATH = System.getProperty("java.io.tmpdir");

    public void logError(String message) {
        getLog().error(message);
    }

    public void logInfo(String message) {
        getLog().info(message);
    }

    public String getArchiveName() {
        return archiveName == null ? project.getArtifactId() + "_" + project.getVersion()
                + CAR_FILE_EXTENSION : archiveName;
    }

    public void execute() throws MojoExecutionException {
        logInfo("Creating Docker Image");
        String basedir = project.getBasedir().toString();

        // with archiveLocation and archiveName compute the resolvedArchiveFilePath and check whether it exists in the given path
        // if exists extract the archive file in to the target folder
        archiveLocation = StringUtils.isEmpty(archiveLocation) ? basedir + File.separator +
                Constants.DEFAULT_TARGET_DIR : archiveLocation;
        Path resolvedArchiveFilePath = Paths.get(archiveLocation).resolve(getArchiveName());
        logInfo("Resolved Archive File Path: " + resolvedArchiveFilePath);
        File file = resolvedArchiveFilePath.toFile();
        if (!file.exists()) {
            logError("Archive file does not exist in the given path: " + resolvedArchiveFilePath);
            throw new MojoExecutionException("Archive file does not exist in the given path: " + resolvedArchiveFilePath);
        }
        logInfo("Archive file exists in the given path: " + resolvedArchiveFilePath);
        Path extractedCarbonApp = extractCarbonApp(resolvedArchiveFilePath);

        Path miHomePath = Paths.get(basedir, Constants.DEFAULT_TARGET_DIR, Constants.TMP_DOCKER_DIR, "carbon-home");

        // Build the app configuration by reading the artifacts.xml file from the extractedCarbonApp
        File metaFile = extractedCarbonApp.resolve(Constants.METADATA_XML).toFile();
        if (!metaFile.exists()) {
            metaFile = extractedCarbonApp.resolve(Constants.ARTIFACTS_XML).toFile();
            if (!metaFile.exists()) {
                logError("artifacts.xml file not found at : " + extractedCarbonApp);
                throw new MojoExecutionException("artifacts.xml file not found at : " + extractedCarbonApp);
            }
        }

        // Build the configuration by reading the artifacts.xml file from the provided path
        // Unmarshal the artifacts.xml file using StAXOMBuilder
        try {
            ArtifactsParser parser = new ArtifactsParser();
            Application application = parser.parse(metaFile.getAbsolutePath());
            Artifact appArtifact = application.getApplicationArtifact();

            // If we don't have (artifacts) for this server image, ignore
            if (appArtifact.getDependencies().isEmpty()) {
                logError("No artifacts found to be deployed in this server. " +
                        "Ignoring Carbon Application : " + getArchiveName());
                return;
            }

            // If we have artifacts, deploy them walk through the dependencies and prepare the deployment
            // for each dependency
            File extractedDir = extractedCarbonApp.toFile();
            File[] allFiles = extractedDir.listFiles();
            if (allFiles == null) {
                return;
            }

            // list to keep all artifacts
            List<Artifact> allArtifacts = new ArrayList<>();
            Map<String, String> swaggerTable = new HashMap<String, String>();
            Map<String, String> apiArtifactMap = new HashMap<String, String>();

            // Iterate through the files in the extracted directory and process artifact.xml file inside each directory.
            for (File fileEntry : allFiles) {
                if (!fileEntry.isDirectory()) {
                    continue;
                }

                File metaFileInDir = Paths.get(fileEntry.getAbsolutePath(), Constants.ARTIFACT_XML).toFile();
                if (!metaFileInDir.exists()) {
                    if (fileEntry.getName().endsWith(Constants.METADATA_DIR)) {
                        File[] metadataFiles = fileEntry.listFiles();
                        if (metadataFiles == null) {
                            return;
                        }
                        for (File metadataFile : metadataFiles) {
                            if (metadataFile.isDirectory()) {
                                File metaFileInMetaDir = Paths.get(metadataFile.getAbsolutePath(), Constants.ARTIFACT_XML)
                                        .toFile();
                                try (FileInputStream fis = new FileInputStream(metaFileInMetaDir.getAbsolutePath())) {
                                    XMLInputFactory factory = XMLInputFactory.newInstance();
                                    XMLStreamReader reader = factory.createXMLStreamReader(fis);
                                    OMElement documentElement = OMXMLBuilderFactory.createStAXOMBuilder(reader)
                                            .getDocumentElement();
                                    ArtifactsParser artifactsParser = new ArtifactsParser();
                                    Artifact artifact = artifactsParser.parseArtifact(documentElement);
                                    // Removing metadata dependencies from the CAPP parent artifact
                                    boolean removed =
                                            appArtifact.getDependencies()
                                                    .removeIf(c -> c.getArtifactName().equals(artifact.getName()));
                                    if (removed)
                                        appArtifact.resolveDependency();

                                    if (metadataFile.getName().contains(SWAGGER_SUBSTRING)) {
                                        File swaggerFile = new File(metadataFile, artifact.getFile());
                                        byte[] bytes = Files.readAllBytes(Paths.get(swaggerFile.getPath()));
                                        String artifactName = artifact.getName()
                                                .substring(0, artifact.getName().indexOf(SWAGGER_SUBSTRING));
                                        swaggerTable.put(artifactName, new String(bytes));
                                    }
                                } catch (FileNotFoundException e) {
                                    logError("Could not find the Artifact.xml file for the metadata");
                                } catch (IOException e) {
                                    logError("Error occurred while reading the swagger file from metadata");
                                }
                            }
                        }
                    }
                    // TODO: process swagger files inside meta directory. Refer: CappDeployer.java -> line 400
                    continue;
                }
                Artifact artifact;
                try (FileInputStream fis = new FileInputStream(metaFileInDir.getAbsolutePath())) {
                    XMLInputFactory factory = XMLInputFactory.newInstance();
                    XMLStreamReader reader = factory.createXMLStreamReader(fis);
                    OMElement documentElement = OMXMLBuilderFactory.createStAXOMBuilder(reader).getDocumentElement();

                    if ("artifact".equals(documentElement.getLocalName())) {
                        ArtifactsParser artifactsParser = new ArtifactsParser();
                        artifact = artifactsParser.parseArtifact(documentElement);

                        if (artifact != null && API_TYPE.equals(artifact.getType())) {
                            apiArtifactMap.put(artifact.getName(), artifact.getVersion());
                        }
                        // TODO: Add apimapping when it is a API type. Refer: CappDeployer.java -> line 442
                    } else {
                        logError("artifact.xml is invalid. Parent Application : "
                                + appArtifact.getName());
                        return;
                    }
                }

                if (artifact == null) {
                    logError("Error while parsing the artifact.xml file: " + metaFileInDir);
                    return;
                }
                artifact.setExtractedPath(fileEntry.getAbsolutePath());
                allArtifacts.add(artifact);
            }
            buildDependencyTree(appArtifact, allArtifacts);

            for (String artifactName : swaggerTable.keySet()) {
                String apiname = apiArtifactMap.get(artifactName);
                if (!StringUtils.isEmpty(apiname)) {
                    // TODO: Add Swagger Definition to the Synapse configuration. Refer: CappDeployer.java -> line 470
                }
            }

            copyResourcesToTarget(miHomePath);
            HandlerFactory handlerFactory = new HandlerFactory(this, miHomePath);
            handlerFactory.getArtifactHandlers().forEach(handler -> handler.copyArtifacts(application));
        } catch (Exception e) {
            logError("Error while parsing the artifacts.xml file: " + e.getMessage());
            throw new MojoExecutionException("Error while parsing the artifacts.xml file", e);
        }

    }

    private Path extractCarbonApp(Path resolvedArchiveFilePath) throws MojoExecutionException {
        // Create a new directory named "extracted" inside the archiveLocation
        Path extractionDir = Paths.get(archiveLocation, "extracted");
        try {
            Files.createDirectories(extractionDir);
        } catch (IOException e) {
            logError("Error creating extraction directory: " + e.getMessage());
            throw new MojoExecutionException("Error creating extraction directory", e);
        }
        logInfo("Extracting the archive file into the new directory: " + extractionDir);

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(resolvedArchiveFilePath))) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                Path newPath = zipSlipProtect(zipEntry, extractionDir);
                if (zipEntry.isDirectory()) {
                    Files.createDirectories(newPath);
                } else {
                    if (newPath.getParent() != null) {
                        if (Files.notExists(newPath.getParent())) {
                            Files.createDirectories(newPath.getParent());
                        }
                    }
                    Files.copy(zis, newPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
        } catch (IOException e) {
            logError("Error while extracting the archive file: " + e.getMessage());
            throw new MojoExecutionException("Error while extracting the archive file", e);
        }
        return extractionDir;
    }

    private static Path zipSlipProtect(ZipEntry zipEntry, Path targetDir) throws IOException {
        Path targetDirResolved = targetDir.resolve(zipEntry.getName());
        Path normalizePath = targetDirResolved.normalize();
        if (!normalizePath.startsWith(targetDir)) {
            throw new IOException("Bad zip entry: " + zipEntry.getName());
        }
        return normalizePath;
    }

    /**
     * If the given artifact is a dependent artifact for the rootArtifact, include it as the actual dependency. The
     * existing one is a dummy one. So remove it. Do this recursively for the dependent artifacts as well..
     *
     * @param rootArtifact - root to start search
     * @param allArtifacts - all artifacts found under current cApp
     */
    private void buildDependencyTree(Artifact rootArtifact, List<Artifact> allArtifacts) {
        for (Dependency dep : rootArtifact.getDependencies()) {
            for (Artifact artifact : allArtifacts) {
                if (dep.getArtifactName().equals(artifact.getName())) {
                    String depVersion = dep.getVersion();
                    String attVersion = artifact.getVersion();
                    if ((depVersion == null && attVersion == null) ||
                            (depVersion != null && depVersion.equals(attVersion))) {
                        dep.setArtifact(artifact);
                        rootArtifact.resolveDependency();
                        break;
                    }
                }
            }

            // if we've found the dependency, check for it's dependencies as well..
            if (dep.getArtifactName() != null) {
                buildDependencyTree(dep.getArtifact(), allArtifacts);
            }
        }
    }

    /**
     * Copy the original docker folder to the target directory.
     *
     * @throws MojoExecutionException while copying the resources
     */
    private void copyResourcesToTarget(Path tmpCarbonHomeDirectory) throws MojoExecutionException {
        try {
            String projectLocation = project.getBasedir().getAbsolutePath();
            File sourceDir = Paths.get(projectLocation, "deployment", "docker", "resources").toFile();
            File targetDir = tmpCarbonHomeDirectory.resolve("repository").resolve("resources")
                    .resolve("security").toFile();
            FileUtils.copyDirectory(sourceDir, targetDir);
            FileUtils.copyFile(Paths.get(projectLocation, "deployment", "docker", "Dockerfile").toFile(),
                    Paths.get(projectLocation, Constants.DEFAULT_TARGET_DIR, Constants.TMP_DOCKER_DIR,
                            Constants.DOCKER_FILE).toFile());
            FileUtils.copyFile(Paths.get(projectLocation, "deployment", "deployment.toml").toFile(),
                        tmpCarbonHomeDirectory.resolve("conf").resolve("deployment.toml").toFile());
            FileUtils.copyDirectory(Paths.get(projectLocation, "deployment", "libs").toFile(),
                    tmpCarbonHomeDirectory.resolve("lib").toFile());
        } catch (IOException e) {
            throw new MojoExecutionException("Exception while parsing the deployment.toml file \n" + e);
        }
    }
}
