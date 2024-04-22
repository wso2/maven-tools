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

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.name;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.twdata.maven.mojoexecutor.MojoExecutor.Element;

/**
 * Abstract Mojo for wso2 maven release plugin.
 */
public abstract class AbstractMavenReleaseMojo extends AbstractMojo {

	/**
     * String constants
     */
    protected static final String ARTIFACT_XML = "artifact.xml";
    protected static final String ARTIFACT_XML_REGEX = "**/artifact.xml";
    protected static final String EMPTY_STRING = "";
    protected static final String POM_XML = "pom.xml";
    protected static final String PROJECT_PREFIX = "project.";
    protected static final String ARTIFACT_XML_TMP_FILE = "artifact.xml.tmp";
    protected static final String RELEASE_PROPERTIES = "release.properties";
    protected static final String WSO2_RELEASE_PLUGIN_PREFIX = "[wso2-release-plugin]";
	protected static final String ARTIFACT = "artifact";
	protected static final String VERSION = "version";

    /**
     * maven scm plugin meta data
     */
    protected static final String MAVEN_PLUGINS_GROUP = "org.apache.maven.plugins";
    protected static final String MAVEN_SCM_PLUGIN = "maven-scm-plugin";
    protected static final String SCM_PLUGIN_VERSION = "1.9.4";
    protected static final String GOAL_CHECK_IN = "checkin";

    /**
     * maven scm plugin params
     */
    protected static final String PARAMETER_BASEDIR = "basedir";
    protected static final String PARAMETER_MESSAGE = "message";
    protected static final String PARAM_INCLUDES = "includes";
    protected static final String PARAM_USERNAME = "username";
    protected static final String PARAM_PASSWORD = "password";

    /**
     * release plugin scm properties
     */
    protected static final String PROP_SCM_TAG = "scm.tag";
    protected static final String PROP_SCM_USERNAME = "scm.username";
    protected static final String PROP_SCM_PASSWORD = "scm.password";

	/**
     * The project currently being build.
     */
    @Parameter(property="project", required = true)
    protected MavenProject mavenProject;

    /**
     * The current Maven session.
     */
    @Parameter(property="session", required = true)
    protected MavenSession mavenSession;

    /**
     * The Maven BuildPluginManager component.
     */
    @Component
    protected BuildPluginManager pluginManager;

    /**
     * Properties loaded from release.properties file.
     */
    protected Properties releaseProperties;

    /**
     * Logger for mojo.
     */
    protected final Log log = getLog();

    /**
     * Method to indicate current goal.
     *
     * @return Current Goal.
     */
    protected abstract String getGoal();

    /**
     * Method to retrieve the file prefix to be used in dryRunMode of current goal.
     *
     * @return File prefix for dryRunMode.
     */
    protected abstract String getDryRunFilePrefix();

    /**
     * Method to get the customised commit message for each goal.
     *
     * @param releaseProperties Properties generated by maven release plugin.
     * @return Commit message for current goal.
     */
    protected abstract String getCommitMessage(Properties releaseProperties);

    /**
     * Method to get the new version for the artifacts. Implementation depends on
     * the goals(prepare-release, prepare-snapshots and rollback).
     *
     * @param artifactXml artifact.xml file of the project.
     * @return new version.
     * @throws IOException
     * @throws XmlPullParserException
     */
    protected abstract String getNewVersion(File artifactXml) throws IOException, XmlPullParserException;

    @Override public void execute() throws MojoExecutionException, MojoFailureException {

        File parentProjectBaseDir = mavenProject.getBasedir();
        File releasePropertiesFile = new File(parentProjectBaseDir, RELEASE_PROPERTIES);
        releaseProperties = new Properties();

        // execute only for the project in repository root.
        if (releasePropertiesFile.exists()) {
            try {
                InputStream inputStream = new FileInputStream(releasePropertiesFile);
                releaseProperties.load(inputStream);
            } catch (IOException e) {
                String errorMessage = "Error while reading " + RELEASE_PROPERTIES;
                throw new MojoExecutionException(errorMessage, e);
            }
            // iterate recursively and filter out files named artifact.xml
            Collection<File> artifactXMLFiles = FileUtils
                    .listFiles(parentProjectBaseDir, new ArtifactXMLFilter(), TrueFileFilter.INSTANCE);
            for (File artifactXML : artifactXMLFiles) {

                File projectPath = new File(artifactXML.getPath().replaceAll(ARTIFACT_XML + "$", EMPTY_STRING));
                File pomFile = new File(projectPath, POM_XML);
                // if not a maven project, continue.
                if (!pomFile.exists()) {
                    log.warn("Skipping project since " + artifactXML.getPath() +
                            " does not belong to a maven project.");
                    continue;
                }
                try {
                    // getNewVersion() depends on current goal.
                    // Eg: for prepare-release, it should be the releasing version
                    //     for prepare-snapshot, it should be the next development version
                    //     for rollback, it should be the previous development version
                    String newVersion = getNewVersion(artifactXML);
	                if (StringUtils.isNotEmpty(newVersion)) {
		                updateArtifactVersions(artifactXML, newVersion);
	                } else {
                        String errorMessage = "Cannot update artifact.xml as new version is empty/null. " +
                                "Project: " + pomFile.getPath() + " Goal: " + getGoal();
                        throw new MojoFailureException(errorMessage);
                    }
                } catch (IOException | XmlPullParserException e) {
                    String errorMessage = "Error occurred while getting the new version for artifacts." +
                            " Project: " + pomFile.getPath() + " Goal: " + getGoal();
                    throw new MojoFailureException(errorMessage, e);
                } catch (Exception e) {
                    String errorMessage =
                            "Error occurred while updating artifact versions. Project: " + pomFile.getPath() + " Goal: "
                                    + getGoal();
                    throw new MojoFailureException(errorMessage, e);
                }
            }
            // commit changes only if not running in dryRun mode
            if (isInDryRunMode()) {
                log.info("Skipped committing changes in dryRun mode.");
            } else {
                try {
	                executeMojo(
			                plugin(groupId(MAVEN_PLUGINS_GROUP),
			                       artifactId(MAVEN_SCM_PLUGIN),
			                       version(SCM_PLUGIN_VERSION)),
			                goal(GOAL_CHECK_IN),
			                configuration(getScmPluginProperties(parentProjectBaseDir)),
			                executionEnvironment(mavenProject, mavenSession, pluginManager));
                } catch (MojoExecutionException e) {
                    throw new MojoExecutionException("Error occurred while invoking maven scm plug-in.", e);
                }
            }
        } else {
            log.debug("Skipping project since the " + RELEASE_PROPERTIES +
                    " file was not found in project root.");
        }
    }

