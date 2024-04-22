/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.maven.p2;

import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.artifact.Artifact;

public class FeatureArtifact {
    /**
     * Group Id of the Bundle
     */
	@Parameter(required = true)
	private String groupId;
	
	/**
     * Artifact Id of the Bundle
     */
	@Parameter(required = true)
	private String artifactId;
	
    /**
     * Version of the Bundle
     */
	@Parameter(defaultValue = "")
	private String version;

	private Artifact artifact;
	
	private String featureId;
	private String featureVersion;
	
	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}
	public String getGroupId() {
		return groupId;
	}
	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}
	public String getArtifactId() {
		return artifactId;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public String getVersion() {
		return version;
	}
	public void setArtifact(Artifact artifact) {
		this.artifact = artifact;
	}
	public Artifact getArtifact() {
		return artifact;
	}
	protected static FeatureArtifact getFeatureArtifact(String featureArtifactDefinition, FeatureArtifact featureArtifact) throws MojoExecutionException{
		String[] split = featureArtifactDefinition.split(":");
		if (split.length>1){
			featureArtifact.setGroupId(split[0]);
			featureArtifact.setArtifactId(split[1]);
			if (split.length==3) featureArtifact.setVersion(split[2]);
			return featureArtifact;
		}
		throw new MojoExecutionException("Insufficient artifact information provided to determine the feature: "+featureArtifactDefinition) ; 
	}
	public static FeatureArtifact getFeatureArtifact(String featureArtifactDefinition) throws MojoExecutionException{
		return getFeatureArtifact(featureArtifactDefinition, new FeatureArtifact());
	}
	public void resolveVersion(MavenProject project) throws MojoExecutionException{
		if (version==null){
			//Check direct dependencies
			List<Dependency> projectDependencies = project.getDependencies();
			for (Dependency prjDependency : projectDependencies) {
				if (prjDependency.getGroupId().equalsIgnoreCase(getGroupId())&&prjDependency.getArtifactId().equalsIgnoreCase(getArtifactId())){
					setVersion(prjDependency.getVersion());
				}
			}
			
		}
		if (version==null) {
			//Check inherited dependencies
			List<Dependency> dependencies = project.getDependencyManagement().getDependencies();
			for (Dependency dmDependency: dependencies) {
				if (dmDependency.getGroupId().equalsIgnoreCase(getGroupId())&&dmDependency.getArtifactId().equalsIgnoreCase(getArtifactId())){
					setVersion(dmDependency.getVersion());
				}
			}
		}
		if (version==null) {
			throw new MojoExecutionException("Could not find the version for "+getGroupId()+":"+getArtifactId());
		}
		Properties properties = project.getProperties();
		for(Object key:properties.keySet()){
			version=version.replaceAll(Pattern.quote("${"+key+"}"), properties.get(key).toString());
		}
	}
	
	public void setFeatureId(String featureId) {
		this.featureId = featureId;
	}
	public String getFeatureId() {
		return featureId;
	}
	public void setFeatureVersion(String featureVersion) {
		this.featureVersion = featureVersion;
	}
	public String getFeatureVersion() {
		return featureVersion;
	}
}
