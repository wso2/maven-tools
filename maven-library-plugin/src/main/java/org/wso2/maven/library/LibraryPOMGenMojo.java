package org.wso2.maven.library;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.wso2.maven.capp.bundleartifact.AbstractBundlePOMGenMojo;
import org.wso2.maven.capp.model.Artifact;

/**
 * This is the Maven Mojo used for generating a pom for a synapse custome mediator artifact 
 * from the old CApp project structure
 */
@Mojo(name = "pom-gen")
public class LibraryPOMGenMojo extends AbstractBundlePOMGenMojo {
	
	@Parameter(defaultValue = "${project}")
	public MavenProject project;

	/**
	 * Maven ProjectHelper.
	 */
	@Inject
	public MavenProjectHelper projectHelper;

	/**
	 * The path of the location to output the pom
	 */
	@Parameter(defaultValue = "${project.build.directory}/artifacts")
	public File outputLocation;

	/**
	 * The resulting extension of the file
	 */
	@Parameter
	public File artifactLocation;
	
	/**
	 * POM location for the module project
	 */
	@Parameter(defaultValue = "${project.build.directory}/pom.xml")
	public File moduleProject;
	
	/**
	 * Group id to use for the generated pom
	 */
	@Parameter
	public String groupId;

	/**
	 * Comma separated list of "artifact_type=extension" to be used when creating dependencies for other capp artifacts
	 */
	@Parameter
	public String typeList;

	/**
	 * A list of projects in eclipse workspace which can be referred using maven groupid, artifactid, version
	 */
	@Parameter
	private List<String> projects;

	@Inject
	public ArtifactFactory artifactFactory;

	@Inject
    public ArtifactResolver resolver;

    @Parameter(defaultValue = "${localRepository}")
    public ArtifactRepository localRepository;

    @Parameter(defaultValue = "${project.remoteArtifactRepositories}")
    public List<?> remoteRepositories;


	private static final String ARTIFACT_TYPE="lib/library/bundle";

	protected String getArtifactType() {
		return ARTIFACT_TYPE;
	}

	protected String getBundleActivatorClass(Artifact artifact) {
		return null;
	}

	protected List<String> getProjectMapStrings() {
		return projects;
	}

	public ArtifactFactory getArtifactFactory() {
		return artifactFactory;
	}

	public ArtifactResolver getResolver() {
		return resolver;
	}

	public ArtifactRepository getLocalRepository() {
		return localRepository;
	}

	public List<?> getRemoteRepositories() {
		return remoteRepositories;
	}
	
	protected void addPlugins(MavenProject artifactMavenProject,Artifact artifact) {
		addMavenBundlePlugin(artifactMavenProject, artifact);
	}

}
