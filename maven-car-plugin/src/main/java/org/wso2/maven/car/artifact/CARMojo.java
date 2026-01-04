package org.wso2.maven.car.artifact;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.wso2.developerstudio.eclipse.utils.file.FileUtils;
import org.wso2.maven.capp.model.CAppArtifact;
import org.wso2.maven.capp.model.CAppArtifactDependency;
import org.wso2.maven.capp.utils.CAppMavenUtils;
import org.wso2.maven.car.artifact.utils.FileManagementUtil;
import org.wso2.maven.plugin.synapse.utils.SynapseArtifactBundleCreator;

/**
 * Create a bpel artifact from Maven project
 * @description build car artifact
 */
@Mojo(name="car", defaultPhase = LifecyclePhase.PACKAGE)
public class CARMojo extends AbstractMojo {

	private static final String METADATA_ARTIFACT_TYPE = "synapse/metadata";
	private static final String METADATA_FOLDER_NAME = "metadata";
	private static final String METADATA_FILE_NAME = "metadata.xml";
	private static final String ARTIFACTS_FILE_NAME = "artifacts.xml";

    /**
     * Location target folder
     */
	@Parameter(property = "project.build.directory")
    private File target;
    
    /**
     * Location archiveLocation folder
     */
	@Parameter(property = "project.build.directory")
    private File archiveLocation;
    
	/**
	 * finalName to use for the generated capp project if the user wants to override the default name
	 */
	@Parameter
	public String finalName;

	/**
	 * A classifier for the build final name
	 */
	@Parameter
	public String classifier;

	@Parameter(defaultValue = "${project}")
	private MavenProject project;

	/**
	 * Maven ProjectHelper.
	 */
	@Inject
	private MavenProjectHelper projectHelper;

	@Inject
	private ArtifactFactory artifactFactory;

	@Inject
    private ArtifactResolver resolver;

    @Parameter(defaultValue = "${localRepository}")
    private ArtifactRepository localRepository;

    @Parameter(defaultValue = "${project.remoteArtifactRepositories}")
    private List<?> remoteRepositories;
    
    /**
	 * Maven ProjectHelper.
	 */
    @Parameter
    private List<artifact> artifacts;

    private void setupMavenRepoObjects(){
    	CAppMavenUtils.setArtifactFactory(artifactFactory);
    	CAppMavenUtils.setResolver(resolver);
    	CAppMavenUtils.setLocalRepository(localRepository);
    	CAppMavenUtils.setRemoteRepositories(remoteRepositories);
    }
	
    private Map<String,CAppArtifactDependency> cAppArtifactDependencies=new HashMap<String, CAppArtifactDependency>();
    
	public void execute() throws MojoExecutionException, MojoFailureException {
		setupMavenRepoObjects();
		CAppArtifact cAppArtifact = new CAppArtifact(project,null);
		collectArtifacts(cAppArtifact, cAppArtifactDependencies);
		try {
			cAppArtifact.setRoot(true);
			// Creating the metadata.xml file with all the dependencies
			cAppArtifact.toFile(new File(getArchiveDir(), METADATA_FILE_NAME));
			// Creating the artifact.xml file excluding metadata dependencies
			cAppArtifact.getDependencies().removeIf(c -> METADATA_ARTIFACT_TYPE.equals(c.getcAppArtifact().getType()));
			cAppArtifact.toFile(new File(getArchiveDir(), ARTIFACTS_FILE_NAME));
			for (CAppArtifactDependency cAppDependency : cAppArtifactDependencies.values()) {
				cAppArtifact.setRoot(false);
				createArtifactData(getArchiveDir(), cAppDependency);
			}
			FileManagementUtil.zipFolder(getArchiveDir().toString(), getArchiveFile().toString());
			FileManagementUtil.deleteDirectories(getArchiveDir());
			project.getArtifact().setFile(getArchiveFile());
		} catch (Exception e) {
			throw new MojoExecutionException("",e);
		}
	}

