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
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.p2.facade.internal.P2ApplicationLauncher;
import org.wso2.maven.p2.BundleArtifact;
import org.wso2.maven.p2.EquinoxLauncher;
import org.wso2.maven.p2.FeatureArtifact;
import org.wso2.maven.p2.P2Profile;
import org.wso2.maven.p2.utils.BundleUtils;
import org.wso2.maven.p2.utils.FileManagementUtil;
import org.wso2.maven.p2.utils.MavenUtils;
import org.wso2.maven.p2.utils.P2Utils;

/**
 * Write environment information for the current build to file.
 *
 * @goal p2-repo-gen
 * @phase package
 */
public class RepositoryGenMojo extends AbstractMojo {

//    /**
//     * URL of the Metadata Repository
//     *
//     * @parameter
//     */
//    private URL repository;

    /**
     * Name of the repository
     *
     * @parameter
     */
    private String name;

    /**
     * URL of the Metadata Repository
     *
     * @parameter
     */
    private URL metadataRepository;

    /**
     * URL of the Artifact Repository
     *
     * @parameter
     */
    private URL artifactRepository;

    /**
     * Source folder
     *
     * @parameter
     * @required
     */
    private ArrayList featureArtifacts;

    /**
     * Source folder
     *
     * @parameter
     */
    private ArrayList bundleArtifacts;

    /**
     * Source folder
     *
     * @parameter
     */
    private ArrayList categories;

    /**
     * flag indicating whether the artifacts should be published to the repository. When this flag is not set,
     * the actual bytes underlying the artifact will not be copied, but the repository index will be created.
     * When this option is not specified, it is recommended to set the artifactRepository to be in the same location
     * as the source (-source)
     *
     * @parameter
     */
    private boolean publishArtifacts;

    /**
     * Type of Artifact (War,Jar,etc)
     *
     * @parameter
     */
    private boolean publishArtifactRepository;

    /**
     * Equinox Launcher
     *
     * @parameter
     */
    private EquinoxLauncher equinoxLauncher;


    /**
     * Equinox p2 configuration path
     *
     * @parameter
     */
    private P2Profile p2Profile;

    /**
     * @parameter default-value="${project}"
     */
    private MavenProject project;

    /**
     * @parameter default-value="false"
     */
    private boolean archive;

    /**
     * @component
     */
    private org.apache.maven.artifact.factory.ArtifactFactory artifactFactory;

    /**
     * @component
     */
    private org.apache.maven.artifact.resolver.ArtifactResolver resolver;

    /**
     * @parameter default-value="${localRepository}"
     */
    private org.apache.maven.artifact.repository.ArtifactRepository localRepository;

    /**
     * @parameter default-value="${project.remoteArtifactRepositories}"
     */
    private List remoteRepositories;


    /** @component */
    private P2ApplicationLauncher launcher;

    /**
     * Kill the forked test process after a certain number of seconds. If set to 0, wait forever for
     * the process, never timing out.
     *
     * @parameter expression="${p2.timeout}"
     */
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
        resourceBundle.setEquinoxLauncher(this.equinoxLauncher);
        resourceBundle.setP2Profile(this.p2Profile);
        resourceBundle.setProject(this.project);
        resourceBundle.setArchive(this.archive);
        resourceBundle.setArtifactFactory(this.artifactFactory);
        resourceBundle.setResolver(this.resolver);
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
