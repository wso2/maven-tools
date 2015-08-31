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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.wso2.maven.p2.beans.Bundle;
import org.wso2.maven.p2.beans.ImportFeature;
import org.wso2.maven.p2.beans.IncludedFeature;
import org.wso2.maven.p2.beans.Property;
import org.wso2.maven.p2.feature.AdviceFile;
import org.wso2.maven.p2.feature.FeatureResourceBundle;
import org.wso2.maven.p2.utils.BundleUtils;
import org.wso2.maven.p2.utils.FeatureUtils;
import org.wso2.maven.p2.utils.MavenUtils;
import org.wso2.maven.p2.utils.PropertyUtils;

import java.util.ArrayList;

/**
 * This class takes the configuration data entered into the plugin and cast it to the internal
 * data representation. This sole purpose of this is to reduce the complexity of the FeatureGenerator.java.
 */
public class InputParamProcessor {

    private java.util.List remoteRepositories;
    private org.apache.maven.artifact.repository.ArtifactRepository localRepository;
    private org.apache.maven.repository.RepositorySystem repositorySystem;
    private MavenProject project;
    private AdviceFile adviceFile;
    private FeatureResourceBundle resourceBundle;

    /**
     * Constructs the InputParamProcessor by taking the feature resource bundle.
     *
     * @param resourceBundle FeatureResourceBundle contains all the utility objects and data structures needed
     *                       for processing input values.
     */
    public InputParamProcessor(FeatureResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
        this.remoteRepositories = resourceBundle.getRemoteRepositories();
        this.localRepository = resourceBundle.getLocalRepository();
        this.repositorySystem = resourceBundle.getRepositorySystem();
        this.project = resourceBundle.getProject();
        this.adviceFile = resourceBundle.getAdviceFile();
    }

    /**
     * Generates processed bundles taken from pom.xml configuration.
     *
     * @return processed bundles in an ArrayList&lt;Bundle&gt;
     * @throws MojoExecutionException
     */
    public ArrayList<Bundle> getProcessedBundlesList() throws MojoExecutionException {
        return getProcessedBundlesList(this.resourceBundle.getBundles(), false);
    }

    /**
     * Generates processed import bundles taken from pom.xml configuration.
     *
     * @return processed import bundles in an ArrayList&lt;Bundle&gt;
     * @throws MojoExecutionException
     */
    public ArrayList<Bundle> getProcessedImportBundlesList() throws MojoExecutionException {
        return getProcessedBundlesList(this.resourceBundle.getImportBundles(), true);
    }

    /**
     * Gets an object ArrayList and populate the content into a ArrayList<Bundle> by casting it properly. The object
     * array may contain strings or Bundle objects.
     *
     * @param bundles         ArrayList of bundles
     * @param isImportBundles set this true to get processed importBundles
     * @return ArrayList<Bundle>
     * @throws MojoExecutionException
     */
    private ArrayList<Bundle> getProcessedBundlesList(ArrayList bundles, boolean isImportBundles)
            throws MojoExecutionException {
        if (bundles == null || bundles.size() == 0) {
            return new ArrayList<Bundle>();
        }
        ArrayList<Bundle> processedBundles = new ArrayList<Bundle>();
        for (Object obj : bundles) {
            Bundle b;
            if (obj instanceof String) {
                b = BundleUtils.getBundle(obj.toString());
            } else {
                throw new MojoExecutionException("Unknown bundle definition: " + obj.toString());
            }
            BundleUtils.resolveVersionForBundle(b, this.project);
            b.setArtifact(MavenUtils.getResolvedArtifact(b, this.repositorySystem, this.remoteRepositories,
                    this.localRepository));

            if (isImportBundles) {
                //TODO: The code throws an nullpointer exception when isExclude is true. Check with SameeraJ.
//            if (!b.isExclude()) {
//                b.setArtifact(getResolvedArtifact(b));
//            } else {
//                b.resolveOSGIInfo();
//            }
            }
            processedBundles.add(b);
        }
        return processedBundles;
    }

    /**
     * Generates the processed ImportFeatures from pom.xml configuration.
     *
     * @return processed import features in an ArrayList&lt;ImportFeature&gt;
     * @throws MojoExecutionException
     */
    public ArrayList<ImportFeature> getProcessedImportFeaturesList() throws MojoExecutionException {
        ArrayList importFeatures = this.resourceBundle.getImportFeatures();
        if (importFeatures == null || importFeatures.size() == 0) {
            return new ArrayList<ImportFeature>();
        }
        ArrayList<ImportFeature> processedImportFeatures = new ArrayList<ImportFeature>();
        for (Object obj : importFeatures) {
            ImportFeature f;
            if (obj instanceof String) {
                f = FeatureUtils.getImportFeature(obj.toString());
            } else {
                throw new MojoExecutionException("Unknown ImportFeature definition: " + obj.toString());
            }
            f.setFeatureVersion(BundleUtils.getOSGIVersion(this.project.getVersion()));
            processedImportFeatures.add(f);
        }
        return processedImportFeatures;
    }

    /**
     * Generates the processed IncludedFeatures from an pom.xml configuration
     *
     * @return processed included features in an ArrayList&lt;IncludedFeature&gt;
     * @throws MojoExecutionException
     */
    public ArrayList<IncludedFeature> getIncludedFeatures() throws MojoExecutionException {
        ArrayList includedFeatures = this.resourceBundle.getIncludedFeatures();
        if (includedFeatures == null || includedFeatures.size() == 0) {
            return new ArrayList<IncludedFeature>();
        }

        ArrayList<IncludedFeature> processedIncludedFeatures = new ArrayList<IncludedFeature>();
        for (Object obj : includedFeatures) {
            if (obj instanceof String) {
                IncludedFeature includedFeature = FeatureUtils.getIncludedFeature((String) obj);
                if (includedFeature != null) {
                    includedFeature.setFeatureVersion(this.project.getVersion());
                    includedFeature.setArtifact(MavenUtils.getResolvedArtifact(includedFeature, this.repositorySystem,
                            this.remoteRepositories, this.localRepository));
                    processedIncludedFeatures.add(includedFeature);
                }
            }
        }
        return processedIncludedFeatures;
    }

    /**
     * Returns processed AdviceProperties from the row adviceFile.
     *
     * @return ArrayList&lt;Property&gt;
     * @throws MojoExecutionException
     */
    public ArrayList<Property> getProcessedAdviceProperties() throws MojoExecutionException {
        ArrayList<Property> processedAdviceProperties = new ArrayList<Property>();
        if (adviceFile != null && adviceFile.getProperties() != null) {
            for (Object property : adviceFile.getProperties()) {
                Property prop;
                if (property instanceof String) {
                    prop = PropertyUtils.getProperty(property.toString());
                } else {
                    throw new MojoExecutionException("Unknown advice property definition: " + property.toString());
                }
                processedAdviceProperties.add(prop);
            }
        }
        return processedAdviceProperties;
    }

}
