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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.wso2.maven.p2.commons.FeatureResourceBundle;
import org.wso2.maven.p2.feature.AdviceFile;
import org.wso2.maven.p2.feature.Bundle;
import org.wso2.maven.p2.feature.ImportFeature;
import org.wso2.maven.p2.feature.IncludedFeature;
import org.wso2.maven.p2.feature.Property;
import org.wso2.maven.p2.utils.BundleUtils;
import org.wso2.maven.p2.utils.MavenUtils;

import java.util.ArrayList;

/**
 * This class takes the configuration data entered into the plugin and cast it to the internal
 * data representation. This sole purpose of this is to reduce the complexity of the FeatureGenerator.java.
 */
public class InputParamProcessor {

    private java.util.List remoteRepositories;
    private org.apache.maven.artifact.repository.ArtifactRepository localRepository;
    private org.apache.maven.artifact.factory.ArtifactFactory artifactFactory;
    private org.apache.maven.artifact.resolver.ArtifactResolver resolver;
    private MavenProject project;
    private AdviceFile adviceFile;

    public InputParamProcessor(FeatureResourceBundle resourceBundle) {
        this.remoteRepositories = resourceBundle.getRemoteRepositories();
        this.localRepository = resourceBundle.getLocalRepository();
        this.artifactFactory = resourceBundle.getArtifactFactory();
        this.resolver = resourceBundle.getResolver();
        this.project = resourceBundle.getProject();
        this.adviceFile = resourceBundle.getAdviceFile();
    }

    /**
     * Gets an object ArrayList and populate the content into a ArrayList<Bundle> by casting it properly. The object
     * array may contain strings or Bundle objects.
     *
     * @param bundles ArrayList of bundles
     * @param isImportBundles set this true to get processed importBundles
     * @return ArrayList<Bundle>
     * @throws MojoExecutionException
     */
    public ArrayList<Bundle> getProcessedBundlesList(ArrayList bundles, boolean isImportBundles) throws MojoExecutionException {
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

            b.setArtifact(getResolvedArtifact(b));
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
     * Returns the processed ImportFeatures from unprocessed importfeatures.
     * @param importFeatures ArrayList of importFeatures
     * @return ArrayList&lt;ImportFeature&gt;
     * @throws MojoExecutionException
     */
    public ArrayList<ImportFeature> getProcessedImportFeaturesList(ArrayList importFeatures) throws MojoExecutionException {
        if (importFeatures == null || importFeatures.size() == 0) {
            return null;
        }
        ArrayList<ImportFeature> processedImportFeatures = new ArrayList<ImportFeature>();
        for (Object obj : importFeatures) {
            ImportFeature f;
            if (obj instanceof String) {
                f = ImportFeature.getFeature(obj.toString());
            } else {
                throw new MojoExecutionException("Unknown ImportFeature definition: " + obj.toString());
            }
            f.setFeatureVersion(this.project.getVersion());
            processedImportFeatures.add(f);
        }
        return processedImportFeatures;
    }

    /**
     * Returns the processed IncludedFeatures from an ArrayList of row includedFeatures ArrayList.
     * @param includedFeatures ArrayList
     * @return ArrayList&lt;IncludedFeature&gt;
     * @throws MojoExecutionException
     */
    public ArrayList<IncludedFeature> getIncludedFeatures(ArrayList includedFeatures) throws MojoExecutionException {
        if (includedFeatures == null || includedFeatures.size() == 0)
            return null;

        ArrayList<IncludedFeature> processedIncludedFeatures = new ArrayList<IncludedFeature>();
        for (Object obj : includedFeatures) {
            if (obj instanceof String) {
                IncludedFeature includedFeature = IncludedFeature.getIncludedFeature((String) obj);
                if (includedFeature != null) {
                    includedFeature.setFeatureVersion(this.project.getVersion());
                    Artifact artifact = this.artifactFactory.createArtifact(includedFeature.getGroupId(),
                            includedFeature.getArtifactId(), includedFeature.getArtifactVersion(),
                            Artifact.SCOPE_RUNTIME, "zip");
                    includedFeature.setArtifact(
                            MavenUtils.getResolvedArtifact(artifact, this.remoteRepositories, this.localRepository, this.resolver));
                    processedIncludedFeatures.add(includedFeature);
                }
            }
        }
        return processedIncludedFeatures;
    }

    /**
     * Returns processed AdviceProperties from the row adviceFile.
     * @return ArrayList&lt;Property&gt;
     * @throws MojoExecutionException
     */
    public ArrayList<Property> getProcessedAdviceProperties() throws MojoExecutionException {
        ArrayList<Property> processedAdviceProperties = new ArrayList<Property>();
        if (adviceFile != null && adviceFile.getProperties() != null) {
            for (Object property : adviceFile.getProperties()) {
                Property prop;
                if (property instanceof String) {
                    prop = Property.getProperty(property.toString());
                } else {
                    throw new MojoExecutionException("Unknown advice property definition: " + property.toString());
                }
                processedAdviceProperties.add(prop);
            }
        }
        return processedAdviceProperties;
    }

    private Artifact getResolvedArtifact(Bundle bundle) throws MojoExecutionException {
        Artifact artifact = this.artifactFactory.createArtifact(bundle.getGroupId(), bundle.getArtifactId(), bundle.getVersion(), Artifact.SCOPE_RUNTIME, "jar");
        try {
            this.resolver.resolve(artifact, this.remoteRepositories, this.localRepository);
        } catch (ArtifactResolutionException e) {
            throw new MojoExecutionException("ERROR", e);
        } catch (ArtifactNotFoundException e) {
            throw new MojoExecutionException("ERROR", e);
        }
        return artifact;
    }

}
