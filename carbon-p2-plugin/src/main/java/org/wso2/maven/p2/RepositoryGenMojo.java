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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.internal.p2.updatesite.CategoryPublisherApplication;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.publisher.eclipse.FeaturesAndBundlesPublisherApplication;
import org.wso2.maven.p2.generate.utils.FileManagementUtil;
import org.wso2.maven.p2.generate.utils.MavenUtils;
import org.wso2.maven.p2.generate.utils.P2Utils;

/**
 * Write environment information for the current build to file.
 */
@Mojo(name = "p2-repo-gen", defaultPhase = LifecyclePhase.PACKAGE)
public class RepositoryGenMojo extends AbstractMojo {

    /**
     * Name of the repository
     */
    @Parameter(name = "name")
    private String name;

    /**
     * URL of the Metadata Repository
     */
    @Parameter(name = "metadataRepository")
    private URL metadataRepository;

    /**
     * URL of the Artifact Repository
     */
    @Parameter(name = "artifactRepository")
    private URL artifactRepository;

    /**
     * Source folder
     */
    @Parameter(name = "featureArtifacts", required = true)
    private ArrayList featureArtifacts;

    /**
     * Source folder
     */
    @Parameter(name = "bundleArtifacts")
    private ArrayList bundleArtifacts;
    
    /**
     * Source folder
     */
    @Parameter(name = "categories")
    private ArrayList categories;

    /**
     * flag indicating whether the artifacts should be published to the repository. When this flag is not set,
     * the actual bytes underlying the artifact will not be copied, but the repository index will be created.
     * When this option is not specified, it is recommended to set the artifactRepository to be in the same location
     * as the source (-source)
     */
    @Parameter(name = "publishArtifacts")
    private boolean publishArtifacts;

    /**
     * Type of Artifact (War,Jar,etc)
     */
    @Parameter(name = "publishArtifactRepository")
    private boolean publishArtifactRepository;

    /**
     * Equinox Launcher
     */
    @Parameter(name = "equinoxLauncher")
    private EquinoxLauncher equinoxLauncher;


    /**
     * Equinox p2 configuration path
     */
    @Parameter(name = "p2Profile")
    private P2Profile p2Profile;

    @Parameter(name = "project", defaultValue = "${project}")
    private MavenProject project;

    @Parameter(name = "archive", defaultValue = "false")
    private boolean archive;

    @Component
    private org.apache.maven.artifact.factory.ArtifactFactory artifactFactory;

    @Component
    private org.apache.maven.artifact.resolver.ArtifactResolver resolver;

    @Parameter(name = "localRepository", defaultValue = "${localRepository}")
    private org.apache.maven.artifact.repository.ArtifactRepository localRepository;

    @Parameter(name = "remoteRepositories", defaultValue = "${project.remoteArtifactRepositories}")
    private List remoteRepositories;

    /**
     * Kill the forked test process after a certain number of seconds. If set to 0, wait forever for
     * the process, never timing out.
     */
    @Parameter(name = "forkedProcessTimeoutInSeconds", property = "p2.timeout")
    private int forkedProcessTimeoutInSeconds;

    @Component
    private IProvisioningAgent agent;

    private ArrayList processedFeatureArtifacts;
    private ArrayList processedP2LauncherFiles;
    private File targetDir;
    private File tempDir;
    private File sourceDir;
    private File p2AgentDir;

	private ArrayList processedBundleArtifacts;

	private File REPO_GEN_LOCATION;
	
	private File categoryDeinitionFile;

	private File ARCHIVE_FILE;

