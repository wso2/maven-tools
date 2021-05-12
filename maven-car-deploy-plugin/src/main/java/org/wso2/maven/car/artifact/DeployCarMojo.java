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

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.json.JSONObject;
import org.wso2.maven.car.artifact.impl.CAppMgtApiHelperServiceImpl;

import java.io.File;
import java.util.List;

import static org.wso2.maven.car.artifact.util.Constants.CONFIG_OPERATION_DEPLOY;
import static org.wso2.maven.car.artifact.util.Constants.CONFIG_OPERATION_UNDEPLOY;

/**
 * Deploy the generated CAR file to a Remote Server
 *
 * @goal deploy-car
 * @phase deploy
 * @description Deploy CAR Artifact
 */
public class DeployCarMojo extends AbstractMojo {
	
	private static final String EXTENSION_CAR = ".car";

	/**
     * Location Trust Store folder
     * @required
     * @parameter expression="${trustStorePath}" default-value="${basedir}/src/main/resources/security/wso2carbon.jks"
     */
	private String trustStorePath;
	
	/**
     * Location Trust Store folder
     * @required
     * @parameter expression="${trustStorePassword}" default-value="wso2carbon"
     */
	private String trustStorePassword;
	
	/**
     * Type of Trust Store
     * @required
     * @parameter expression="${trustStoreType}" default-value="JKS"
     */	
	private String trustStoreType;
	
	/**
     * Server URL
     * @required
     * @parameter expression="${serverUrl}" default-value="https://localhost:9443"
     */	
	private String serverUrl;
	
	/**
     * Server URL
     * @required
     * @parameter expression="${userName}" default-value="admin"
     */	
	private String userName;
	
	
	/**
     * Server URL
     * @required
     * @parameter expression="${password}" default-value="admin"
     */	
	private String password;
	
	/**
     * Location target folder
     *
     * @parameter expression="${project.build.directory}"
     */
    private File target;
    
    /**
     * Location archiveLocation folder
     *
     * @parameter
     */
    private File archiveLocation;
    
	/**
	 * finalName to use for the generated capp project if the user wants to override the default name
	 * 
	 * @parameter
	 */
	public String finalName;
    

	/**
	 * @parameter default-value="${project}"
	 */
	private MavenProject project;

	/**
	 * Maven ProjectHelper.
	 * 
	 * @component
	 */
	private MavenProjectHelper projectHelper;

    /**
     * @component
     */
    private ArtifactFactory artifactFactory;

    /**
     * @component
     */
    private ArtifactResolver resolver;

    /**
     * @parameter default-value="${localRepository}"
     */
    private ArtifactRepository localRepository;

    /**
     * @parameter default-value="${project.remoteArtifactRepositories}"
     */
    private List<?> remoteRepositories;
    
    /**
     * @parameter expression="${operation}" default-value="deploy"
     */
    private String operation;
    
    /**
	 * Maven ProjectHelper.
	 * 
	 * @parameter
	 */
    private List<CarbonServer> carbonServers;
    
	/**
	 * Set this to 'false' to enable C-App artifact deploy
	 * 
	 * @parameter expression="${maven.car.deploy.skip}" default-value="true"
	 */
    private boolean skip;

    private final CAppMgtApiHelperServiceImpl capppMgtApiHelperServiceImpl;

