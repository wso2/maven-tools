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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.wso2.maven.Constants.CAR_FILE_EXTENSION;
import static org.wso2.maven.Constants.CONF_DIR;
import static org.wso2.maven.Constants.DEPLOYMENT_DIR;
import static org.wso2.maven.Constants.DEPLOYMENT_TOML_FILE;
import static org.wso2.maven.Constants.DOCKERFILE_FILE;
import static org.wso2.maven.Constants.DOCKER_DIR;
import static org.wso2.maven.Constants.REPOSITORY_DIR;
import static org.wso2.maven.Constants.RESOURCES_DIR;
import static org.wso2.maven.Constants.SECURITY_DIR;
import static org.wso2.maven.Constants.TMP_CARBONAPPS_DIR;
import static org.wso2.maven.Constants.TMP_CARBON_HOME;

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
        Path miHomePath = Paths.get(basedir, Constants.DEFAULT_TARGET_DIR, Constants.TMP_DOCKER_DIR, TMP_CARBON_HOME);

        Path extractedCarbonApp = extractCarbonApp(resolvedArchiveFilePath, miHomePath);

        // Build the app configuration by reading the artifacts.xml file from the extractedCarbonApp
        File metaFile = extractedCarbonApp.resolve(Constants.METADATA_XML).toFile();
        if (!metaFile.exists()) {
            metaFile = extractedCarbonApp.resolve(Constants.ARTIFACTS_XML).toFile();
            if (!metaFile.exists()) {
                logError("artifacts.xml file not found at : " + extractedCarbonApp);
                throw new MojoExecutionException("artifacts.xml file not found at : " + extractedCarbonApp);
            }
        }

        // Copy resources to the target directory.
        copyResourcesToTarget(miHomePath);
    }

    private Path extractCarbonApp(Path resolvedArchiveFilePath, Path carbonHomePath) throws MojoExecutionException {
        // Create a new directory named carbonapps in the /target/tmp_docker/carbon-home directory
        Path carbonappsDir = carbonHomePath.resolve(TMP_CARBONAPPS_DIR);
        try {
            Files.createDirectories(carbonappsDir);
        } catch (IOException e) {
            logError("Error creating extraction directory: " + e.getMessage());
            throw new MojoExecutionException("Error creating extraction directory", e);
        }

        // Copy the archive file into the /target/tmp_docker/carbon-home/carbonapps directory
        logInfo("Copying the archive file into the new directory: " + carbonappsDir);
        try {
            Files.copy(resolvedArchiveFilePath, carbonappsDir.resolve(resolvedArchiveFilePath.getFileName()),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new MojoExecutionException("Error while copying the archive file", e);
        }

        // Create a new directory to extract the archive file
        Path extractionDir = carbonappsDir.resolve(resolvedArchiveFilePath.getFileName().toString().replace(CAR_FILE_EXTENSION, ""));

        // Extract the archive file into the /target/tmp_docker/carbon-home/carbonapps directory
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
     * Copy the original docker folder to the target directory.
     *
     * @throws MojoExecutionException while copying the resources
     */
    private void copyResourcesToTarget(Path tmpCarbonHomeDirectory) throws MojoExecutionException {
        try {
            String projectLocation = project.getBasedir().getAbsolutePath();
            Path projectDeploymentDir = Paths.get(projectLocation, DEPLOYMENT_DIR);
            Path projectDockerDir = projectDeploymentDir.resolve(DOCKER_DIR);
            File sourceDir = projectDockerDir.resolve(RESOURCES_DIR).toFile();
            File targetDir = tmpCarbonHomeDirectory.resolve(REPOSITORY_DIR).resolve(RESOURCES_DIR)
                    .resolve(SECURITY_DIR).toFile();
            FileUtils.copyDirectory(sourceDir, targetDir);
            FileUtils.copyFile(projectDockerDir.resolve(DOCKERFILE_FILE).toFile(),
                    Paths.get(projectLocation, Constants.DEFAULT_TARGET_DIR, Constants.TMP_DOCKER_DIR,
                            Constants.DOCKER_FILE).toFile());
            FileUtils.copyFile(projectDeploymentDir.resolve(DEPLOYMENT_TOML_FILE).toFile(),
                        tmpCarbonHomeDirectory.resolve(CONF_DIR).resolve(DEPLOYMENT_TOML_FILE).toFile());
        } catch (IOException e) {
            throw new MojoExecutionException("Exception while parsing the deployment.toml file \n" + e);
        }
    }
}