    public void execute() throws MojoExecutionException, MojoFailureException {
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
            this.getLog().info("Running Equinox P2 Publisher Application for Repository Generation");
            generateRepository();
            this.getLog().info("Running Equinox P2 Category Publisher Application for the Generated Repository");
            updateRepositoryWithCategories();
            archiveRepo();
        } catch (Exception e) {
            this.getLog().error(e.getMessage(), e);
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private void copyResources() throws MojoExecutionException {
        List resources = project.getResources();
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


    protected FeaturesAndBundlesPublisherApplication getPublisherApplication() {
        return new FeaturesAndBundlesPublisherApplication();
    }

    private void generateRepository() throws Exception {

        List<String> arguments = new ArrayList<>();

        addArguments(arguments);

        Object result = getPublisherApplication().run(arguments.toArray(String[]::new));
        if (result != IApplication.EXIT_OK) {
            throw new MojoFailureException("P2 publisher return code was " + result);
        }
    }

    private void addArguments(List<String> arguments) throws IOException, MalformedURLException {
        arguments.add("-source");
        arguments.add(sourceDir.getAbsolutePath());
        arguments.add("-metadataRepository");
        arguments.add(metadataRepository.toString());
        arguments.add("-metadataRepositoryName");
        arguments.add(getRepositoryName());
        arguments.add("-artifactRepository");
        arguments.add(metadataRepository.toString());
        arguments.add("-artifactRepositoryName");
        arguments.add(getRepositoryName());
        arguments.add("-publishArtifacts");
        arguments.add("-publishArtifactRepository");
        arguments.add("-compress");
        arguments.add("-append");
    }
    
    private void extractFeatures() throws MojoExecutionException {
        ArrayList processedFeatureArtifacts = getProcessedFeatureArtifacts();
        if (processedFeatureArtifacts == null) return;
        for (Iterator iterator = processedFeatureArtifacts.iterator(); iterator
                .hasNext();) {
            FeatureArtifact featureArtifact = (FeatureArtifact) iterator.next();
            try {
                getLog().info("Extracting feature " + featureArtifact.getGroupId()
                        + ":" + featureArtifact.getArtifactId());
                FileManagementUtil.unzip(featureArtifact.getArtifact().getFile(), sourceDir);
            } catch (Exception e) {
                throw new MojoExecutionException(
                        "Error occured when extracting the Feature Artifact: " + featureArtifact.toString(), e);
            }
        }
    }

    private void copyBundleArtifacts()throws MojoExecutionException {
        ArrayList processedBundleArtifacts = getProcessedBundleArtifacts();
        if (processedBundleArtifacts == null) return;
        File pluginsDir = new File(sourceDir,"plugins");
        for (Iterator iterator = processedBundleArtifacts.iterator(); iterator
                .hasNext();) {
            BundleArtifact bundleArtifact = (BundleArtifact) iterator.next();
            try {
            	File file = bundleArtifact.getArtifact().getFile();
                FileManagementUtil.copy(file, new File(pluginsDir,file.getName()));
            } catch (Exception e) {
                throw new MojoExecutionException("Error occurred when extracting the Feature Artifact: "
                        + bundleArtifact.toString(), e);
            }
        }
    }

    private ArrayList getProcessedFeatureArtifacts() throws MojoExecutionException {
        if (processedFeatureArtifacts != null)
            return processedFeatureArtifacts;
        if (featureArtifacts == null || featureArtifacts.size() == 0) return null;
        processedFeatureArtifacts = new ArrayList();
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
                f.setArtifact(MavenUtils.getResolvedArtifact(f, getArtifactFactory(), remoteRepositories, getLocalRepository(), getResolver()));
                processedFeatureArtifacts.add(f);
        	} catch (Exception e) {
                throw new MojoExecutionException("Error occured when processing the Feature Artifact: " + obj.toString(), e);
            }
        }
        return processedFeatureArtifacts;
    }
    
    private void archiveRepo() throws MojoExecutionException {
    	if (isArchive()) {
    		getLog().info("Generating repository archive...");
    		FileManagementUtil.zipFolder(REPO_GEN_LOCATION.toString(), ARCHIVE_FILE.toString());
    		getLog().info("Repository Archive: "+ARCHIVE_FILE.toString());
    		FileManagementUtil.deleteDirectories(REPO_GEN_LOCATION);
    	}
    }
    
    private ArrayList getProcessedBundleArtifacts() throws MojoExecutionException {
        if (processedBundleArtifacts != null)
            return processedBundleArtifacts;
        if (bundleArtifacts == null || bundleArtifacts.size() == 0) return null;
        processedBundleArtifacts = new ArrayList();
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
            f.setArtifact(MavenUtils.getResolvedArtifact(f, getArtifactFactory(), remoteRepositories, getLocalRepository(), getResolver()));
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
			metadataRepository = repo.toURL();
			artifactRepository = metadataRepository;
		}
        REPO_GEN_LOCATION=new File(metadataRepository.getFile().replace("/",File.separator));
        ARCHIVE_FILE=new File(targetDir,getProject().getArtifactId()+"_"+getProject().getVersion()+".zip");
        categoryDeinitionFile=File.createTempFile("equinox-p2", "category");
    }

	private void updateRepositoryWithCategories() throws Exception {
		if (!isCategoriesAvailable()) {
			return;
		} else {
			P2Utils.createCategoryFile(getProject(), categories, categoryDeinitionFile,
			                           getArtifactFactory(), getRemoteRepositories(),
			                           getLocalRepository(), getResolver());
            List<String> arguments = new ArrayList<>();
            arguments.add("-metadataRepository");
            arguments.add(metadataRepository.toString());
            arguments.add("-categoryDefinition");
            arguments.add(categoryDeinitionFile.toURI().toString());
            arguments.add("-categoryQualifier");
            arguments.add("-compress");
            arguments.add("-append");

            Object result = new CategoryPublisherApplication().run(arguments.toArray(String[]::new));
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

    public void setArtifactFactory(org.apache.maven.artifact.factory.ArtifactFactory artifactFactory) {
        this.artifactFactory = artifactFactory;
    }

    public void setResolver(org.apache.maven.artifact.resolver.ArtifactResolver resolver) {
        this.resolver = resolver;
    }

    public void setLocalRepository(org.apache.maven.artifact.repository.ArtifactRepository localRepository) {
        this.localRepository = localRepository;
    }

    public void setRemoteRepositories(List remoteRepositories) {
        this.remoteRepositories = remoteRepositories;
    }

    public MavenProject getProject() {
        return project;
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

    public List getRemoteRepositories() {
        return remoteRepositories;
    }

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
