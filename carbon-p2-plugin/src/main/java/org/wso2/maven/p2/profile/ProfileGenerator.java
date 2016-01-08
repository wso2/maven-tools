/*
 *  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.maven.p2.profile;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.wso2.maven.p2.commons.Generator;
import org.wso2.maven.p2.commons.P2ApplicationLaunchManager;
import org.wso2.maven.p2.exceptions.InvalidBeanDefinitionException;
import org.wso2.maven.p2.utils.FeatureUtils;
import org.wso2.maven.p2.utils.FileManagementUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * ProfileGenerator takes parameters from the pom.xml and generates the profile.
 */
public class ProfileGenerator extends Generator {

    private final ProfileResourceBundle resourceBundle;
    private final MavenProject project;
    private final String destination;
    private static final String PUBLISHER_APPLICATION = "org.eclipse.equinox.p2.director";
    private static final String DEFAULT_ENCODING = "UTF-8";

    /**
     * ProfileGenerator constructor taking ProfileResourceBundle as a parameter.
     *
     * @param resourceBundle ProfileResourceBundle
     */
    public ProfileGenerator(ProfileResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
        this.project = resourceBundle.getProject();
        this.destination = resourceBundle.getDestination();
    }

    @Override
    public void generate() throws MojoExecutionException, MojoFailureException {
        try {
            writeEclipseIni();
            installFeatures();
            updateProfileConfigIni();
            deleteOldProfiles();
        } catch (InvalidBeanDefinitionException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    /**
     * updating profile's config.ini p2.data.area property using relative path
     */
    private void updateProfileConfigIni() {
        File profileConfigIni = FileManagementUtil.getProfileConfigIniFile(destination, resourceBundle.getProfile());
        FileManagementUtil.changeConfigIniProperty(profileConfigIni, "eclipse.p2.data.area", "@config.dir/../../p2/");
    }

    /**
     * Calls the P2ApplicationLauncher and install the features.
     *
     * @throws InvalidBeanDefinitionException
     * @throws MojoFailureException
     */
    private void installFeatures() throws InvalidBeanDefinitionException, MojoFailureException {
        String installIUs = extractIUsToInstall();
        getLog().info("Running Equinox P2 Director Application");
        P2ApplicationLaunchManager launcher = new P2ApplicationLaunchManager(resourceBundle.getLauncher());
        launcher.setWorkingDirectory(project.getBasedir());
        launcher.setApplicationName(PUBLISHER_APPLICATION);
        launcher.addArgumentsToInstallFeatures(resourceBundle.getMetadataRepository().toExternalForm(),
                resourceBundle.getArtifactRepository().toExternalForm(), installIUs, destination,
                resourceBundle.getProfile());
        launcher.generateRepo(resourceBundle.getForkedProcessTimeoutInSeconds());
    }

    /**
     * Generate the formatted string representation of features from the features passed in through the pom.xml. This
     * formatted string is passed into P2ApplicationLauncher to generate the profile.
     *
     * @return formatted string to pass into P2ApplicationLauncher
     * @throws InvalidBeanDefinitionException
     */
    private String extractIUsToInstall() throws InvalidBeanDefinitionException {
        StringBuilder installIUs = new StringBuilder();
        for (Object featureObj : resourceBundle.getFeatures()) {
            Feature feature;
            if (featureObj instanceof Feature) {
                feature = (Feature) featureObj;
            } else if (featureObj instanceof String) {
                feature = FeatureUtils.getFeature(featureObj.toString());
            } else {
                throw new InvalidBeanDefinitionException("Unknown feature definition: " + featureObj.toString());
            }
            installIUs.append(feature.getId().trim()).append("/").append(feature.getVersion().trim()).append(",");
        }

        return installIUs.toString();
    }

    /**
     * Delete old profile files located at ${destination}/p2/org.eclipse.equinox.p2.engine/profileRegistry
     *
     * @throws MojoExecutionException
     */
    private void deleteOldProfiles() throws MojoExecutionException {
        //In not specified to delete, then return the method.
        if (!resourceBundle.isDeleteOldProfileFiles()) {
            return;
        }
        String destination = resourceBundle.getDestination();
        if (!destination.endsWith("/")) {
            destination = destination + "/";
            resourceBundle.setDestination(destination);
        }
        String profileFolderName = resourceBundle.getDestination() +
                "p2/org.eclipse.equinox.p2.engine/profileRegistry/" + resourceBundle.getProfile() + ".profile";

        File profileFolder = new File(profileFolderName);
        if (profileFolder.isDirectory()) {
            String[] profileFileList = profileFolder.list(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".profile");
                }
            });

            //deleting old profile files
            if (profileFileList != null) {
                for (int i = 0; i < (profileFileList.length - 1); i++) {
                    File profileFile = new File(profileFolderName, profileFileList[i]);
                    if (profileFile.exists() && !profileFile.delete()) {
                        throw new MojoExecutionException("Failed to delete old profile file: " +
                                profileFile.getAbsolutePath());
                    }
                }
            }

        }
    }

    /**
     * Truncate and write the null.ini file with the new profile location. If null.ini is not found, then truncate
     * and write new profile location in eclipse.ini.
     *
     * @throws MojoExecutionException
     */
    private void writeEclipseIni() throws MojoExecutionException {
        String profileLocation = resourceBundle.getDestination() + File.separator + resourceBundle.getProfile();

        File eclipseIni = new File(profileLocation + File.separator + "null.ini");
        if (!eclipseIni.exists()) {
            eclipseIni = new File(profileLocation + File.separator + "eclipse.ini");
        }
        if (eclipseIni.exists()) {
            rewriteFile(eclipseIni, profileLocation);
        }
    }

    private void rewriteFile(File file, String profileLocation) throws MojoExecutionException {
        if (file.exists()) {
            if (!file.delete()) {
                throw new MojoExecutionException("Failed to delete " + file.getAbsolutePath());
            }
        }

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), DEFAULT_ENCODING);
             PrintWriter pw = new PrintWriter(writer)){
            pw.write("-install\n");
            pw.write(profileLocation);
            pw.flush();
        } catch (IOException e) {
            this.getLog().debug("Error while writing to file " + file.getName());
            e.printStackTrace();
        }
    }
}