    public DeployCarMojo() {
        capppMgtApiHelperServiceImpl = new CAppMgtApiHelperServiceImpl();
    }
    

	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skip) {
			getLog().info("Skipping CAR artifact deployment to Carbon Server(s).");
			return;
		}
		
		if(carbonServers == null) {
            getLog().info("Could not find <carbonServers> element in the pom.xml. " +
                    "Hence proceeding with default values");
			process();
		} else if(carbonServers != null && carbonServers.isEmpty()){
            getLog().info("Could not find properties under <carbonServer> element in the pom.xml. " +
                    "Hence proceeding with default values");
			process();
		} else{
			for (CarbonServer server : carbonServers) {
				getLog().info("Deploying to Server...");
				getLog().info("TSPath=" + server.getTrustStorePath());
				if(server.getTrustStorePath() != null){
					trustStorePath = server.getTrustStorePath();
				}
				if(server.getTrustStorePassword() != null){
					trustStorePassword = server.getTrustStorePassword();
				}
				getLog().info("TSType=" + server.getTrustStoreType());
				if(server.getTrustStoreType()!=null){
					trustStoreType = server.getTrustStoreType();
				}
				getLog().info("Server URL=" + server.getServerUrl());
				if(server.getServerUrl() != null){
					serverUrl = server.getServerUrl();
				}
				if(server.getUserName() != null){
					userName = server.getUserName();
				}
				if(server.getPassword() != null){
					password = server.getPassword();
				}
				getLog().info("Operation=" + server.getOperation());

				if(server.getOperation() != null){
					operation = server.getOperation();
				}
				process();
			}
		}
    }

	private void process() throws MojoExecutionException {
		setSystemProperties();
		
		List<Plugin> buildPlugins = project.getBuildPlugins();
		
		for (Plugin plugin : buildPlugins) {
			String artifactId = plugin.getArtifactId();
			if(artifactId.equals("maven-car-plugin")){
				Xpp3Dom configurationNode = (Xpp3Dom)plugin.getConfiguration();
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
		
	    if(operation.equalsIgnoreCase(CONFIG_OPERATION_DEPLOY)){
		    try {
				deployCApp(userName, password, serverUrl, carFile);
			} catch (Exception e) {
				getLog().error("Uploading " + carFile.getName() + " to " + serverUrl + " Failed.", e);
				throw new MojoExecutionException("Deploying " + carFile.getName() + " to " + serverUrl + " Failed.", e);
			}
	    } else if (operation.equalsIgnoreCase(CONFIG_OPERATION_UNDEPLOY)) {
	    	 try {
	 			unDeployCAR(userName, password, serverUrl);
	 		} catch (Exception e) {
	 			getLog().error("Deleting " + carFile.getName() + " to " + serverUrl + " Failed.", e);
	 			throw new MojoExecutionException("Deleting " + carFile.getName() + " to " + serverUrl + " Failed.", e);
	 		}
	    } else {
            throw new MojoExecutionException("Unsupported operation. Only allows \"deploy\" or \"undeploy\" ");
        }
	}
	
	@SuppressWarnings("unused")
	private void printParams(){
		if(!carbonServers.isEmpty()){
			for (CarbonServer server : carbonServers) {
				getLog().info("Server:");
				getLog().info("TSPath="+server.getTrustStorePath());
				getLog().info("TSPWD="+server.getTrustStorePassword());
				getLog().info("TSType="+server.getTrustStoreType());
				getLog().info("Server URL="+server.getServerUrl());
				getLog().info("Operation="+server.getOperation());
				
				if(server.getUserName()==null){
					getLog().info("Please enter a valid user name.");
				}
			}
		}
	}
	
	private void setSystemProperties(){
		System.setProperty("javax.net.ssl.trustStore", trustStorePath);
		System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
		System.setProperty("javax.net.ssl.trustStoreType", trustStoreType);
	}
	
	public void deployCApp(String username, String password, String serverUrl, File carFile) throws Exception{
        JSONObject resObj = capppMgtApiHelperServiceImpl.doAuthenticate(serverUrl, username, password);
        if (resObj != null) {
            getLog().info("Authentication to " + serverUrl + " successful.");
            String accessToken = resObj.getString("AccessToken");
            getLog().info("AccessToken is : " + accessToken);
            if (accessToken != null && !accessToken.equals("")) {
                if (capppMgtApiHelperServiceImpl.deployCApp(carFile, accessToken, serverUrl)) {
                    getLog().info("Uploaded " + carFile.getName()+ " to " + serverUrl+ " ...");
                }
            }
        }
	}

	public void unDeployCAR(String username, String password,String serverUrl) throws Exception {
        JSONObject resObj = capppMgtApiHelperServiceImpl.doAuthenticate(serverUrl, username, password);
        if (resObj != null) {
            getLog().info("Authentication to " + serverUrl + " successful.");
            String accessToken = resObj.getString("AccessToken");
            if (accessToken != null && !accessToken.equals("")) {
                if (capppMgtApiHelperServiceImpl.unDeployCApp(accessToken, serverUrl,
                        project.getArtifactId() + "_" + project.getVersion())) {
                    getLog().info("Located the C-App " + project.getArtifactId() +
                            "_" + project.getVersion() + " and undeployed ...");
                }
            }
        }
	}
}
