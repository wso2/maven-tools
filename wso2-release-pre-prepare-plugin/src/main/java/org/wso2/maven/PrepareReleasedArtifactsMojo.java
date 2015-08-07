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

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * Implementation of wso2-release:prepare-release. This will modify versions in artifact.xml file
 * of ESB and Registry projects to current release version.
 *
 * @goal prepare-release
 */
public class PrepareReleasedArtifactsMojo extends AbstractMavenReleaseMojo {

	/**
	 * {@inheritDoc}
	 */
	protected String getMode() {
		return "rel";
	}

	/**
	 * {@inheritDoc}
	 */
	protected String getCommitMessage(Properties releaseProperties) {
		return "prepare release " + releaseProperties.getProperty(PROP_SCM_TAG);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override protected String getNewVersion(File artifactXml)
			throws IOException, XmlPullParserException {
		File pomFile = new File(artifactXml.getParent() + File.separator + POM_XML);
		MavenProject mavenProject = getMavenProject(pomFile);
		String newVersion = releaseProperties.getProperty(
				PROJECT_PREFIX + getMode() + "." + mavenProject.getGroupId() + ":" +
				mavenProject.getArtifactId());
		return newVersion;
	}
}
