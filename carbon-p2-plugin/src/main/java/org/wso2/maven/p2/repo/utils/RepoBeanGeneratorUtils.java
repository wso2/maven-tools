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

package org.wso2.maven.p2.repo.utils;

import org.wso2.maven.p2.beans.Bundle;
import org.wso2.maven.p2.beans.FeatureArtifact;
import org.wso2.maven.p2.exceptions.ArtifactVersionNotFoundException;
import org.wso2.maven.p2.exceptions.InvalidBeanDefinitionException;
import org.wso2.maven.p2.exceptions.OSGIInformationExtractionException;
import org.wso2.maven.p2.repo.RepositoryResourceBundle;
import org.wso2.maven.p2.utils.BundleUtils;
import org.wso2.maven.p2.utils.FeatureUtils;
import org.wso2.maven.p2.utils.MavenUtils;

import java.io.IOException;
import java.util.ArrayList;

/**
 * This class takes the configuration data entered into the plugin and cast it to the internal
 * data representation. This sole purpose of this is to reduce the complexity of the RepositoryGenerator.java.
 */
public class RepoBeanGeneratorUtils {

    private final RepositoryResourceBundle resourceBundle;

    public RepoBeanGeneratorUtils(RepositoryResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
    }

    /**
     * Generates an ArrayList<FeatureArtifact> from feature artifact data passed into plugin through plugin
     * configuration in pom.xml
     *
     * @return processed bundles in an ArrayList&lt;FeatureArtifact&gt;
     * @throws InvalidBeanDefinitionException
     * @throws ArtifactVersionNotFoundException
     */
    public ArrayList<FeatureArtifact> getProcessedFeatureArtifacts() throws
            InvalidBeanDefinitionException, ArtifactVersionNotFoundException {
        ArrayList<FeatureArtifact> processedFeatureArtifacts = new ArrayList<>();
        if (resourceBundle.getFeatureArtifacts() == null) {
            return processedFeatureArtifacts;
        }
        for (Object obj : resourceBundle.getFeatureArtifacts()) {
            try {
                FeatureArtifact featureArtifact;
                if (obj instanceof String) {
                    featureArtifact = FeatureUtils.getFeatureArtifact(obj.toString());
                } else {
                    featureArtifact = (FeatureArtifact) obj;
                }
                FeatureUtils.resolveVersion(featureArtifact, this.resourceBundle.getProject());
                featureArtifact.setArtifact(MavenUtils.getResolvedArtifact(featureArtifact, resourceBundle.getRepositorySystem(),
                        resourceBundle.getRemoteRepositories(), resourceBundle.getLocalRepository()));
                processedFeatureArtifacts.add(featureArtifact);
            } catch (InvalidBeanDefinitionException | ArtifactVersionNotFoundException e) {
                if(e instanceof InvalidBeanDefinitionException) {
                    throw new InvalidBeanDefinitionException("Error occurred when processing the Feature Artifact: " +
                        obj.toString(), e);
                } else {
                    throw new ArtifactVersionNotFoundException("Error occurred when processing the Feature Artifact: " +
                            obj.toString(), e);
                }
            }
        }
        return processedFeatureArtifacts;
    }

    /**
     * Generates an ArrayList<Bundle> from feature artifact data passed into plugin through plugin
     * configuration in pom.xml
     *
     * @return processed bundles in an ArrayList&lt;Bundle&gt;
     * @throws InvalidBeanDefinitionException
     * @throws ArtifactVersionNotFoundException
     * @throws IOException
     * @throws OSGIInformationExtractionException
     */
    public ArrayList<Bundle> getProcessedBundleArtifacts() throws InvalidBeanDefinitionException,
            ArtifactVersionNotFoundException, IOException, OSGIInformationExtractionException {
        ArrayList<Bundle> processedBundleArtifacts = new ArrayList<>();
        if (resourceBundle.getBundleArtifacts() == null) {
            return processedBundleArtifacts;
        }
        for (Object obj : resourceBundle.getBundleArtifacts()) {
            Bundle bundleArtifact;
            if (obj instanceof String) {
                bundleArtifact = BundleUtils.getBundleArtifact(obj.toString());
            } else {
                bundleArtifact = (Bundle) obj;
            }
            BundleUtils.resolveVersionForBundle(bundleArtifact, this.resourceBundle.getProject());
            bundleArtifact.setArtifact(MavenUtils.getResolvedArtifact(bundleArtifact, resourceBundle.getRepositorySystem(),
                    resourceBundle.getRemoteRepositories(), resourceBundle.getLocalRepository()));
            processedBundleArtifacts.add(bundleArtifact);
        }
        return processedBundleArtifacts;
    }
}
