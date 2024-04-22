/*
 * Copyright (c) 2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.maven.car.artifact;

import static org.wso2.maven.car.artifact.util.Constants.CONFIG_OPERATION_DEPLOY;
import static org.wso2.maven.car.artifact.util.Constants.CONFIG_OPERATION_UNDEPLOY;
import static org.wso2.maven.car.artifact.util.Constants.EI_SERVER;
import static org.wso2.maven.car.artifact.util.Constants.MI_SERVER;

import java.io.File;
import java.util.List;

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Deploy the generated CAR file to a Remote Server
 */
@Mojo(name = "deploy-car", defaultPhase = LifecyclePhase.DEPLOY)
public class DeployCarMojo extends AbstractMojo {

    private static final String EXTENSION_CAR = ".car";

    /**
     * Location Trust Store folder
     */
    @Parameter(property = "trustStorePath", defaultValue = "${basedir}/src/main/resources/security/wso2carbon.jks", required = true)
    private String trustStorePath;

    /**
     * Location Trust Store folder
     */
    @Parameter(property = "trustStorePath", defaultValue = "${basedir}/src/main/resources/security/wso2carbon.jks", required = true)
    private String trustStorePassword;

    /**
     * Type of Trust Store
     */
    @Parameter(property = "trustStoreType", defaultValue = "JKS", required = true)
    private String trustStoreType;

    /**
     * Server URL
     */
    @Parameter(property = "serverUrl", defaultValue = "https://localhost:9443")
    private String serverUrl;

    /**
     * Server URL
     */
    @Parameter(property = "username", defaultValue = "admin", required = true)
    private String userName;

    /**
     * Server URL
     */
    @Parameter(property = "password", defaultValue = "admin", required = true)
    private String password;

    /**
     * Location target folder
     */
    @Parameter(property = "project.build.directory")
    private File target;

    /**
     * Location archiveLocation folder
     */
    @Parameter
    private File archiveLocation;

    /**
     * finalName to use for the generated capp project if the user wants to override the default name
     */
    @Parameter
    public String finalName;

    @Parameter(defaultValue = "${project}")
    private MavenProject project;

    /**
     * Maven ProjectHelper.
     */
    @Component
    private MavenProjectHelper projectHelper;

    @Parameter(defaultValue = "${project.remoteArtifactRepositories}")
    private List<RemoteRepository> remoteRepositories;

    @Parameter(property = "operation", defaultValue = "deploy")
    private String operation;

    @Parameter(property = "serverType", defaultValue = "mi")
    private String serverType;

    /**
     * Maven ProjectHelper.
     */
    @Parameter
    private List<CarbonServer> carbonServers;

    /**
     * Set this to 'false' to enable C-App artifact deploy
     */
    @Parameter(property = "maven.car.deploy.skip", defaultValue = "true")
    private boolean skip;

    private CAppHandler cAppHandler;

    public void execute() throws MojoExecutionException, MojoFailureException {

        if (skip) {
            getLog().info("Skipping CAR artifact deployment to Carbon Server(s).");
            return;
        }

        if (carbonServers == null) {
            getLog().info("Could not find <carbonServers> element in the pom.xml. " +
                    "Hence proceeding with default values");
            process();
        } else if (carbonServers != null && carbonServers.isEmpty()) {
            getLog().info("Could not find properties under <carbonServer> element in the pom.xml. " +
                    "Hence proceeding with default values");
            process();
        } else {
            for (CarbonServer server : carbonServers) {
                getLog().info("Deploying to Server...");
                getLog().info("TSPath=" + server.getTrustStorePath());
                if (server.getTrustStorePath() != null) {
                    trustStorePath = server.getTrustStorePath();
                }
                if (server.getTrustStorePassword() != null) {
                    trustStorePassword = server.getTrustStorePassword();
                }
                getLog().info("TSType=" + server.getTrustStoreType());
                if (server.getTrustStoreType() != null) {
                    trustStoreType = server.getTrustStoreType();
                }
                getLog().info("Server URL=" + server.getServerUrl());
                if (server.getServerUrl() != null) {
                    serverUrl = server.getServerUrl();
                }
                if (server.getUserName() != null) {
                    userName = server.getUserName();
                }
                if (server.getPassword() != null) {
                    password = server.getPassword();
                }
                getLog().info("Operation=" + server.getOperation());

                if (server.getOperation() != null) {
                    operation = server.getOperation();
                }
                if (server.getServerType() != null) {
                    serverType = server.getServerType();
                }
                getLog().info("ServerType=" + serverType);
                process();
            }
        }
    }

