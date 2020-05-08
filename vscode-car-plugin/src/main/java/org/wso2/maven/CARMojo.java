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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.xml.stream.XMLStreamException;

import com.google.inject.internal.util.Lists;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.codehaus.plexus.component.annotations.Requirement;
import org.wso2.maven.Model.ArchiveException;
import org.wso2.maven.Model.ArtifactDependency;

import static org.wso2.maven.CAppHandler.REGISTRY_RESOURCES_FOLDER;
import static org.wso2.maven.CAppHandler.SYNAPSE_CONFIG_FOLDER;


/**
 * Goal which touches a timestamp file.
 *
 * @goal car
 * @phase package
 */
@Mojo(name = "WSO2ESBDeployableArchive")
public class CARMojo extends AbstractMojo {
    private static final List<String> EMPTY_LIST = Collections.unmodifiableList(Lists.<String>newArrayList());

    /**
     * Location archiveLocation folder.
     *
     * @parameter expression="${project.basedir}"
     */
    private File projectBaseDir;

    /**
     * Location archiveLocation folder.
     *
     * @parameter expression="${project.build.directory}"
     */
    private File archiveLocation;

    /**
     * archiveName is used if the user wants to override the default name of the generated archive with .car extension.
     *
     * @parameter expression="${project.build.archiveName}"
     */
    private String archiveName;

    /**
     * CarbonApp is used if the user wants to override the default name of the carbon application.
     *
     * @parameter
     */
    private String cAppName;

    /**
     * @parameter default-value="${project}"
     */
    private MavenProject project;

    /**
     * A classifier for the build final name.
     *
     * @parameter
     */
    private String classifier;

    /**
     * Source encoding for this project.
     *
     * @parameter expression="${project.build.sourceEncoding}"
     */
    private String sourceEncoding;

    /**
     * the current maven session instance.
     *
     * @parameter expression="${session}"
     */
    protected MavenSession session;

    /**
     * a filtering capability is needed to filter project-resources
     */
    @Component
    private MavenResourcesFiltering mavenResourcesFiltering;

    /**
     * Read artifact.xml files and create .car file.
     */
    public void execute() throws MojoExecutionException {
        appendLogs();
        // Create CApp
        try {
            // Create directory to be compressed.
            String archiveWorkDirectory = getArchiveFile(Constants.EMPTY_STRING).getAbsolutePath();
            boolean createdArchiveDirectory = org.wso2.developerstudio.eclipse.utils.file.FileUtils.createDirectories(archiveWorkDirectory);

            if (createdArchiveDirectory) {
                CAppHandler cAppHandler = new CAppHandler(cAppName);
                List<ArtifactDependency> dependencies = new ArrayList<>();

                // filter synapse-config files into the archiveWorkDirectory
                filterResources(Paths.get(projectBaseDir.getAbsolutePath(), SYNAPSE_CONFIG_FOLDER), Paths.get(archiveWorkDirectory, SYNAPSE_CONFIG_FOLDER).toFile());
                // filter registry-resources files into the archiveWorkDirectory
                filterResources(Paths.get(projectBaseDir.getAbsolutePath(), REGISTRY_RESOURCES_FOLDER), Paths.get(archiveWorkDirectory, REGISTRY_RESOURCES_FOLDER).toFile());

                // Make sure we continue our work using the archiveWorkDirectory
                File filteredProjectBaseDir = Paths.get(archiveWorkDirectory).toFile();

                String cappArchiveDirectory = Paths.get(this.project.getBuild().getOutputDirectory(), "capp").toString();
                cAppHandler.processConfigArtifactXmlFile(filteredProjectBaseDir, cappArchiveDirectory, dependencies);
                cAppHandler.processRegistryResourceArtifactXmlFile(filteredProjectBaseDir, cappArchiveDirectory, dependencies);
                cAppHandler.createDependencyArtifactsXmlFile(cappArchiveDirectory, dependencies, project);

                File fileToZip = new File(cappArchiveDirectory);
                File carFile = getArchiveFile(".car");
                zipFolder(fileToZip.getPath(), carFile.getPath());

                // Attach carFile to Maven context.
                this.project.getArtifact().setFile(carFile);

                // Delete the filtered files folder
                recursiveDelete(fileToZip);
                // Delete temp archive folder
                recursiveDelete(filteredProjectBaseDir);

            } else {
                getLog().error("Could not create corresponding archive directory.");
            }
        } catch (XMLStreamException | IOException | ArchiveException e) {
            getLog().error(e);
        }
    }

    /**
     * Filter the resources in given sourcePath, output the results to the outputFolder
     * @param sourcePath folder containing resources
     * @param outputFolder location where filtered resources will be stored
     * @throws MojoExecutionException if filtering throws an exception its wrapped in this MojoExecutionException for clean handling by Maven.
     */
    private void filterResources(Path sourcePath, File outputFolder) throws MojoExecutionException {
        Resource srcResource = new Resource();
        srcResource.setDirectory(sourcePath.toString());
        srcResource.setFiltering(true);

        MavenResourcesExecution execution = new MavenResourcesExecution(Collections.singletonList(srcResource), outputFolder, this.project, sourceEncoding, EMPTY_LIST, EMPTY_LIST, session);
        execution.setUseDefaultFilterWrappers(true);

        try {
            mavenResourcesFiltering.filterResources(execution);
        } catch (MavenFilteringException e) {
            throw new MojoExecutionException("Error while filtering project resources", e);
        }
    }

    /**
     * Append log of BUILD process when building started.
     */
    private void appendLogs() {
        getLog().info("------------------------------------------------------------------------");
        getLog().info("Building Synapse Config Project");
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
        if (archiveName != null && !archiveName.trim().equals(Constants.EMPTY_STRING)) {
            archiveFile = new File(archiveLocation, archiveName + fileExtension);
            return archiveFile;
        }
        String archiveFilename = project.getArtifactId() + "_" + project.getVersion() +
                (classifier != null ? "-" + classifier : Constants.EMPTY_STRING) + fileExtension;
        archiveFile = new File(archiveLocation, archiveFilename);

        return archiveFile;
    }

    /**
     * Zip a folder.
     *
     * @param srcFolder:   file path of the archive directory
     * @param destZipFile: file path of the .car file
     */
    private void zipFolder(String srcFolder, String destZipFile) throws ArchiveException {
        try (FileOutputStream fileWriter = new FileOutputStream(destZipFile);
             ZipOutputStream zip = new ZipOutputStream(fileWriter)) {

            addFolderContentsToZip(srcFolder, zip);
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
    private void addFolderContentsToZip(String srcFolder, ZipOutputStream zip) throws ArchiveException {
        File folder = new File(srcFolder);
        String[] fileList = folder.list();
        if (fileList == null) {
            return;
        }
        try {
            int i = 0;
            while (true) {
                if (fileList.length == i) break;
                if (new File(folder, fileList[i]).isDirectory()) {
                    zip.putNextEntry(new ZipEntry(fileList[i] + "/"));
                    zip.closeEntry();
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
            while (true) {
                if (fileList.length == i) break;
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
     * @param file directory to be deleted
     */
    private void recursiveDelete(File file) {
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
                recursiveDelete(f);
            }
        }
        //call delete to delete files and empty directory
        file.delete();
    }
}