	private void createArtifactData(File baseCARLocation, CAppArtifactDependency cAppArtifactDependency) throws IOException, MojoExecutionException{
		getLog().info("Generating artifact descriptor for artifact: "+cAppArtifactDependency.getName());

		if (cAppArtifactDependency.getcAppArtifact().getType() != null &&
				cAppArtifactDependency.getcAppArtifact().getType().equals(METADATA_ARTIFACT_TYPE)) {
			baseCARLocation = new File(baseCARLocation, METADATA_FOLDER_NAME);
		}

		File artifactLocation = new File(baseCARLocation,cAppArtifactDependency.getName()+"_"+cAppArtifactDependency.getVersion());
		
		CAppArtifact cAppArtifact = cAppArtifactDependency.getcAppArtifact();
		Dependency mavenArtifact = cAppArtifactDependency.getMavenDependency();
		
		String artifactFinalName = null;
		
		if(artifacts != null){
			for (artifact cappArtifact : artifacts) {
				if(mavenArtifact.getGroupId().equals(cappArtifact.getGroupId()) && 
						mavenArtifact.getArtifactId().equals(cappArtifact.getArtifactId())){
					artifactFinalName = cappArtifact.getFinalName();
					break;
				}
			}
		}
		
		getLog().info("Copying artifact content to target location.");
		File[] cappArtifactFile = cAppArtifactDependency.getCappArtifactFile();
		for (File file : cappArtifactFile) {
			if (file.isDirectory()){
				FileUtils.copyDirectory(file, new File(artifactLocation,file.getName()));
			}else if(artifactFinalName == null){
				FileUtils.copy(file, new File(artifactLocation,file.getName()));
			}else{
				FileUtils.copy(file, new File(artifactLocation,artifactFinalName));
				cAppArtifact.setFile(artifactFinalName);
			}
		}
		
		cAppArtifact.toFile(new File(artifactLocation,"artifact.xml"));
	}
	
	private void collectArtifacts(CAppArtifact cAppArtifact, Map<String,CAppArtifactDependency> cAppArtifacts) throws MojoExecutionException{
		List<CAppArtifactDependency> dependencies = cAppArtifact.getDependencies();
		for (CAppArtifactDependency artifactDependency : dependencies) {
			if (!cAppArtifacts.containsKey(artifactDependency.getDependencyId())){
				List<CAppArtifactDependency> artifactsToAdd = processArtifactsToAdd(artifactDependency);
				boolean originalDependencyPresent=false;
				for (CAppArtifactDependency cAppArtifactDependency : artifactsToAdd) {
					cAppArtifact.addDependencies(cAppArtifactDependency);
					cAppArtifacts.put(cAppArtifactDependency.getDependencyId(),
							cAppArtifactDependency);
					collectArtifacts(cAppArtifactDependency.getcAppArtifact(),
							cAppArtifacts);	
					originalDependencyPresent=originalDependencyPresent || (artifactDependency.getName().equals(cAppArtifactDependency.getName()) && artifactDependency.getVersion().equals(cAppArtifactDependency.getVersion()));
				}
				if (!originalDependencyPresent){
					cAppArtifact.addIgnoreDependency(artifactDependency);
				}
			}
		}
	}

	private List<CAppArtifactDependency> processArtifactsToAdd(
			CAppArtifactDependency artifactDependency) throws MojoExecutionException{
		List<CAppArtifactDependency> artifactsToAdd =new ArrayList<CAppArtifactDependency>();
		try {
			if (artifactDependency.getcAppArtifact().getProject().getPackaging().equals("synapse/configuration")) {
				SynapseArtifactBundleCreator synapseArtifactBundleCreator = new SynapseArtifactBundleCreator(artifactDependency);
				artifactsToAdd = synapseArtifactBundleCreator.exportDependentArtifacts(artifactDependency.getCappArtifactFile()[0], artifactDependency);
			}else{
				artifactsToAdd.add(artifactDependency);
			}
		} catch (Exception e) {
			throw new MojoExecutionException("Error occured while processing artifact", e);
		}
		return artifactsToAdd;
	}

	public MavenProjectHelper getProjectHelper() {
		return projectHelper;
	}

	public void setTarget(File target) {
		this.target = target;
	}

	public File getTarget() {
		return target;
	}
	
	private File getArchiveDir(){
		File archiveDir = new File(getTarget(),"car");
		if (!archiveDir.exists()){
			archiveDir.mkdirs();
		}
		return archiveDir;
	}

	private File getArchiveFile(){
		String archiveFilename = new StringBuilder().append(project.getArtifactId()).append("_")
		                                            .append(project.getVersion())
		                                            .append(classifier != null ? "-" + classifier : "")
				                                    .append(".car").toString();
		File archiveFile = new File(getArchiveLocation(), archiveFilename);
		if(finalName != null && !finalName.trim().equals("")){
			archiveFile=new File(getArchiveLocation(), finalName+".car");
		}		return archiveFile;
	}
	
	public File getArchiveLocation() {
		return archiveLocation;
	}

	public void setArchiveLocation(File archiveLocation) {
		this.archiveLocation = archiveLocation;
	}
	
}