    private void process() throws MojoExecutionException {

        setSystemProperties();

        List<Plugin> buildPlugins = project.getBuildPlugins();

        for (Plugin plugin : buildPlugins) {
            String artifactId = plugin.getArtifactId();
            if (artifactId.equals("car-maven-plugin")) {
                Xpp3Dom configurationNode = (Xpp3Dom) plugin.getConfiguration();
                Xpp3Dom finalNameNode = configurationNode.getChild("finalName");
                if (finalNameNode != null) {
                    finalName = finalNameNode.getValue();
                    getLog().info("Final Name of C-App: " + finalName + EXTENSION_CAR);
                }
                break;
            }
        }

        File carFile = null;
        if (null != archiveLocation) { // If default target location is changed by user
            if (archiveLocation.isFile() && archiveLocation.getName().endsWith(EXTENSION_CAR)) {
                carFile = archiveLocation;
            } else {
                throw new MojoExecutionException("Archive location is not a valid file");
            }
        } else { // Default target file
            if (finalName == null) {
                carFile = new File(target + File.separator + project.getArtifactId() + "_" +
                        project.getVersion() + EXTENSION_CAR);
            } else {
                carFile = new File(target + File.separator + finalName + EXTENSION_CAR);
            }
        }

        if (isValidServerType()) {
            cAppHandler = getCAppHandler();
        } else {
            throw new MojoExecutionException("Unsupported serverType. Only allows \"mi\" or \"ei\" ");
        }

        if (operation.equalsIgnoreCase(CONFIG_OPERATION_DEPLOY)) {
            try {
                cAppHandler.deployCApp(userName, password, serverUrl, carFile);
            } catch (Exception e) {
                getLog().error("Uploading " + carFile.getName() + " to " + serverUrl + " Failed.", e);
                throw new MojoExecutionException("Deploying " + carFile.getName() + " to " + serverUrl + " Failed.", e);
            }
        } else if (operation.equalsIgnoreCase(CONFIG_OPERATION_UNDEPLOY)) {
            try {
                cAppHandler.unDeployCApp(userName, password, serverUrl, project);
            } catch (Exception e) {
                getLog().error("Deleting " + carFile.getName() + " to " + serverUrl + " Failed.", e);
                throw new MojoExecutionException("Deleting " + carFile.getName() + " to " + serverUrl + " Failed.", e);
            }
        } else {
            throw new MojoExecutionException("Unsupported operation. Only allows \"deploy\" or \"undeploy\" ");
        }
    }

    @SuppressWarnings("unused")
    private void printParams() {

        if (!carbonServers.isEmpty()) {
            for (CarbonServer server : carbonServers) {
                getLog().info("Server:");
                getLog().info("TSPath=" + server.getTrustStorePath());
                getLog().info("TSPWD=" + server.getTrustStorePassword());
                getLog().info("TSType=" + server.getTrustStoreType());
                getLog().info("Server URL=" + server.getServerUrl());
                getLog().info("Operation=" + server.getOperation());

                if (server.getUserName() == null) {
                    getLog().info("Please enter a valid user name.");
                }
            }
        }
    }

    private void setSystemProperties() {

        System.setProperty("javax.net.ssl.trustStore", trustStorePath);
        System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
        System.setProperty("javax.net.ssl.trustStoreType", trustStoreType);
    }

    private boolean isValidServerType() {

        return serverType.equalsIgnoreCase(MI_SERVER) || serverType.equalsIgnoreCase(EI_SERVER);
    }

    private CAppHandler getCAppHandler() {

        if (serverType.equalsIgnoreCase(EI_SERVER)) {
            return new EICAppHandler(getLog());
        }
        return new MICAppHandler(getLog());
    }
}
