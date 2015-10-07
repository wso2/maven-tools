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
package org.wso2.maven.p2.feature.generate;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.repository.RepositorySystem;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Properties;

/**
 * Write environment information for the current build to file.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.PACKAGE)
public class FeatureGenMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.artifactId}")
    private String id;

    @Parameter(defaultValue = "${project.version}")
    private String version;

    @Parameter(defaultValue = "${project.name}")
    private String label;

    @Parameter(defaultValue = "${project.description}")
    private String description;

    @Parameter(defaultValue = "%providerName")
    private String providerName;

    @Parameter(defaultValue = "%copyright")
    private String copyright;

    @Parameter(defaultValue = "%licenseURL")
    private String licenceUrl;

    @Parameter(defaultValue = "%license")
    private String licence;

    /**
     * path to manifest file
     */
    @Parameter
    private File manifest;

    /**
     * path to properties file
     */
    @Parameter
    private URL propertyFile;

    /**
     * list of properties precedence over propertyFile
     */
    @Parameter
    private Properties properties;

    /**
     * Collection of bundles
     */
    @Parameter
    private List<Bundle> bundles;

    /**
     * Collection of required Features
     */
    @Parameter
    private List<Feature> importFeatures;

    /**
     * Collection of required Features
     */
    @Parameter
    private List<Feature> includeFeatures;

    /**
     * define advice file content
     */
    @Parameter
    private List<Advice> adviceFileContents;

    @Component
    private RepositorySystem repositorySystem;

    @Parameter(defaultValue = "${localRepository}")
    private ArtifactRepository localRepository;

    @Parameter(defaultValue = "${project.remoteArtifactRepositories}")
    private List<ArtifactRepository> remoteRepositories;

    @Parameter(defaultValue = "${project}")
    private MavenProject project;

    @Component
    private MavenProjectHelper projectHelper;

    public void execute() throws MojoExecutionException, MojoFailureException {
        FeatureGenerator featureGenerator = constructFeatureGenerator();
        featureGenerator.generate();
    }

    /**
     * Generates the FeatureGenerator object in order to generate the feature.
     *
     * @return FeatureGenerator
     */
    public FeatureGenerator constructFeatureGenerator() {
        FeatureResourceBundle resourceBundle = new FeatureResourceBundle();
        resourceBundle.setId(id);
        resourceBundle.setVersion(version);
        resourceBundle.setLabel(label);
        resourceBundle.setDescription(description);
        resourceBundle.setProviderName(providerName);
        resourceBundle.setCopyright(copyright);
        resourceBundle.setLicence(licence);
        resourceBundle.setLicenceUrl(licenceUrl);
        resourceBundle.setManifest(manifest);
        resourceBundle.setPropertyFile(propertyFile);
        resourceBundle.setProperties(properties);
        resourceBundle.setBundles(bundles);
        resourceBundle.setImportFeatures(importFeatures);
        resourceBundle.setIncludeFeatures(includeFeatures);
        resourceBundle.setAdviceFileContent(adviceFileContents);
        resourceBundle.setRepositorySystem(repositorySystem);
        resourceBundle.setLocalRepository(localRepository);
        resourceBundle.setRemoteRepositories(remoteRepositories);
        resourceBundle.setProject(project);
        resourceBundle.setProjectHelper(projectHelper);
        resourceBundle.setLog(getLog());
        return new FeatureGenerator(resourceBundle);
    }


}
