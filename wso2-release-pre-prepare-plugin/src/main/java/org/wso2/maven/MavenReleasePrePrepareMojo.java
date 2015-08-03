/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
package org.wso2.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import javax.xml.stream.FactoryConfigurationError;

import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFile;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.checkout.CheckOutScmResult;
import org.apache.maven.scm.manager.BasicScmManager;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.provider.git.gitexe.GitExeScmProvider;
import org.apache.maven.scm.provider.svn.svnexe.SvnExeScmProvider;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;
import org.wso2.maven.esb.ESBArtifact;
import org.wso2.maven.esb.ESBProjectArtifact;
import org.wso2.maven.registry.GeneralProjectArtifact;
import org.wso2.maven.registry.RegistryArtifact;

/**
 * This is the Maven Mojo used for do pre prepare tasks such as update version
 * of artifacts in Artifact.xml etc.
 * 
 * @goal pre-prepare
 * 
 */
public class MavenReleasePrePrepareMojo extends AbstractMojo {

	private static final String PROJECT_NATURES = "projectnatures";
	private static final String MAVEN_ECLIPSE_PLUGIN = "maven-eclipse-plugin";
	private static final String POM_XML = "pom.xml";
	private static final String ORG_WSO2_DEVELOPER_STUDIO_ECLIPSE_GENERAL_PROJECT_NATURE =
	                                                                                      "org.wso2.developerstudio.eclipse.general.project.nature";
	private static final String ORG_WSO2_DEVELOPER_STUDIO_ECLIPSE_ESB_PROJECT_NATURE =
	                                                                                  "org.wso2.developerstudio.eclipse.esb.project.nature";
	private static final String PROJECT = "project.";
	private static final String ARTIFACT_XML_REGEX = "**/artifact.xml";
	private static final String DEVELOPMENT = "dev";
	private static final String RELEASE = "rel";
	private static final String SCM = "scm:";
	private static final String SCM_URL = "scm.url";
	private static final String SCM_TAG_BASE = "scm.tagBase";
	private static final String SCM_TAG = "scm.tag";
	private static final String SCM_USERNAME = "scm.username";
	private static final String SCM_PASSWORD = "scm.password";
	private static final String POM = "pom";
	private static final String RELEASE_PROPERTIES = "release.properties";
	private static final String ARTIFACT_XML = "artifact.xml";
	private static final String SVN = "svn";
	private static final String GIT = "git";
	public static final String TRUNK_SUFFIX = "/trunk";
	public static final String TAGS_SUFFIX = "/tags";
	public static final String SCM_SVN_PREFIX = "scm:svn:";

	private final Log log = getLog();

	/**
	 * @parameter default-value="${project}"
	 */
	private MavenProject project;
	
	/** 
	 * @component role-hint="mojo" 
	 */
    private SecDispatcher secDispatcher;

	/**
	 * @parameter expression="${dryRun}" default-value="false"
	 */
	private boolean dryRun;

	/**
	 * Field to hold the name of scm provider which is derived using scm URL
	 */
	private String scmProvider;

	public void execute() throws MojoExecutionException, MojoFailureException {

		if (dryRun) {
			log.warn(" **** wso2-release-pre-prepare-plugin does not support the dryRun mode **** ");
			return;
		}

		Properties prop = new Properties();
		String baseDirPath = project.getBasedir().getPath();
		File artifactXml = new File(baseDirPath, ARTIFACT_XML);
		String packagingType = project.getPackaging();

		if (!artifactXml.exists() && packagingType.equals(POM)) {
			InputStream input = null;
			try {
				input = new FileInputStream(new File(baseDirPath, RELEASE_PROPERTIES));
				prop.load(input);
				String scmUrl = prop.getProperty(SCM_URL);
				String scmTag = prop.getProperty(SCM_TAG);
				String scmTagBase = prop.getProperty(SCM_TAG_BASE);
				String scmUserName = prop.getProperty(SCM_USERNAME);
				String scmPassword = decrypt(prop.getProperty(SCM_PASSWORD));
				if (scmUrl == null) {
					scmUrl = project.getScm().getConnection();
				}
				// get ScmManager
				ScmManager scmManager = getScmManager();
				// derive scm Provider
				scmProvider = scmUrl.split(":")[1];
				if(scmTagBase == null && SVN.equals(scmProvider)){
					scmTagBase = getDefaultSVNTagBase(scmUrl);
				}
				String checkoutUrl = SCM + scmProvider + ":" +
				                       scmTagBase.replaceAll("/$", "").concat("/").concat(scmTag);
				// checkout and commit tag
				checkoutAndCommit(scmManager, prop, checkoutUrl, scmUserName, scmPassword, RELEASE);
				scmTagBase = project.getScm().getConnection();
				// checkout and commit trunk
				checkoutAndCommit(scmManager, prop, scmTagBase, scmUserName, scmPassword, DEVELOPMENT);

				ScmFileSet scmFileSet = new ScmFileSet(new File(baseDirPath), ARTIFACT_XML_REGEX,
				                                         null);
				ScmRepository scmRepository = scmManager.makeScmRepository(scmUrl);
				// update the local repo with modified artifact.xml file
				scmManager.update(scmRepository, scmFileSet);

			} catch (FileNotFoundException e) {
				log.error("Cannot find the release.properties file.", e);
			} catch (IOException e) {
				log.error("Error occurred while loading properties from release.properties file.", e);
			} catch (ScmRepositoryException e) {
				log.error("Error occurred while loading ScmRepository.", e);
			} catch (NoSuchScmProviderException e) {
				log.error("Cannot find Scm Provider.", e);
			} catch (FactoryConfigurationError e) {
				log.error("Error occurred while reading factory configurations.", e);
			} catch (ScmException e) {
				log.error("Error occurred while loading ScmRepository.", e);
			} finally {
				if (input != null) {
					try {
						input.close();
					} catch (IOException e) {
						log.error("Error occurred while closing input stream.", e);
					}
				}
			}
		}
	}

