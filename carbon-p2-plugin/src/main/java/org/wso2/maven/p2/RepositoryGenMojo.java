/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.maven.p2;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.internal.p2.updatesite.CategoryPublisherApplication;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.publisher.eclipse.FeaturesAndBundlesPublisherApplication;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.wso2.maven.p2.generate.utils.FileManagementUtil;
import org.wso2.maven.p2.generate.utils.MavenUtils;
import org.wso2.maven.p2.generate.utils.P2Utils;
import org.wso2.maven.p2.generate.utils.RepoSystemHolder;

/**
 * Write environment information for the current build to file.
 */
@Mojo(defaultPhase = LifecyclePhase.PACKAGE, name = "p2-repo-gen")
public class RepositoryGenMojo extends AbstractMojo {

	/**
     * Name of the repository
     */
	@Parameter
    private String name;

    /**
     * URL of the Metadata Repository
     *
     */
	@Parameter
    private URL metadataRepository;

    /**
     * URL of the Artifact Repository
     *
     */
	@Parameter
    private URL artifactRepository;

    /**
     * Source folder
     */
	@Parameter(required = true)
    private ArrayList featureArtifacts;

    /**
     * Source folder
     *
     */
	@Parameter
    private ArrayList bundleArtifacts;

    /**
     * Source folder
     *
     */
	@Parameter
    private ArrayList categories;

    /**
     * flag indicating whether the artifacts should be published to the repository. When this flag is not set,
     * the actual bytes underlying the artifact will not be copied, but the repository index will be created.
     * When this option is not specified, it is recommended to set the artifactRepository to be in the same location
     * as the source (-source)
     *
     */
	@Parameter
    private boolean publishArtifacts;

    /**
     * Type of Artifact (War,Jar,etc)
     */
	@Parameter
    private boolean publishArtifactRepository;

    /**
     * Equinox p2 configuration path
     */
	@Parameter
    private P2Profile p2Profile;

	@Parameter(defaultValue = "${project}")
    private MavenProject project;

	@Parameter(defaultValue = "false")
    private boolean archive;

	//See https://stackoverflow.com/questions/28361289/for-a-maven-3-plugin-what-is-the-latest-way-to-resolve-a-artifact
	//See https://blog.sonatype.com/2011/01/how-to-use-aether-in-maven-plugins/
	//See https://wiki.eclipse.org/Aether/Using_Aether_in_Maven_Plugins
	/**
     * The entry point to Aether, i.e. the component doing all the work.
     */
    @Component
    private RepositorySystem repoSystem;
    
    /**
     * The current repository/network configuration of Maven.
     */
    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;
    
