/*
 *  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.maven.p2.feature.utils;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.wso2.maven.p2.beans.Bundle;
import org.wso2.maven.p2.beans.ImportFeature;
import org.wso2.maven.p2.beans.IncludedFeature;
import org.wso2.maven.p2.beans.Property;
import org.wso2.maven.p2.exceptions.ArtifactVersionNotFoundException;
import org.wso2.maven.p2.exceptions.InvalidBeanDefinitionException;
import org.wso2.maven.p2.exceptions.OSGIInformationExtractionException;
import org.wso2.maven.p2.feature.AdviceFile;
import org.wso2.maven.p2.feature.FeatureResourceBundle;
import org.wso2.maven.p2.utils.BundleUtils;
import org.wso2.maven.p2.utils.FeatureUtils;
import org.wso2.maven.p2.utils.MavenUtils;
import org.wso2.maven.p2.utils.PropertyUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Takes the configuration data entered into the plugin through pom.xml and cast it to the internal bean
 * representation. The sole purpose of this is to reduce the complexity of the FeatureGenerator.java.
 */
public class FeatureBeanGeneratorUtils {

    private List<ArtifactRepository> remoteRepositories;
    private ArtifactRepository localRepository;
    private RepositorySystem repositorySystem;
    private MavenProject project;
    private AdviceFile adviceFile;
    private FeatureResourceBundle resourceBundle;

    /**
     * Constructs the FeatureBeanGeneratorUtils by taking the feature resource bundle.
     *
     * @param resourceBundle FeatureResourceBundle contains all the utility objects and data structures needed
     *                       for processing input values.
     */
    public FeatureBeanGeneratorUtils(FeatureResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
        this.remoteRepositories = resourceBundle.getRemoteRepositories();
        this.localRepository = resourceBundle.getLocalRepository();
        this.repositorySystem = resourceBundle.getRepositorySystem();
        this.project = resourceBundle.getProject();
        this.adviceFile = resourceBundle.getAdviceFile();
    }

    /**
     * Generates an ArrayList<Bundle> from bundle data passed into plugin through plugin configuration in pom.xml
     * configuration.
     *
     * @return processed bundles in an ArrayList&lt;Bundle&gt;
     * @throws InvalidBeanDefinitionException
     * @throws OSGIInformationExtractionException
     * @throws IOException
     * @throws ArtifactVersionNotFoundException
     */
    public ArrayList<Bundle> getProcessedBundlesList() throws InvalidBeanDefinitionException,
            OSGIInformationExtractionException, IOException, ArtifactVersionNotFoundException {
        return getProcessedBundlesList(this.resourceBundle.getBundles(), false);
    }

//    Had a confusion whether import bundles are actually needed during the code review[01/09/2015]. Thus commented this.
//    /**
//     * Generates processed import bundles taken from pom.xml configuration.
//     *
//     * @return processed import bundles in an ArrayList&lt;Bundle&gt;
//     * @throws MojoExecutionException
//     */
//    public ArrayList<Bundle> getProcessedImportBundlesList() throws MojoExecutionException {
//        return getProcessedBundlesList(this.resourceBundle.getImportBundles(), true);
//    }

    /**
     * Takes an object ArrayList containing bundles data passed into the plugin through plugin configuration in pom.xml
     * and populate the content into an ArrayList<Bundle> by casting it properly.
     *
     * @param bundles         ArrayList of bundles
     * @param isImportBundles set this true to get processed importBundles
     * @return ArrayList<Bundle>
     * @throws InvalidBeanDefinitionException
     * @throws ArtifactVersionNotFoundException
     * @throws IOException
     * @throws OSGIInformationExtractionException
     */
    private ArrayList<Bundle> getProcessedBundlesList(List<String> bundles, boolean isImportBundles)
            throws InvalidBeanDefinitionException, ArtifactVersionNotFoundException, IOException,
            OSGIInformationExtractionException {
        if (bundles == null || bundles.size() == 0) {
            return new ArrayList<>();
        }
        ArrayList<Bundle> processedBundles = new ArrayList<>();
        for (Object obj : bundles) {
            Bundle bundle = BundleUtils.getBundle(obj.toString());
            BundleUtils.resolveVersionForBundle(bundle, this.project);
            bundle.setArtifact(MavenUtils.getResolvedArtifact(bundle, this.repositorySystem, this.remoteRepositories,
                    this.localRepository));

/*            if (isImportBundles) {
                //TODO: The code throws an null pointer exception when isExclude is true. Check with SameeraJ.
            if (!bundle.isExclude()) {
                bundle.setArtifact(getResolvedArtifact(bundle));
            } else {
                bundle.resolveOSGIInfo();
            }
            }*/
            processedBundles.add(bundle);
        }
        return processedBundles;
    }

