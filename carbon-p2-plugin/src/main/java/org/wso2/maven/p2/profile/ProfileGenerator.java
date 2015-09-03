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
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.tycho.p2.facade.internal.P2ApplicationLauncher;
import org.wso2.maven.p2.commons.Generator;
import org.wso2.maven.p2.utils.FeatureUtils;
import org.wso2.maven.p2.utils.FileManagementUtil;
import org.wso2.maven.p2.utils.P2Constants;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Date;

public class ProfileGenerator extends Generator {


    private File FOLDER_TARGET;
    private File FOLDER_TEMP;
    private File FOLDER_TEMP_REPO_GEN;
    private File FILE_FEATURE_PROFILE;
    private File p2AgentDir;

    private final String STREAM_TYPE_IN = "inputStream";
    private final String STREAM_TYPE_ERROR = "errorStream";

    private final ProfileResourceBundle resourceBundle;
    private final MavenProject project;
    private final String destination;

    public ProfileGenerator(ProfileResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
        this.project = resourceBundle.getProject();
        this.destination = resourceBundle.getDestination();
    }

    @Override
    public void generate() throws MojoExecutionException, MojoFailureException {
        try {
            if (resourceBundle.getProfile() == null) {
                resourceBundle.setProfile(P2Constants.DEFAULT_PROFILE_ID);
            }
            createAndSetupPaths();
            rewriteEclipseIni();
//          	verifySetupP2RepositoryURL();
            this.getLog().info("Running Equinox P2 Director Application");
            installFeatures(getIUsToInstall());
            //updating profile's config.ini p2.data.area property using relative path
            File profileConfigIni = FileManagementUtil.getProfileConfigIniFile(destination, resourceBundle.getProfile());
            FileManagementUtil.changeConfigIniProperty(profileConfigIni, "eclipse.p2.data.area", "@config.dir/../../p2/");

            //deleting old profile files, if specified
            if (resourceBundle.isDeleteOldProfileFiles()) {
                deleteOldProfiles();
            }
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
//        createArchive();
//        deployArtifact();
        performMopUp();
    }

    private String getIUsToInstall() throws MojoExecutionException {
        String installUIs = "";
        for (Object featureObj : resourceBundle.getFeatures()) {
            Feature f;
            if (featureObj instanceof Feature) {
                f = (Feature) featureObj;
            } else if (featureObj instanceof String) {
                f = FeatureUtils.getFeature(featureObj.toString());
            } else
                f = (Feature) featureObj;
            installUIs = installUIs + f.getId().trim() + "/" + f.getVersion().trim() + ",";
        }

        if (installUIs.length() == 0) {
            installUIs = installUIs.substring(0, installUIs.length() - 1);
        }
        return installUIs;
    }

    private String getPublisherApplication() {
        return "org.eclipse.equinox.p2.director";
    }

    private void installFeatures(String installUIs) throws Exception {
        P2ApplicationLauncher launcher = resourceBundle.getLauncher();
        launcher.setWorkingDirectory(project.getBasedir());
        launcher.setApplicationName(getPublisherApplication());
        addArguments(launcher, installUIs);
        int result = launcher.execute(resourceBundle.getForkedProcessTimeoutInSeconds());
        if (result != 0) {
            throw new MojoFailureException("P2 publisher return code was " + result);
        }
    }

    private void addArguments(P2ApplicationLauncher launcher, String installUIs) throws IOException {
        launcher.addArguments(
                "-metadataRepository", resourceBundle.getMetadataRepository().toExternalForm(),
                "-artifactRepository", resourceBundle.getArtifactRepository().toExternalForm(),
                "-profileProperties", "org.eclipse.update.install.features=true",
                "-installIU", installUIs,
                "-bundlepool", destination,
                //to support shared installation in carbon
                "-shared", destination + File.separator + "p2",
                //target is set to a separate directory per Profile
                "-destination", destination + File.separator + resourceBundle.getProfile(),
                "-profile", resourceBundle.getProfile(),
                "-roaming"
        );
    }

//    public class InputStreamHandler implements Runnable {
//        String streamType;
//        InputStream inputStream;
//
//        public InputStreamHandler(String name, InputStream is) {
//            this.streamType = name;
//            this.inputStream = is;
//        }
//
//        public void start() {
//            Thread thread = new Thread(this);
//            thread.start();
//        }
//
//        public void run() {
//            try {
//                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
//                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
//
//                while (true) {
//                    String s = bufferedReader.readLine();
//                    if (s == null) break;
//                    if (STREAM_TYPE_IN.equals(streamType)) {
//                        getLog().info(s);
//                    } else if (STREAM_TYPE_ERROR.equals(streamType)) {
//                        getLog().error(s);
//                    }
//                }
//                inputStream.close();
//            } catch (Exception ex) {
//                getLog().error("Problem reading the " + streamType + ".", ex);
//            }
//        }
//
//    }


    private void createAndSetupPaths() throws Exception {
        FOLDER_TARGET = new File(project.getBasedir(), "target");
        String timestampVal = String.valueOf((new Date()).getTime());
        FOLDER_TEMP = new File(FOLDER_TARGET, "tmp." + timestampVal);
        FOLDER_TEMP_REPO_GEN = new File(FOLDER_TEMP, "temp_repo");
        FILE_FEATURE_PROFILE = new File(FOLDER_TARGET, project.getArtifactId() + "-" + project.getVersion() + ".zip");


    }

    private void deleteOldProfiles() {
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

            Arrays.sort(profileFileList);

            //deleting old profile files
            for (int i = 0; i < (profileFileList.length - 1); i++) {
                File profileFile = new File(profileFolderName, profileFileList[i]);
                profileFile.delete();
            }
        }
    }

    private void rewriteEclipseIni() {
        File eclipseIni = null;
        String profileLocation = resourceBundle.getDestination() + File.separator + resourceBundle.getProfile();
        // getting the file null.ini
        eclipseIni = new File(profileLocation + File.separator + "null.ini");
        if (eclipseIni.exists()) {
            rewriteFile(eclipseIni, profileLocation);
            return;
        }
        // null.ini does not exist. trying with eclipse.ini
        eclipseIni = new File(profileLocation + File.separator + "eclipse.ini");
        if (eclipseIni.exists()) {
            rewriteFile(eclipseIni, profileLocation);
            return;
        }
    }

    private void rewriteFile(File file, String profileLocation) {
        file.delete();
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new FileWriter(file));
            pw.write("-install\n");
            pw.write(profileLocation);
            pw.flush();
        } catch (IOException e) {
            this.getLog().debug("Error while writing to file " + file.getName());
            e.printStackTrace();
        } finally {
            pw.close();
        }
    }

    private void deployArtifact() {
        if (FILE_FEATURE_PROFILE != null && FILE_FEATURE_PROFILE.exists()) {
            project.getArtifact().setFile(FILE_FEATURE_PROFILE);
            resourceBundle.getProjectHelper().attachArtifact(project, "zip", null, FILE_FEATURE_PROFILE);
        }
    }

    private void performMopUp() {
        try {
            FileUtils.deleteDirectory(FOLDER_TEMP);
        } catch (Exception e) {
            getLog().warn(new MojoExecutionException("Unable complete mop up operation", e));
        }
    }
}
