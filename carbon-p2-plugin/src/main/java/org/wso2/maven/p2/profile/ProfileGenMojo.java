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
package org.wso2.maven.p2.profile;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.p2.facade.internal.P2ApplicationLauncher;
import org.wso2.maven.p2.utils.P2Constants;

import java.net.URL;
import java.util.List;

/**
 * Write environment information for the current build to file.
 *
 */
@Mojo(name = "p2-profile-gen", defaultPhase = LifecyclePhase.PACKAGE)
public class ProfileGenMojo extends AbstractMojo {


    /**
     * Destination to which the features should be installed
     */
    @Parameter(required = true)
    private String destination;

    /**
     * target profile
     */
    @Parameter(required = true)
    private String profile;


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
     * List of features
     */
    @Parameter(required = true)
    private List features;

    /**
     * Flag to indicate whether to delete old profile files
     */
    @Parameter(defaultValue = "true")
    private boolean deleteOldProfileFiles = true;


    @Parameter(defaultValue = "${project}")
    private MavenProject project;

    @Component
    private P2ApplicationLauncher launcher;

    /**
     * Kill the forked test process after a certain number of seconds. If set to 0, wait forever for
     * the process, never timing out.
     */
    @Parameter(defaultValue = "${p2.timeout}")
    private int forkedProcessTimeoutInSeconds;


    public void execute() throws MojoExecutionException, MojoFailureException {
        ProfileGenerator generator = constructProfileGenerator();
        generator.generate();
    }

    private ProfileGenerator constructProfileGenerator() {
        ProfileResourceBundle resourceBundle = new ProfileResourceBundle();
        resourceBundle.setDestination(this.destination);
        resourceBundle.setProfile(this.profile == null ? P2Constants.DEFAULT_PROFILE_ID : this.profile);
        resourceBundle.setMetadataRepository(this.metadataRepository);
        resourceBundle.setArtifactRepository(this.artifactRepository);
        resourceBundle.setFeatures(this.features);
        resourceBundle.setDeleteOldProfileFiles(this.deleteOldProfileFiles);
        resourceBundle.setProject(this.project);
        resourceBundle.setLauncher(this.launcher);
        resourceBundle.setForkedProcessTimeoutInSeconds(this.forkedProcessTimeoutInSeconds);

        ProfileGenerator generator = new ProfileGenerator(resourceBundle);
        generator.setLog(getLog());
        return generator;
    }
}
