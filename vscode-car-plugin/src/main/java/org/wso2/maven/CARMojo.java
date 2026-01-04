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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
import org.apache.commons.lang.StringUtils;
import org.wso2.maven.datamapper.DataMapperBundler;
import org.wso2.maven.datamapper.DataMapperException;
import org.wso2.maven.libraries.CAppDependencyResolver;
import org.wso2.maven.libraries.ConnectorDependencyResolver;
import org.wso2.maven.model.ArchiveException;
import org.wso2.maven.model.ArtifactDependency;

/**
 * Goal which touches a timestamp file.
 */
@Mojo(name = "car", defaultPhase = LifecyclePhase.PACKAGE)
public class CARMojo extends AbstractMojo {

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
     * The source directory
     *
     * @parameter expression="${sourceDirectory}"
     */
    private String sourceDirectory;

    public void logError(String message) {
        getLog().error(message);
    }

    public void logWarn(String message) {
        getLog().warn(message);
    }

    public void logInfo(String message) {
        getLog().info(message);
    }

    public String getArchiveName() {
        return archiveName == null ? project.getArtifactId() + "_" + project.getVersion() : archiveName;
    }

    public void execute() throws MojoExecutionException, MojoFailureException {

        String basedir = project.getBasedir().toString();
        archiveLocation = StringUtils.isEmpty(archiveLocation) ? basedir + File.separator +
                Constants.DEFAULT_TARGET_FOLDER : archiveLocation;
        if (StringUtils.isEmpty(sourceDirectory)) {
            sourceDirectory = basedir;
        }
        String artifactFolderPath = sourceDirectory + File.separator + Constants.ARTIFACTS_FOLDER_PATH;
        String resourcesFolderPath = sourceDirectory + File.separator + Constants.RESOURCES_FOLDER_PATH;
        DataMapperBundler bundler = null;
        try {
            try {
                bundler = new DataMapperBundler(this, basedir, sourceDirectory, resourcesFolderPath);
                bundler.bundleDataMapper();
            } catch (DataMapperException e) {
                getLog().error("Error during data mapper bundling: " + e.getMessage(), e);
                throw new MojoExecutionException("Data Mapper bundling failed.", e);
            }

            processCARCreation(basedir, artifactFolderPath, resourcesFolderPath);
        } finally {
            if (bundler != null) {
                try {
                    bundler.deleteGeneratedDatamapperArtifacts();
                } catch (DataMapperException e) {
                    getLog().error("Error during data mapper cleanup: " + e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Handles the creation of the Composite Application Archive (CAR).
     */
    private void processCARCreation(String basedir, String artifactFolderPath, String resourcesFolderPath)
            throws MojoExecutionException {
        appendLogs();

        File artifactFolder = new File(artifactFolderPath);
        File resourcesFolder = new File(resourcesFolderPath);
        boolean createdArchiveDirectory = false;
        String tempTargetDir = basedir + File.separator + Constants.TEMP_TARGET_DIR_NAME;
        File targetFolder = new File(tempTargetDir);
        if (!targetFolder.exists()) {
            createdArchiveDirectory = targetFolder.mkdir();
        }
        CAppHandler cAppHandler = new CAppHandler(getArchiveName(), this);
        List<ArtifactDependency> dependencies = new ArrayList<>();
        List<ArtifactDependency> metaDependencies = new ArrayList<>();
        if (createdArchiveDirectory || targetFolder.exists()) {
            String projectVersion = project.getVersion().replace("-SNAPSHOT", "");
            cAppHandler.processArtifacts(artifactFolder, tempTargetDir, dependencies, metaDependencies, projectVersion);
            cAppHandler.processAPIDefinitions(resourcesFolder, tempTargetDir, metaDependencies, projectVersion);
            cAppHandler.processResourcesFolder(resourcesFolder, tempTargetDir, dependencies,
                    metaDependencies, projectVersion, project);
            cAppHandler.processClassMediators(dependencies, project);
            resolveConnectorDependencies();
            cAppHandler.processConnectorLibDependencies(dependencies, project);
            resolveCAppDependencies(tempTargetDir, dependencies, metaDependencies);
            cAppHandler.createDependencyArtifactsXmlFile(tempTargetDir, dependencies, metaDependencies, project);
            cAppHandler.createDependencyDescriptorFile(tempTargetDir, project);
            File fileToZip = new File(tempTargetDir);
            String fileExtension = ".car";
            try {
                File carFile = getArchiveFile(fileExtension);
                zipFolder(fileToZip.getPath(), carFile.getPath(), fileExtension);
                // Attach carFile to Maven context.
                this.project.getArtifact().setFile(carFile);
            } catch (ArchiveException e) {
                logError("Error occurred while creating the .car file");
                logError(e.getMessage());
            }
            recursiveDelete(fileToZip, fileExtension);
        } else {
            getLog().error("Could not create the archive directory.");
        }
    }

    /**
     * Append log of BUILD process when building started.
     */
    private void appendLogs() {
        getLog().info("------------------------------------------------------------------------");
        getLog().info("Building Composite Project");
        getLog().info("------------------------------------------------------------------------");
    }

    /**
     * Get archive file/directory.
     *
     * @param fileExtension to be included
     * @return empty archive file
     */
    private File getArchiveFile(String fileExtension) {
        File archiveLocation = new File(this.archiveLocation);
        if (!archiveLocation.exists()) {
            archiveLocation.mkdir();
        }
        return new File(archiveLocation, getArchiveName() + fileExtension);
    }

    /**
     * Zip a folder.
     *
     * @param srcFolder:   file path of the archive directory
     * @param destZipFile: file path of the .car file
     */
    private void zipFolder(String srcFolder, String destZipFile, String fileExtension) throws ArchiveException {
        try (FileOutputStream fileWriter = new FileOutputStream(destZipFile);
             ZipOutputStream zip = new ZipOutputStream(fileWriter)) {
            addFolderContentsToZip(srcFolder, zip, fileExtension);
            zip.flush();
        } catch (IOException ex) {
            throw new ArchiveException(Constants.ARCHIVE_EXCEPTION_MSG, ex);
        }
    }

    /**
     * Add contents in the given folder in the .car file.
     *
     * @param srcFolder: file path of the archive directory
     * @param zip:       ZipOutputStream
     */
    private void addFolderContentsToZip(String srcFolder, ZipOutputStream zip, String fileExtension)
            throws ArchiveException {
        File folder = new File(srcFolder);
        String[] fileList = folder.list();
        if (fileList == null) {
            return;
        }
        try {
            int i = 0;
            while (fileList.length != i) {
                if (new File(folder, fileList[i]).isDirectory()) {
                    zip.putNextEntry(new ZipEntry(fileList[i] + "/"));
                    zip.closeEntry();
                }
                // stop the recursive loop
                if (getArchiveName().concat(fileExtension).equals(fileList[i])) {
                    i++;
                    continue;
                }
                addToZip(Constants.EMPTY_STRING, srcFolder + "/" + fileList[i], zip);
                i++;
            }
        } catch (IOException ex) {
            throw new ArchiveException(Constants.ARCHIVE_EXCEPTION_MSG, ex);
        }
    }

    /**
     * Add the given folder to the given location in the .car file.
     *
     * @param path:    file path in the .car file
     * @param srcFile: file to be included in the .car file
     * @param zip:     ZipOutputStream
     */
    private void addToZip(String path, String srcFile, ZipOutputStream zip) throws ArchiveException {
        File folder = new File(srcFile);
        if (folder.isDirectory()) {
            addFolderToZip(path, srcFile, zip);
        } else {
            // Transfer bytes from in to out
            byte[] buf = new byte[1024];
            int len;
            try (FileInputStream in = new FileInputStream(srcFile)) {
                if (path.trim().equals(Constants.EMPTY_STRING)) {
                    zip.putNextEntry(new ZipEntry(folder.getName()));
                } else {
                    zip.putNextEntry(new ZipEntry(path + "/" + folder.getName()));
                }
                while ((len = in.read(buf)) > 0) {
                    zip.write(buf, 0, len);
                }
            } catch (IOException ex) {
                throw new ArchiveException(Constants.ARCHIVE_EXCEPTION_MSG, ex);
            }
        }
    }

    /**
     * Add the given folder to the given location in the .car file.
     *
     * @param path:      file path in the .car file
     * @param srcFolder: folder to be included in the .car file
     * @param zip:       ZipOutputStream
     */
    private void addFolderToZip(String path, String srcFolder, ZipOutputStream zip) throws ArchiveException {
        File folder = new File(srcFolder);
        String[] fileList = folder.list();
        if (fileList == null) {
            return;
        }
        try {
            int i = 0;
            while (fileList.length != i) {
                String newPath = folder.getName();
                if (!path.equalsIgnoreCase(Constants.EMPTY_STRING)) {
                    newPath = path + "/" + newPath;
                }
                if (new File(folder, fileList[i]).isDirectory()) {
                    zip.putNextEntry(new ZipEntry(newPath + "/" + fileList[i] + "/"));
                }
                addToZip(newPath, srcFolder + "/" + fileList[i], zip);
                i++;
            }
        } catch (IOException ex) {
            throw new ArchiveException(Constants.ARCHIVE_EXCEPTION_MSG, ex);
        }
    }

    /**
     * Delete the file/folder in the given file path.
     *
     * @param file          directory to be deleted
     * @param fileExtension to exclude CAPP from deletion
     */
    private void recursiveDelete(File file, String fileExtension) {
        //to end the recursive loop
        if (!file.exists()) {
            return;
        }
        //if directory, go inside and call recursively
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null) {
                return;
            }
            for (File f : files) {
                //call recursively
                recursiveDelete(f, fileExtension);
            }
        } else if (file.getName().equals(getArchiveName().concat(fileExtension))) {
            return;
        }
        //call delete to delete files and empty directory
        file.delete();
    }

    private void resolveConnectorDependencies() throws MojoExecutionException {
        try {
            ConnectorDependencyResolver.resolveDependencies(this, project);
        } catch (Exception e) {
            getLog().error("Error occurred while resolving connector dependencies.", e);
            throw new MojoExecutionException("Connector dependency resolution failed.", e);
        }
    }

    private void resolveCAppDependencies(String tempTargetDir, List<ArtifactDependency> dependencies,
                                         List<ArtifactDependency> metaDependencies) throws MojoExecutionException {
        try {
            CAppDependencyResolver.resolveDependencies(this, project, tempTargetDir, dependencies, metaDependencies);
        } catch (Exception e) {
            getLog().error("Error occurred while resolving CApp dependencies.", e);
            throw new MojoExecutionException("CApp dependency resolution failed.", e);
        }
    }

    public MavenProject getProject() {

        return project;
    }
}
