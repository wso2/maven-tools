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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.wso2.maven.esb.ESBArtifact;
import org.wso2.maven.esb.ESBProjectArtifact;
import org.wso2.maven.registry.GeneralProjectArtifact;
import org.wso2.maven.registry.RegistryArtifact;

import javax.xml.stream.FactoryConfigurationError;
import java.io.*;
import java.util.Collection;
import java.util.Properties;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

public abstract class AbstractMavenReleaseMojo extends AbstractMojo {

	protected static final String ARTIFACT_XML = "artifact.xml";
	protected static final String EMPTY_STRING = "";
	protected static final String POM_XML = "pom.xml";
	protected static final String PROJECT = "project.";
	protected static final String RELEASE_PROPERTIES = "release.properties";

	/**
	 * @parameter expression="${dryRun}" default-value="false"
	 */
	private boolean dryRun;

	/**
	 * The project currently being build.
	 *
	 * @parameter expression="${project}"
	 * @required
	 */
	private MavenProject mavenProject;

	/**
	 * The current Maven session.
	 *
	 * @parameter expression="${session}"
	 * @required
	 */
	private MavenSession mavenSession;

	/**
	 * The Maven BuildPluginManager component.
	 *
	 * @component
	 * @required
	 */
	private BuildPluginManager pluginManager;

	private boolean isSafeToCommit;

	protected final Log log = getLog();
	protected Properties releaseProperties;

	@Override public void execute() throws MojoExecutionException, MojoFailureException {

		File parentProjectBaseDir = mavenProject.getBasedir();
		File releasePropertiesFile = new File(parentProjectBaseDir, RELEASE_PROPERTIES);
		releaseProperties = new Properties();

		if (releasePropertiesFile.exists()) {
			try {
				InputStream io =
						new FileInputStream(releasePropertiesFile);
				releaseProperties.load(io);
			} catch (IOException e) {
				e.printStackTrace();
			}

			IOFileFilter ioFileFilter = new IOFileFilter() {
				@Override public boolean accept(File file) {
					if (file.getPath().endsWith(ARTIFACT_XML)) {
						return true;
					}
					return false;
				}

				@Override public boolean accept(File file, String name) {
					if (name.endsWith(ARTIFACT_XML)) {
						return true;
					}
					return false;
				}
			};

			Collection<File>
					files = (Collection<File>) FileUtils
					.listFiles(parentProjectBaseDir, ioFileFilter,
					           TrueFileFilter.INSTANCE);
			for (File artifactXML : files) {

				File projectPath =
						new File(
								artifactXML.getPath().replaceAll(ARTIFACT_XML + "$", EMPTY_STRING));
				File pomFile = new File(projectPath, POM_XML);
				if (!pomFile.exists()) {
					log.warn("Skipping as artifact.xml does not belongs to a maven project.");
					continue;
				}
				File synapseConfigs =
						new File(projectPath + File.separator + "src" + File.separator + "main" +
						         File.separator, "synapse-config");
				try {
					if (synapseConfigs.exists()) {
						// This is an ESB project
						setESBArtifactVersion(artifactXML);
					} else {
						// This is a registry project
						setRegArtifactVersion(artifactXML);
					}
					isSafeToCommit = true;
				} catch (Exception e) {
					log.error("Error occurred while setting artifact version.", e);
				}
			}

			if (!dryRun && isSafeToCommit) {
				executeMojo(
						plugin(
								groupId("org.apache.maven.plugins"),
								artifactId("maven-scm-plugin"),
								version("1.9.4")
						),
						goal("checkin"),
						configuration(
								element(name("basedir"), parentProjectBaseDir.getAbsolutePath()),
								element(name("message"), getCommitMessage(releaseProperties)),
								element(name("includes"), "**/artifact.xml"),
								element(name("username"),
								        releaseProperties.getProperty("scm.username")),
								element(name("password"),
								        releaseProperties.getProperty("scm.password"))
						),
						executionEnvironment(
								mavenProject,
								mavenSession,
								pluginManager
						)
				);
			}
		}
	}

	protected abstract String getMode();

	protected abstract String getCommitMessage(Properties releaseProperties);

	protected abstract String getNewVersion(File artifactXml)
			throws IOException, XmlPullParserException;

	private void setESBArtifactVersion(File artifactXml)
			throws Exception {
		String newVersion = getNewVersion(artifactXml);
		ESBProjectArtifact projectArtifact = new ESBProjectArtifact();
		projectArtifact.fromFile(artifactXml);
		for (ESBArtifact artifact : projectArtifact.getAllESBArtifacts()) {
			if (artifact.getVersion() != null && artifact.getType() != null) {
				if (newVersion != null) {
					artifact.setVersion(newVersion);
				}
			}
		}
		projectArtifact.toFile();
	}

	private void setRegArtifactVersion(File artifactXml)
			throws Exception {

		String newVersion = getNewVersion(artifactXml);
		GeneralProjectArtifact projectArtifact = new GeneralProjectArtifact();
		projectArtifact.fromFile(artifactXml);
		for (RegistryArtifact artifact : projectArtifact.getAllESBArtifacts()) {
			if (artifact.getVersion() != null && artifact.getType() != null) {
				if (newVersion != null) {
					artifact.setVersion(newVersion);
				}
			}
		}
		projectArtifact.toFile();
	}

	/**
	 * @param pomFile
	 * @return
	 * @throws IOException
	 * @throws org.codehaus.plexus.util.xml.pull.XmlPullParserException
	 */
	protected MavenProject getMavenProject(final File pomFile) throws IOException,
	                                                                      XmlPullParserException {
		MavenXpp3Reader xpp3Reader = new MavenXpp3Reader();
		FileReader reader = new FileReader(pomFile);
		Model model = xpp3Reader.read(reader);
		MavenProject project = new MavenProject(model);
		return project;
	}
}