    /**
     * Update versions in the given artifact.xml file of a ESB/DSS project.
     *
     * @param artifactXml artifact.xml file of a ESB/DSS project.
     * @param newVersion  new version to which, the artifacts should be updated.
     * @throws Exception
     */
    protected void updateArtifactVersions(File artifactXml, String newVersion) throws Exception {
        InputStream inputStream = new FileInputStream(artifactXml);
        XMLStreamReader xmlStreamReader = XMLInputFactory.newInstance().createXMLStreamReader(inputStream);
        StAXOMBuilder builder = new StAXOMBuilder(xmlStreamReader);
        OMElement documentElement = builder.getDocumentElement();
        Iterator artifacts = documentElement.getChildrenWithName(new QName(ARTIFACT));
        while (artifacts.hasNext()) {
            OMElement artifact = (OMElement) artifacts.next();
            OMAttribute version = artifact.getAttribute(new QName(VERSION));
            if (version != null) {
                version.setAttributeValue(newVersion);
            }
        }
        if(isInDryRunMode()){
            artifactXml = new File(artifactXml.getPath() + getDryRunFilePrefix());
        }
        File artifactXmlTemp = new File(artifactXml.getParentFile().getPath(),ARTIFACT_XML_TMP_FILE);
        FileOutputStream outputStream = new FileOutputStream(artifactXmlTemp);

        XMLStreamWriter xmlStreamWriter =
                XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream);
        builder.getDocument().serialize(xmlStreamWriter);
        inputStream.close();
        xmlStreamReader.close();
        outputStream.close();
        xmlStreamWriter.flush();
        xmlStreamWriter.close();

        String artifactXmlPath = artifactXml.getPath();
        artifactXml.delete();
        artifactXmlTemp.renameTo(new File(artifactXmlPath));
    }



    /**
     * Method to check  whether release plugin is running in dryRunMode.
     *
     * @return true, if running in dryRun mode.
     */
    protected boolean isInDryRunMode() {
        File projectBaseDir = mavenProject.getBasedir();
        File dryRunPomFile = new File(projectBaseDir, POM_XML + getDryRunFilePrefix());
        return dryRunPomFile.exists();
    }

    /**
     * Method to generate configuration for maven scm plugin.
     *
     * @param parentProjectBaseDir root of the repository.
     * @return configuration for maven scm plugin.
     */
    protected Element[] getScmPluginProperties(File parentProjectBaseDir) {

        // if username is available in release.properties file, scm credentials are passed
        // via console args. Hence we need to forward them to scm plugin also.
        // if username is not available, credentials may configured in settings.xml file and we can avoid passing
        // them as args to scm plugin
        if (releaseProperties.containsKey(PROP_SCM_USERNAME)) {
            log.debug("SCM credentials are found in release.properties file.");
            return new Element[] { element(name(PARAMETER_BASEDIR), parentProjectBaseDir.getAbsolutePath()),
                    element(name(PARAMETER_MESSAGE),
                            WSO2_RELEASE_PLUGIN_PREFIX + " " + getCommitMessage(releaseProperties).trim()),
                    element(name(PARAM_INCLUDES), ARTIFACT_XML_REGEX),
                    element(name(PARAM_USERNAME), releaseProperties.getProperty(PROP_SCM_USERNAME)),
                    element(name(PARAM_PASSWORD), releaseProperties.getProperty(PROP_SCM_PASSWORD)) };

        } else {
            log.debug("SCM credentials are not found in release.properties file.");
            return new Element[] { element(name(PARAMETER_BASEDIR), parentProjectBaseDir.getAbsolutePath()),
                    element(name(PARAMETER_MESSAGE),
                            WSO2_RELEASE_PLUGIN_PREFIX + " " + getCommitMessage(releaseProperties).trim()),
                    element(name(PARAM_INCLUDES), ARTIFACT_XML_REGEX) };
        }
    }

    /**
     * Method to instantiate a Maven project model from a pom file.
     *
     * @param pomFile Path to pom file.
     * @return Project model
     * @throws IOException
     * @throws org.codehaus.plexus.util.xml.pull.XmlPullParserException
     */
    protected MavenProject getMavenProject(final File pomFile) throws IOException, XmlPullParserException {
        MavenXpp3Reader xpp3Reader = new MavenXpp3Reader();
        FileReader reader = new FileReader(pomFile);
        Model model = xpp3Reader.read(reader);
        return new MavenProject(model);
    }

    /**
     * Filter for artifact.xml files.
     */
    class ArtifactXMLFilter implements IOFileFilter {
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
    }
}