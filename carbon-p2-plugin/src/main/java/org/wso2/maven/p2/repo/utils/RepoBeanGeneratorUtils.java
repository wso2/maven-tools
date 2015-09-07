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

import org.apache.maven.plugin.MojoExecutionException;
import org.wso2.maven.p2.beans.Bundle;
import org.wso2.maven.p2.beans.FeatureArtifact;
import org.wso2.maven.p2.repo.RepositoryResourceBundle;
import org.wso2.maven.p2.utils.BundleUtils;
import org.wso2.maven.p2.utils.FeatureUtils;
import org.wso2.maven.p2.utils.MavenUtils;

import java.util.ArrayList;

/**
 * This class takes the configuration data entered into the plugin and cast it to the internal
 * data representation. This sole purpose of this is to reduce the complexity of the RepoGenerator.java.
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
     * @throws MojoExecutionException
     */
    public ArrayList<FeatureArtifact> getProcessedFeatureArtifacts() throws MojoExecutionException {
        ArrayList<FeatureArtifact> processedFeatureArtifacts = new ArrayList<FeatureArtifact>();
        if (resourceBundle.getFeatureArtifacts() == null) {
            return processedFeatureArtifacts;
        }
        for (Object obj : resourceBundle.getFeatureArtifacts()) {
            try {
                FeatureArtifact f = null;
                if (obj instanceof String) {
                    f = FeatureUtils.getFeatureArtifact(obj.toString());
                } else {
                    f = (FeatureArtifact) obj;
                }
                FeatureUtils.resolveVersion(f, this.resourceBundle.getProject());
                f.setArtifact(MavenUtils.getResolvedArtifact(f, resourceBundle.getRepositorySystem(),
                        resourceBundle.getRemoteRepositories(), resourceBundle.getLocalRepository()));
                processedFeatureArtifacts.add(f);
            } catch (Exception e) {
                throw new MojoExecutionException("Error occurred when processing the Feature Artifact: " +
                        obj.toString(), e);
            }
        }
        return processedFeatureArtifacts;
    }

    /**
     * Generates an ArrayList<Bundle> from feature artifact data passed into plugin through plugin
     * configuration in pom.xml
     *
     * @return processed bundles in an ArrayList&lt;Bundle&gt;
     * @throws MojoExecutionException
     */
    public ArrayList<Bundle> getProcessedBundleArtifacts() throws MojoExecutionException {
        ArrayList<Bundle> processedBundleArtifacts = new ArrayList<Bundle>();
        if (resourceBundle.getBundleArtifacts() == null) {
            return processedBundleArtifacts;
        }
        for (Object obj : resourceBundle.getBundleArtifacts()) {
            Bundle f;
            if (obj instanceof String) {
                f = BundleUtils.getBundleArtifact(obj.toString());
            } else {
                f = (Bundle) obj;
            }
            BundleUtils.resolveVersionForBundle(f, this.resourceBundle.getProject());
            f.setArtifact(MavenUtils.getResolvedArtifact(f, resourceBundle.getRepositorySystem(),
                    resourceBundle.getRemoteRepositories(), resourceBundle.getLocalRepository()));
            processedBundleArtifacts.add(f);
        }
        return processedBundleArtifacts;
    }
}