	/**
	 * Method to derive the default SVN tagBase URL (<URL>/tags) from scmURL.
	 *
	 * @param scmUrl SCM URL
	 *
	 * @return Default TagBase for given SVN Repo
	 */
	private String getDefaultSVNTagBase(String scmUrl) {

		return scmUrl.replace(TRUNK_SUFFIX, TAGS_SUFFIX).replace(SCM_SVN_PREFIX, "");
	}

	/**
	 * Method to create the ScmManager Instance.
	 *
	 * @return  ScmManager  ScmManager instance
	 */
	private ScmManager getScmManager() {
		ScmManager scmManager = new BasicScmManager();
		scmManager.setScmProvider(SVN, new SvnExeScmProvider());
		scmManager.setScmProvider(GIT, new GitExeScmProvider());
		return scmManager;
	}

	/**
	 * Check out the artifact.xml file and change the artifact version and
	 * commit the modified file
	 * 
	 * @param scmManager
	 *            ScmManager
	 * @param prop
	 *            properties in release.properties file
	 * @param checkoutUrl
	 *            url of the repository
	 * @param repoType
	 *            type of the repository
	 * @throws FactoryConfigurationError
	 * @throws IOException
	 * @throws ScmException
	 * @throws Exception
	 */
	private void checkoutAndCommit(ScmManager scmManager, Properties prop, String checkoutUrl, String scmUserName, String scmPassword,
	                               String repoType) throws FactoryConfigurationError, IOException,
	                                               ScmException {

		String modifiedCheckoutUrl = checkoutUrl;
		if(scmUserName != null && scmPassword != null){
			// Building the checkout url to have username and password Eg: scm:svn:https://username:password@svn.apache.org/svn/root/module
			modifiedCheckoutUrl = checkoutUrl.replace("://", "://" + scmUserName + ":" + scmPassword + "@");
		}else if(scmUserName != null && scmPassword == null){
			// Building the checkout url to have username Eg: scm:svn:https://username@svn.apache.org/svn/root/module
			modifiedCheckoutUrl = checkoutUrl.replace("://", "://" + scmUserName + "@");
		}
		ScmRepository scmRepository = scmManager.makeScmRepository(modifiedCheckoutUrl);
		String scmBaseDir = project.getBuild().getDirectory();
		// create temp directory for checkout
		File targetFile = new File(scmBaseDir, repoType);
		targetFile.mkdirs();
		ScmFileSet scmFileSet = new ScmFileSet(targetFile, ARTIFACT_XML_REGEX, null);

		CheckOutScmResult checkOut = scmManager.checkOut(scmRepository, scmFileSet);
		if (checkOut.isSuccess()) {
			List<ScmFile> checkedOutFiles = checkOut.getCheckedOutFiles();
			for (ScmFile scmFile : checkedOutFiles) {
				String scmFilePath = scmFile.getPath();
				if (scmFilePath.endsWith(ARTIFACT_XML)) {
					File artifactXml = new File(scmBaseDir, scmFilePath);
					File pomFile =
							new File(scmBaseDir, scmFilePath.replaceAll(ARTIFACT_XML + "$",
							                                            POM_XML));
					if (!pomFile.exists()) {
						log.warn("Skipping as artifact.xml does not belongs to a maven project.");
						continue;
					}
					try {
						if (hasNature(pomFile, ORG_WSO2_DEVELOPER_STUDIO_ECLIPSE_ESB_PROJECT_NATURE)) {
							setESBArtifactVersion(prop, repoType, artifactXml);
						} else if (hasNature(pomFile,
						                     ORG_WSO2_DEVELOPER_STUDIO_ECLIPSE_GENERAL_PROJECT_NATURE)) {
							setRegArtifactVersion(prop, repoType, artifactXml);
						}
					} catch (Exception e) {
						log.error("Error occurred while setting artifact version.", e);
					}

				}
			}
			ScmFileSet scmCheckInFileSet =
					new ScmFileSet(new File(scmBaseDir), ARTIFACT_XML_REGEX, null);
			// commit modified artifact.xml files
			scmManager.checkIn(scmRepository, scmCheckInFileSet,
			                   "Committed the modified artifact.xml file.");
		}
		else {
			log.error("Checkout failed: " + checkOut.getCommandOutput());
		}
	}