    /**
     * Generates an ArrayList<ImportFeature> from import feature data passed into the plugin through plugin
     * configuration in pom.xml configuration.
     *
     * @return processed import features in an ArrayList&lt;ImportFeature&gt;
     * @throws InvalidBeanDefinitionException
     */
    public ArrayList<ImportFeature> getProcessedImportFeaturesList() throws InvalidBeanDefinitionException {
        List<String> importFeatures = this.resourceBundle.getImportFeatures();
        if (importFeatures == null || importFeatures.size() == 0) {
            return new ArrayList<>();
        }
        ArrayList<ImportFeature> processedImportFeatures = new ArrayList<>();
        for (String featureString : importFeatures) {
            ImportFeature feature = FeatureUtils.getImportFeature(featureString);
            if(feature.getFeatureVersion() == null || feature.getFeatureVersion().equals("")) {
                feature.setFeatureVersion(BundleUtils.getOSGIVersion(this.project.getVersion()));
            }
            processedImportFeatures.add(feature);
        }
        return processedImportFeatures;
    }

    /**
     * Generates an ArrayList<IncludedFeature> from included feature data passed into the plugin through plugin
     * configuration in pom.xml configuration.
     *
     * @return processed included features in an ArrayList&lt;IncludedFeature&gt;
     * @throws InvalidBeanDefinitionException
     */
    public ArrayList<IncludedFeature> getIncludedFeatures() throws InvalidBeanDefinitionException {
        List<String> includedFeatures = this.resourceBundle.getIncludedFeatures();
        if (includedFeatures == null || includedFeatures.size() == 0) {
            return new ArrayList<>();
        }

        ArrayList<IncludedFeature> processedIncludedFeatures = new ArrayList<>();
        for (String featureString : includedFeatures) {
            IncludedFeature includedFeature = FeatureUtils.getIncludedFeature(featureString);
            if (includedFeature != null) {
                includedFeature.setArtifactVersion(this.project.getVersion());
                if(includedFeature.getFeatureVersion() == null || includedFeature.getFeatureVersion().equals("")) {
                    includedFeature.setFeatureVersion(BundleUtils.getOSGIVersion(this.project.getVersion()));
                }
                includedFeature.setArtifact(MavenUtils.getResolvedArtifact(includedFeature, this.repositorySystem,
                        this.remoteRepositories, this.localRepository));
                processedIncludedFeatures.add(includedFeature);
            }
        }
        return processedIncludedFeatures;
    }

    /**
     * Generates an ArrayList<Property> from advice properties passed into the plugin through plugin
     * configuration in pom.xml configuration.
     *
     * @return ArrayList&lt;Property&gt;
     * @throws InvalidBeanDefinitionException
     */
    public ArrayList<Property> getProcessedAdviceProperties() throws InvalidBeanDefinitionException {
        ArrayList<Property> processedAdviceProperties = new ArrayList<>();
        if (adviceFile != null && adviceFile.getProperties() != null) {
            for (Object property : adviceFile.getProperties()) {
                Property prop;
                if (property instanceof String) {
                    prop = PropertyUtils.getProperty(property.toString());
                } else {
                    throw new InvalidBeanDefinitionException("Unknown advice property definition: " + property.toString());
                }
                processedAdviceProperties.add(prop);
            }
        }
        return processedAdviceProperties;
    }
}