    /**
     * The project's remote repositories to use for the resolution of project dependencies.
     */
    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true)
    private List<RemoteRepository> remoteProjectRepos;
    
    /**
     * The project's remote repositories to use for the resolution of plugins and their dependencies.
     */
    @Parameter(defaultValue = "${project.remotePluginRepositories}", readonly = true)
    private List<RemoteRepository> remotePluginRepos;
    
    private List<FeatureArtifact> processedFeatureArtifacts;
    private File targetDir;
    private File tempDir;
    private File sourceDir;

	private List<BundleArtifact> processedBundleArtifacts;

	private File REPO_GEN_LOCATION;

	private File categoryDefinitionFile;

	private File ARCHIVE_FILE;
	
	@Component
	private IProvisioningAgent agent;

    public void execute() throws MojoExecutionException, MojoFailureException {
    	agent.getService(IArtifactRepositoryManager.class); //force init P2 services
        createRepo();
        performMopUp();
    }

    public void createRepo() throws MojoExecutionException, MojoFailureException {
        try {
            getProcessedFeatureArtifacts();
            getProcessedBundleArtifacts();
            createAndSetupPaths();
            extractFeatures();
            copyBundleArtifacts();
            copyResources();
            getLog().info("Running Equinox P2 Publisher Application for Repository Generation");
            generateRepository();
            getLog().info("Running Equinox P2 Category Publisher Application for the Generated Repository");
            updateRepositoryWithCategories();
            archiveRepo();
        } catch (Exception e) {
            getLog().error(e.getMessage(), e);
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }



    private void copyResources() throws MojoExecutionException {
        List<Resource> resources = project.getResources();
        if (resources != null) {
            getLog().info("Copying resources");
            for (Object obj : resources) {
                if (obj instanceof Resource) {
                    Resource resource = (Resource) obj;
                    try {
                        File resourceFolder = new File(resource.getDirectory());
                        if (resourceFolder.exists()) {
                            getLog().info("   " + resource.getDirectory());
                            FileManagementUtil.copyDirectory(resourceFolder, REPO_GEN_LOCATION);
                        }
                    } catch (IOException e) {
                        throw new MojoExecutionException("Unable copy resources: " + resource.getDirectory(), e);
                    }
                }
            }
        }
    }


    private void generateRepository() throws Exception {
    	
    	FeaturesAndBundlesPublisherApplication application = new FeaturesAndBundlesPublisherApplication();
    	Object result = application.run(getGenerateRepositoryConfigurations());
    	if (result != IApplication.EXIT_OK) {
            throw new MojoFailureException("P2 publisher return code was " + result);
        }
    }
    
    private String[] getGenerateRepositoryConfigurations()  throws Exception {
    	String[] result = new String[] {
    		"-ar",
    		String.format("%s", metadataRepository.toURI()),
    		"-mr",
    		String.format("%s", metadataRepository.toURI()),
    		"-source",
    		sourceDir.getAbsolutePath(),
    		"-artifactRepositoryName",
    		getRepositoryName(),
    		"-metadataRepositoryName",
    		getRepositoryName(),
    		"-publishArtifacts",
    		"-publishArtifactRepository",
    		"-compress",
    		"-append"
    	};
    	return result; 
    }
    
    private void extractFeatures() throws MojoExecutionException {
        List<FeatureArtifact> processedFeatureArtifacts = getProcessedFeatureArtifacts();
        if (processedFeatureArtifacts == null) return;
        //getLog().info("Extracting features to: "+sourceDir.getAbsolutePath());
        for (FeatureArtifact featureArtifact : processedFeatureArtifacts) {
        	try {
		        getLog().info("Extracting feature "+featureArtifact.getGroupId()+":"+featureArtifact.getArtifactId());
                FileManagementUtil.unzip(featureArtifact.getArtifact().getFile(), sourceDir);
            } catch (Exception e) {
                throw new MojoExecutionException("Error occured when extracting the Feature Artifact: " + featureArtifact.toString(), e);
            }
        }
    }

    private void copyBundleArtifacts()throws MojoExecutionException {
        List<BundleArtifact> processedBundleArtifacts = getProcessedBundleArtifacts();
        if (processedBundleArtifacts == null) return;
        File pluginsDir = new File(sourceDir,"plugins");
        for (BundleArtifact bundleArtifact : processedBundleArtifacts) {
        	try {
            	File file = bundleArtifact.getArtifact().getFile();
                FileManagementUtil.copy(file, new File(pluginsDir,file.getName()));
            } catch (Exception e) {
                throw new MojoExecutionException("Error occured when extracting the Feature Artifact: " + bundleArtifact.toString(), e);
            }
        }
    }

    private List<FeatureArtifact> getProcessedFeatureArtifacts() throws MojoExecutionException {
        if (processedFeatureArtifacts != null)
            return processedFeatureArtifacts;
        if (featureArtifacts == null || featureArtifacts.size() == 0) return null;
        processedFeatureArtifacts = new ArrayList<FeatureArtifact>();
        RepoSystemHolder repoRefs = new RepoSystemHolder(repoSystem, repoSession, Stream.concat(remotePluginRepos.stream(), remoteProjectRepos.stream()).collect(Collectors.toList()));
        Iterator iter = featureArtifacts.iterator();
        while (iter.hasNext()) {
        	FeatureArtifact f = null;
        	Object obj = iter.next();
        	try {
        		if (obj instanceof FeatureArtifact) {
        			f = (FeatureArtifact) obj;
                } else if (obj instanceof String) {
                    f = FeatureArtifact.getFeatureArtifact(obj.toString());
                } else
                    f = (FeatureArtifact) obj;
                f.resolveVersion(getProject());
                //getLog().info( "Resolving artifact " + f + " from " + remotePluginRepos );
                f.setArtifact(MavenUtils.getResolvedArtifact(getLog(), repoRefs , f));
                processedFeatureArtifacts.add(f);
        	} catch (Exception e) {
                throw new MojoExecutionException("Error occured when processing the Feature Artifact: " + obj.toString(), e);
            }
        }
        return processedFeatureArtifacts;
    }

    private void archiveRepo() throws MojoExecutionException {
    	if (isArchive()){
    		getLog().info("Generating repository archive...");
    		FileManagementUtil.zipFolder(REPO_GEN_LOCATION.toString(), ARCHIVE_FILE.toString());
    		getLog().info("Repository Archive: "+ARCHIVE_FILE.toString());
    		FileManagementUtil.deleteDirectories(REPO_GEN_LOCATION);
    	}
    }

    private List<BundleArtifact> getProcessedBundleArtifacts() throws MojoExecutionException {
        if (processedBundleArtifacts != null)
            return processedBundleArtifacts;
        if (bundleArtifacts == null || bundleArtifacts.size() == 0) return null;
        processedBundleArtifacts = new ArrayList<BundleArtifact>();
        RepoSystemHolder repoRefs = new RepoSystemHolder(repoSystem, repoSession, Stream.concat(remotePluginRepos.stream(), remoteProjectRepos.stream()).collect(Collectors.toList()));
        Iterator iter = bundleArtifacts.iterator();
        while (iter.hasNext()) {
            Object obj = iter.next();
            BundleArtifact f;
            if (obj instanceof BundleArtifact) {
                f = (BundleArtifact) obj;
            } else if (obj instanceof String) {
                f = BundleArtifact.getBundleArtifact(obj.toString());
            } else
                f = (BundleArtifact) obj;
            f.resolveVersion(getProject());
            //getLog().info( "Resolving artifact " + f + " from " + remotePluginRepos );
            f.setArtifact(MavenUtils.getResolvedArtifact(repoRefs, f));
            processedBundleArtifacts.add(f);
        }
        return processedBundleArtifacts;
    }



    private void createAndSetupPaths() throws Exception {
        targetDir = new File(getProject().getBasedir(), "target");
        String timestampVal = String.valueOf((new Date()).getTime());
        tempDir = new File(targetDir, "tmp." + timestampVal);
        sourceDir = new File(tempDir, "featureExtract");
        sourceDir.mkdirs();

		metadataRepository=(artifactRepository==null? metadataRepository:artifactRepository);
		artifactRepository=(metadataRepository==null? artifactRepository:metadataRepository);
		if (metadataRepository == null) {
			File repo = new File(targetDir, getProject().getArtifactId() + "_" + getProject().getVersion());
			metadataRepository = repo.toURI().toURL();
			artifactRepository = metadataRepository;
		}
        REPO_GEN_LOCATION=new File(metadataRepository.getFile().replace("/",File.separator));
        ARCHIVE_FILE=new File(targetDir,getProject().getArtifactId()+"_"+getProject().getVersion()+".zip");
        categoryDefinitionFile=File.createTempFile("equinox-p2", "category");
    }
    
    private String[] getUpdateRepositoryConfigurations() {
    	String[] result = new String[] {
    			"-categoryDefinition", categoryDefinitionFile.toURI().toString(),
    			"-metadataRepositoryName", getRepositoryName(),
    			"-artifactRepositoryName", getRepositoryName(),
    			"-categoryQualifier", //Where is the value for the category qualifier?
    			"-append"
    	};
    	return result; 
    }

	private void updateRepositoryWithCategories() throws Exception {
		if (!isCategoriesAvailable()) {
			return;
		} else {
			P2Utils.createCategoryFile(getProject(), categories, categoryDefinitionFile);
			
			CategoryPublisherApplication application = new CategoryPublisherApplication();
			Object result = application.run(getUpdateRepositoryConfigurations());
			if (result != IApplication.EXIT_OK) {
				throw new MojoFailureException("P2 publisher return code was " + result);
			}
		}
	}

	private boolean isCategoriesAvailable() {
		if (categories == null || categories.size() == 0) {
			return false;
		}
		return true;
	}

    private void performMopUp() {
        try {
            // we want this temp file, in order to debug some errors. since this is in target, it will
            // get removed in the next build cycle.
           // FileUtils.deleteDirectory(tempDir);
        } catch (Exception e) {
            getLog().warn(new MojoExecutionException("Unable complete mop up operation", e));
        }
    }



    public void setP2Profile(P2Profile p2Profile) {
        this.p2Profile = p2Profile;
    }


    public P2Profile getP2Profile() {
        return p2Profile;
    }


    public void setProject(MavenProject project) {
        this.project = project;
    }

//    public void setLocalRepository(LocalRepository localRepository) {
//        this.localRepository = localRepository;
//    }

//    public void setRemoteRepositories(List remoteRepositories) {
//        this.remoteRepositories = remoteRepositories;
//    }

    public MavenProject getProject() {
        return project;
    }

//    public LocalRepository getLocalRepository() {
//        return localRepository;
//    }

//    public List getRemoteRepositories() {
//        return remoteRepositories;
//    }

	public String getRepositoryName() {
		if (name==null){
			return getProject().getArtifactId();
		}else{
			return name;
		}
	}

	public boolean isArchive() {
		return archive;
	}
}