	/**
	 * set Registry artifact version in artifact.xml file
	 * 
	 * @param prop
	 *            properties in release.properties file
	 * @param repoType
	 *            type of the repository
	 * @param artifactXml
	 * @throws FactoryConfigurationError
	 * @throws Exception
	 */
	
	private void setRegArtifactVersion(Properties prop, String repoType, File artifactXml)
	                                                                                      throws FactoryConfigurationError,
	                                                                                      Exception {
		File pomFile = new File(artifactXml.getParent() + File.separator + POM_XML);
		MavenProject mavenProject=getMavenProject(pomFile);	
		String releaseVersion = prop.getProperty(PROJECT + repoType + "."+ mavenProject.getGroupId() + ":" + mavenProject.getArtifactId());
		GeneralProjectArtifact projectArtifact = new GeneralProjectArtifact();
		projectArtifact.fromFile(artifactXml);
		for (RegistryArtifact artifact : projectArtifact.getAllESBArtifacts()) {
			if (artifact.getVersion() != null && artifact.getType() != null) {				
				if (releaseVersion != null) {
					artifact.setVersion(releaseVersion);
				}
			}
		}
		projectArtifact.toFile();
	}

	/**
	 * set ESB artifact version in artifact.xml file
	 * 
	 * @param prop
	 *            properties in release.properties file
	 * @param repoType
	 *            type of the repository
	 * @param artifactXml
	 * @throws FactoryConfigurationError
	 * @throws Exception
	 */

	private void setESBArtifactVersion(Properties prop, String repoType, File artifactXml)
	                                                                                      throws FactoryConfigurationError,
	                                                                                      Exception {
		File pomFile = new File(artifactXml.getParent() + File.separator + POM_XML);
		MavenProject mavenProject=getMavenProject(pomFile);	
		String releaseVersion = prop.getProperty(PROJECT + repoType + "."+mavenProject.getGroupId() + ":" + mavenProject.getArtifactId());
		ESBProjectArtifact projectArtifact = new ESBProjectArtifact();
		projectArtifact.fromFile(artifactXml);
		for (ESBArtifact artifact : projectArtifact.getAllESBArtifacts()) {
			if (artifact.getVersion() != null && artifact.getType() != null) {				
				if (releaseVersion != null) {
					artifact.setVersion(releaseVersion);
				}
			}
		}
		projectArtifact.toFile();
	}
	
	/**
	 * 
	 * @param pomFile
	 * @return
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	private MavenProject getMavenProject(final File pomFile) throws IOException, XmlPullParserException {
		MavenXpp3Reader mavenreader = new MavenXpp3Reader();
		FileReader reader = new FileReader(pomFile);
		Model model = mavenreader.read(reader);
		MavenProject project = new MavenProject(model);
		return project;
	}

	/**
	 * check the project nature
	 * 
	 * @param pomFile
	 *            pom file of the project
	 * @param nature
	 *            project nature
	 * @return
	 */

	private boolean hasNature(final File pomFile, final String nature) {
		Model model = null;
		FileReader reader = null;
		MavenXpp3Reader mavenreader = new MavenXpp3Reader();
		try {
			reader = new FileReader(pomFile);
			model = mavenreader.read(reader);
			MavenProject project = new MavenProject(model);
			@SuppressWarnings("unchecked")
			List<Plugin> plugins = project.getBuild().getPlugins();
			for (Plugin plugin : plugins) {
				if (plugin.getArtifactId().equals(MAVEN_ECLIPSE_PLUGIN)) {
					Xpp3Dom configurationNode = (Xpp3Dom) plugin.getConfiguration();
					Xpp3Dom projectnatures = configurationNode.getChild(PROJECT_NATURES);
					Xpp3Dom[] natures = projectnatures.getChildren();
					for (Xpp3Dom xpp3Dom : natures) {
						if (nature.equals(xpp3Dom.getValue())) {
							return true;
						}
					}
					break;
				}
			}
		} catch (FileNotFoundException e) {
			log.error("Cannot find the pom file", e);
		} catch (IOException e) {
			log.error("Error occurred while reading the pom file", e);
		} catch (XmlPullParserException e) {
			log.error("Error occurred while parse the pom file", e);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					log.error("Error occurred while closing input stream", e);
				}
			}
		}

		return false;
	}
	
	/**
	 * Decrypt a given encrypted password.
	 * @param password
	 * @return
	 */
	private String decrypt(String password) {
		try {
			return secDispatcher.decrypt(password);
		} catch (SecDispatcherException sde) {
			log.warn("Error occurred while decrypting password", sde);
			return password;
		}
	}
}
