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
package org.wso2.maven.p2.feature;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Properties;

/**
 * Write environment information for the current build to file.
 *
 * @goal p2-feature-gen
 * @phase package
 */
public class FeatureGenMojo extends AbstractMojo {

    /**
     * feature id
     *
     * @parameter
     * @required
     */
    private String id;

    /**
     * version
     *
     * @parameter default-value="${project.version}"
     */
    private String version;

    /**
     * label of the feature
     *
     * @parameter default-value="${project.name}"
     */
    private String label;

    /**
     * description of the feature
     *
     * @parameter default-value="${project.description}"
     */
    private String description;

    /**
     * provider name
     *
     * @parameter default-value="%providerName"
     */
    private String providerName;

    /**
     * copyrite
     *
     * @parameter default-value="%copyright"
     */
    private String copyright;

    /**
     * licence url
     *
     * @parameter default-value="%licenseURL"
     */
    private String licenceUrl;

    /**
     * licence
     *
     * @parameter default-value="%license"
     */
    private String licence;

    /**
     * path to manifest file
     *
     * @parameter
     */
    private File manifest;

    /**
     * path to properties file
     *
     * @parameter
     */
    private File propertiesFile;

    /**
     * list of properties
     * precedance over propertiesFile
     *
     * @parameter
     */
    private Properties properties;

    /**
     * Collection of bundles
     *
     * @parameter
     */
    private ArrayList bundles;

    /**
     * Collection of import bundles
     *
     * @parameter
     */
    private ArrayList importBundles;

    /**
     * Collection of required Features
     *
     * @parameter
     */
    private ArrayList importFeatures;

    /**
     * Collection of required Features
     *
     * @parameter
     */
    private ArrayList includedFeatures;

    /**
     * define advice file content
     *
     * @parameter
     */
    private AdviceFile adviceFile;

//    /**
//     * define category
//     * @parameter [alias="carbonCategories"]
//     */
    //    private String category;
    //
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
    private java.util.List remoteRepositories;

//    /**
//     * @parameter default-value="${project.distributionManagementArtifactRepository}"
//     */
//    private ArtifactRepository deploymentRepository;

//    /**
//     * @component
//     */
//    private ArtifactMetadataSource artifactMetadataSource;

    /**
     * @parameter default-value="${project}"
     */
    private MavenProject project;

    /**
     * Maven ProjectHelper.
     *
     * @component
     */
    private MavenProjectHelper projectHelper;

    public void execute() throws MojoExecutionException, MojoFailureException {
        FeatureGenerator featureGenerator = constructFeatureGenerator();
        featureGenerator.generate();
    }

    /**
     * Generates the FeatureGenerator object in order to generate the feature.
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
        resourceBundle.setPropertiesFile(propertiesFile);
        resourceBundle.setProperties(properties);
        resourceBundle.setBundles(bundles);
        resourceBundle.setImportBundles(importBundles);
        resourceBundle.setImportFeatures(importFeatures);
        resourceBundle.setIncludedFeatures(includedFeatures);
        resourceBundle.setAdviceFile(adviceFile);
        resourceBundle.setArtifactFactory(artifactFactory);
        resourceBundle.setResolver(resolver);
        resourceBundle.setLocalRepository(localRepository);
        resourceBundle.setRemoteRepositories(remoteRepositories);
        resourceBundle.setProject(project);
        resourceBundle.setProjectHelper(projectHelper);
        resourceBundle.setLog(getLog());

        FeatureGenerator generator = new FeatureGenerator(resourceBundle, getLog());
        return generator;
    }




}
