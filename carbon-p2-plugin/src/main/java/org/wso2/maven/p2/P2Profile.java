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

import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

public class P2Profile {
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
	@Parameter
	private String version;

	private Artifact artifact;
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
	
	protected static P2Profile getP2Profile(String p2ProfileDefinition, P2Profile p2Profile) throws MojoExecutionException{
		String[] split = p2ProfileDefinition.split(":");
		if (split.length>1){
			p2Profile.setGroupId(split[0]);
			p2Profile.setArtifactId(split[1]);
			if (split.length==3) p2Profile.setVersion(split[2]);
			return p2Profile;
		}
		throw new MojoExecutionException("Insufficient artifact information provided to determine the profile: "+p2ProfileDefinition) ; 
	}
	
	public void resolveVersion(MavenProject project) throws MojoExecutionException{
		if (version==null){
			List dependencies = project.getDependencies();
			for (Iterator iterator = dependencies.iterator(); iterator.hasNext();) {
				Dependency dependancy = (Dependency) iterator.next();
				if (dependancy.getGroupId().equalsIgnoreCase(getGroupId())&&dependancy.getArtifactId().equalsIgnoreCase(getArtifactId())){
					setVersion(dependancy.getVersion());
				}
				
			}
		}
		if (version==null) {
			List dependencies = project.getDependencyManagement().getDependencies();
			for (Iterator iterator = dependencies.iterator(); iterator.hasNext();) {
				Dependency dependancy = (Dependency) iterator.next();
				if (dependancy.getGroupId().equalsIgnoreCase(getGroupId())&&dependancy.getArtifactId().equalsIgnoreCase(getArtifactId())){
					setVersion(dependancy.getVersion());
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

	public void setArtifact(Artifact artifact) {
		this.artifact = artifact;
	}

	public Artifact getArtifact() {
		return artifact;
	}
}
