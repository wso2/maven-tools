/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.maven.pckg.prepare.util;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.FactoryConfigurationError;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.project.MavenProject;
import org.wso2.maven.core.utils.MavenConstants;
import org.wso2.maven.esb.ESBArtifact;
import org.wso2.maven.esb.ESBProjectArtifact;

public class PackagePrepareUtils {

	/**
	 * Returns system paths of artifacts listed under a given project
	 * 
	 * @param project
	 * @return
	 * @throws FactoryConfigurationError
	 * @throws Exception
	 */
	public static Map<String, String> getArtifactsSystemPathMap(MavenProject project) throws FactoryConfigurationError,
			Exception {
		Map<String, String> dependencyData = new HashMap<>();

		StringBuilder artifactXMLPathBuilder = new StringBuilder();
		artifactXMLPathBuilder.append(project.getBasedir().toString());
		artifactXMLPathBuilder.append(File.separator);
		artifactXMLPathBuilder.append(MavenConstants.ARTIFACT_XML_NAME);

		File artifactXMLFile = new File(artifactXMLPathBuilder.toString());

		/*
		 * ATM, System path can only be defined for ESB and Registry projects only. Hence filtered by artifact.xml
		 */
		if (artifactXMLFile.exists()) {
			ESBProjectArtifact artifactXMLDoc = new ESBProjectArtifact();
			artifactXMLDoc.fromFile(artifactXMLFile);

			List<ESBArtifact> artifacts = artifactXMLDoc.getAllESBArtifacts();
			for (ESBArtifact artifact : artifacts) {
				// Key to uniquely identify the dependency
				String dependencyString = getDependencyString(project, artifact);

				// Post fix defines the sub directory for this artifact
				String artifactPostFix = getArtifactPostFix(artifact);

				// Output directory of all artifacts
				StringBuilder systemPathBuilder = new StringBuilder();
				systemPathBuilder.append(project.getBasedir().toString());
				systemPathBuilder.append(File.separator);
				systemPathBuilder.append(MavenConstants.ESB_PROJECT_TARGET_CAPP);
				String outputDirPath = systemPathBuilder.toString();

				// Absolute path of the artifact
				systemPathBuilder = new StringBuilder();
				systemPathBuilder.append(outputDirPath);
				systemPathBuilder.append(File.separator);
				systemPathBuilder.append(artifactPostFix);
				systemPathBuilder.append(File.separator);
				systemPathBuilder.append(artifact.getName());
				systemPathBuilder.append(File.separator);
				systemPathBuilder.append(MavenConstants.POM_FILE_NAME);
				String systemPath = systemPathBuilder.toString();

				dependencyData.put(dependencyString, systemPath);
			}
		}
		return dependencyData;
	}

	private static String getArtifactPostFix(ESBArtifact artifact) {
		// Following artifact types should be changed to common template
		// artifact type
		String artifactType;
		if ((MavenConstants.SEQUENCE_TEMPLATE_TYPE.equals(artifact.getType()))
				|| (MavenConstants.ENDPOINT_TEMPLATE_TYPE.equals(artifact.getType()))) {
			artifactType = MavenConstants.COMMON_TEMPLATE_TYPE;
		} else {
			artifactType = artifact.getType();
		}

		if (MavenConstants.SYNAPSE_CONFIG_TYPE.equalsIgnoreCase(artifactType)) {
			return artifactType.substring(0, artifactType.indexOf("/"));
		} else {
			return artifactType.substring(artifactType.indexOf("/") + 1);
		}
	}

	private static String getDependencyString(MavenProject project, ESBArtifact artifact) {
		String groupId;
		if (StringUtils.isNotEmpty(artifact.getGroupId())) {
			groupId = artifact.getGroupId();
		} else {
			groupId = project.getGroupId();
		}
		String artifactId = artifact.getName();
		String version = artifact.getVersion();

		StringBuilder dependencyStringBuilder = new StringBuilder();
		dependencyStringBuilder.append(groupId);
		dependencyStringBuilder.append(":");
		dependencyStringBuilder.append(artifactId);
		dependencyStringBuilder.append(":");
		dependencyStringBuilder.append(version);
		return dependencyStringBuilder.toString();
	}
}
