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
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.wso2.maven.Model.ArchiveException;
import org.wso2.maven.Model.ArtifactDependency;


/**
 * Goal which touches a timestamp file.
 *
 * @goal car
 * @phase package
 */
@Mojo(name = "WSO2ESBDeployableArchive")
public class CARMojo extends AbstractMojo {

    /**
     * The Maven Project Object
     *
     * @parameter expression="${project}"
     * @required
     */
    MavenProject project;

    @Parameter(defaultValue = "${project.baseDir}", property = "archiveLocation")
    String archiveLocation;

    /**
     * The name of the archive file
     *
     * @parameter expression="${archiveName}"
     */
    String archiveName;

    public void logError(String message) {
        getLog().error(message);
    }

    public void logInfo(String message) {
        getLog().info(message);
    }

    public String getArchiveName() {
        return archiveName == null ? project.getArtifactId() + "CompositeExporter_" + project.getVersion() : archiveName;
    }

    public void execute() {
        appendLogs();
        String basedir = project.getBasedir().toString();
        archiveLocation = StringUtils.isEmpty(archiveLocation) ? basedir + File.separator +
                Constants.DEFAULT_TARGET_FOLDER : archiveLocation;
        String artifactFolderPath = basedir + File.separator + Constants.ARTIFACTS_FOLDER_PATH;
        String resourcesFolderPath = basedir + File.separator + Constants.RESOURCES_FOLDER_PATH;

        File artifactFolder = new File(artifactFolderPath);
        File resourcesFolder = new File(resourcesFolderPath);
        if (!artifactFolder.exists() || !resourcesFolder.exists()) {
            getLog().error("Invalid project structure");
            return;
        }
        boolean createdArchiveDirectory = org.wso2.developerstudio.eclipse.utils.file.FileUtils.createDirectories(
                archiveLocation);
        CAppHandler cAppHandler = new CAppHandler(getArchiveName(), this);
        List<ArtifactDependency> dependencies = new ArrayList<>();
        if (createdArchiveDirectory) {
            cAppHandler.processArtifacts(artifactFolder, archiveLocation, dependencies, project.getVersion());
            cAppHandler.processResourcesFolder(resourcesFolder, archiveLocation, dependencies);
            cAppHandler.createDependencyArtifactsXmlFile(archiveLocation, dependencies, project);
            File fileToZip = new File(archiveLocation);
            String fileExtension = ".car";
            File carFile = getArchiveFile(fileExtension);
            try {
                zipFolder(fileToZip.getPath(), carFile.getPath(), fileExtension);
            } catch (ArchiveException e) {
                logError("Error occurred while creating the .car file");
            }
            // Attach carFile to Maven context.
            this.project.getArtifact().setFile(carFile);
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
        File archiveFile;
        archiveFile = new File(archiveLocation, getArchiveName() + fileExtension);
        return archiveFile;
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
}
