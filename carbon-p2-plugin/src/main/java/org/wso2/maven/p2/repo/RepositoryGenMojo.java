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
package org.wso2.maven.p2.repo;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.eclipse.tycho.p2.facade.internal.P2ApplicationLauncher;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Write environment information for the current build to file.
 */
@Mojo(name = "p2-repo-gen", defaultPhase = LifecyclePhase.PACKAGE)
public class RepositoryGenMojo extends AbstractMojo {

//    /**
//     * URL of the Metadata Repository
//     *
//     * @parameter
//     */
//    private URL repository;

    /**
     * Name of the repository
     */
    @Parameter
    private String name;

    /**
     * URL of the Metadata Repository
     */
    @Parameter
    private URL metadataRepository;

    /**
     * URL of the Artifact Repository
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
     */
    @Parameter
    private ArrayList bundleArtifacts;

    /**
     * Source folder
     */
    @Parameter
    private ArrayList categories;

    /**
     * flag indicating whether the artifacts should be published to the repository. When this flag is not set,
     * the actual bytes underlying the artifact will not be copied, but the repository index will be created.
     * When this option is not specified, it is recommended to set the artifactRepository to be in the same location
     * as the source (-source)
     */
    @Parameter
    private boolean publishArtifacts;

    /**
     * Type of Artifact (War,Jar,etc)
     */
    @Parameter
    private boolean publishArtifactRepository;

    @Parameter(defaultValue = "${project}")
    private MavenProject project;

    @Parameter(defaultValue = "false")
    private boolean archive;

    @Component
    private RepositorySystem repositorySystem;

    @Parameter(defaultValue = "${localRepository}")
    private ArtifactRepository localRepository;


    @Parameter(defaultValue = "${project.remoteArtifactRepositories}")
    private List remoteRepositories;


    @Component
    private P2ApplicationLauncher launcher;

    /**
     * Kill the forked test process after a certain number of seconds. If set to 0, wait forever for
     * the process, never timing out.
     *
     */
    @Parameter(defaultValue = "${p2.timeout}")
    private int forkedProcessTimeoutInSeconds;

    public void execute() throws MojoExecutionException, MojoFailureException {
        RepoGenerator generator = constructRepoGenerator();
        generator.generate();
    }

    private RepoGenerator constructRepoGenerator() {
        RepositoryResourceBundle resourceBundle = new RepositoryResourceBundle();
        resourceBundle.setName(this.name);
        resourceBundle.setMetadataRepository(this.metadataRepository);
        resourceBundle.setArtifactRepository(artifactRepository);
        resourceBundle.setFeatureArtifacts(this.featureArtifacts);
        resourceBundle.setBundleArtifacts(this.bundleArtifacts);
        resourceBundle.setCategories(this.categories);
        resourceBundle.setPublishArtifacts(this.publishArtifacts);
        resourceBundle.setPublishArtifactRepository(this.publishArtifactRepository);
        resourceBundle.setProject(this.project);
        resourceBundle.setArchive(this.archive);
        resourceBundle.setRepositorySystem(this.repositorySystem);
        resourceBundle.setLocalRepository(this.localRepository);
        resourceBundle.setRemoteRepositories(this.remoteRepositories);
        resourceBundle.setLauncher(this.launcher);
        resourceBundle.setForkedProcessTimeoutInSeconds(this.forkedProcessTimeoutInSeconds);
        resourceBundle.setLog(getLog());

        RepoGenerator generator = new RepoGenerator(resourceBundle);
        generator.setLog(getLog());
        return generator;
    }


}
