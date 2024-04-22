package org.wso2.maven.car.artifact;

import org.apache.maven.plugins.annotations.Parameter;

public class CARMavenArtifact {
	
    /**
     * group id of the CAR artifact
     */
	@Parameter(required = true)
	private String groupId;
	
    /**
     * artifact id of the CAR artifact
     */
	@Parameter(required = true)
	private String artifactId;

    /**
     * version of the CAR artifact
     */
	@Parameter(required = true)
	private String version;
	
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
}
