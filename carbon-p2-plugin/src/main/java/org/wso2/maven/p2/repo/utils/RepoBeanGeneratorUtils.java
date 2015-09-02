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
import org.wso2.maven.p2.BundleArtifact;
import org.wso2.maven.p2.FeatureArtifact;
import org.wso2.maven.p2.repo.RepositoryResourceBundle;
import org.wso2.maven.p2.utils.BundleUtils;
import org.wso2.maven.p2.utils.MavenUtils;

import java.util.ArrayList;

public class RepoBeanGeneratorUtils {

    private final RepositoryResourceBundle resourceBundle;

    public RepoBeanGeneratorUtils(RepositoryResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
    }

    public ArrayList<FeatureArtifact> getProcessedFeatureArtifacts() throws MojoExecutionException {
        ArrayList<FeatureArtifact> processedFeatureArtifacts = new ArrayList<FeatureArtifact>();
        if(resourceBundle.getFeatureArtifacts() == null) {
            return processedFeatureArtifacts;
        }
        for (Object obj : resourceBundle.getFeatureArtifacts()) {
            FeatureArtifact f = null;
            try {
                if (obj instanceof FeatureArtifact) {
                    f = (FeatureArtifact) obj;
                } else if (obj instanceof String) {
                    f = FeatureArtifact.getFeatureArtifact(obj.toString());
                } else
                    f = (FeatureArtifact) obj;
                f.resolveVersion(this.resourceBundle.getProject());
                f.setArtifact(MavenUtils.getResolvedArtifact(f, resourceBundle.getArtifactFactory(), resourceBundle.getRemoteRepositories(), resourceBundle.getLocalRepository(), resourceBundle.getResolver()));
                processedFeatureArtifacts.add(f);
            } catch (Exception e) {
                throw new MojoExecutionException("Error occurred when processing the Feature Artifact: " + obj.toString(), e);
            }
        }
        return processedFeatureArtifacts;
    }

    public ArrayList<BundleArtifact> getProcessedBundleArtifacts() throws MojoExecutionException {
        ArrayList<BundleArtifact> processedBundleArtifacts = new ArrayList<BundleArtifact>();
        if(resourceBundle.getBundleArtifacts() == null) {
            return processedBundleArtifacts;
        }
        for (Object obj : resourceBundle.getBundleArtifacts()) {
            BundleArtifact f;
            if (obj instanceof BundleArtifact) {
                f = (BundleArtifact) obj;
            } else if (obj instanceof String) {
                f = BundleArtifact.getBundleArtifact(obj.toString());
            } else {
                f = (BundleArtifact) obj;
            }
            BundleUtils.resolveVersionForBundle(f, this.resourceBundle.getProject());// f.resolveVersion(getProject());
            f.setArtifact(MavenUtils.getResolvedArtifact(f, resourceBundle.getArtifactFactory(), resourceBundle.getRemoteRepositories(), resourceBundle.getLocalRepository(), resourceBundle.getResolver()));
            processedBundleArtifacts.add(f);
        }
        return processedBundleArtifacts;
    }
}
