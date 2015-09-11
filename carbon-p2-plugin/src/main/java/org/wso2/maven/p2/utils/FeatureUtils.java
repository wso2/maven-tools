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

package org.wso2.maven.p2.utils;

import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.wso2.maven.p2.beans.FeatureArtifact;
import org.wso2.maven.p2.beans.ImportFeature;
import org.wso2.maven.p2.beans.IncludedFeature;
import org.wso2.maven.p2.exceptions.ArtifactVersionNotFoundException;
import org.wso2.maven.p2.exceptions.InvalidBeanDefinitionException;
import org.wso2.maven.p2.profile.Feature;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

public class FeatureUtils {

    /**
     * Generates IncludedFeature bean class from the given string definition for an included feature.
     *
     * @param definition String IncludeFeature definition
     * @return IncludedFeature generated from string included feature definition
     * @throws InvalidBeanDefinitionException
     */
    public static IncludedFeature getIncludedFeature(String definition) throws InvalidBeanDefinitionException {
        IncludedFeature feature;
        String segment;
        String[] segments = definition.split(":");

        if (segments.length >= 2) {
            feature = new IncludedFeature();
            feature.setGroupId(segments[0]);
            feature.setArtifactId(segments[1]);
            if (segments[1].endsWith(".feature")) {
                feature.setFeatureId(segments[1].substring(0, segments[1].lastIndexOf(".feature")));
            }
        } else {
            throw new InvalidBeanDefinitionException("Insufficient IncludeFeature information provided to determine " +
                    "the IncludeFeature");
        }

        if (segments.length >= 3) {
            segment = segments[2];
            if ("optional".equals(segment)) {
                feature.setOptional(true);
            } else {
                feature.setArtifactVersion(segment);
                feature.setFeatureVersion(BundleUtils.getOSGIVersion(segment));
            }
        }

        if (segments.length == 4) {
            segment = segments[3];
            if ("optional".equals(segment)) {
                feature.setOptional(true);
            }
        }
        return feature;
    }

    /**
     * Generates ImportFeature bean class from the given string definition for an import feature.
     * @param featureDefinition String ImportFeature definition
     * @return ImportFeature
     * @throws InvalidBeanDefinitionException
     */
    public static ImportFeature getImportFeature(String featureDefinition) throws InvalidBeanDefinitionException {
        String[] split = featureDefinition.split(":");
        ImportFeature feature = new ImportFeature();
        if (split.length > 0) {
            feature.setFeatureId(split[0]);
            String match = "equivalent";
            if (split.length > 1) {
                if (P2Utils.isMatchString(split[1])) {
                    match = split[1].toUpperCase();
                    if (match.equalsIgnoreCase("optional")) {
                        feature.setOptional(true);
                    }
                    if (split.length > 2) {
                        feature.setFeatureVersion(BundleUtils.getOSGIVersion(split[2]));
                    }
                } else {
                    feature.setFeatureVersion(BundleUtils.getOSGIVersion(split[1]));
                    if (split.length > 2) {
                        if (P2Utils.isMatchString(split[2])) {
                            match = split[2].toUpperCase();
                            if (match.equalsIgnoreCase("optional")) {
                                feature.setOptional(true);
                            }
                        }
                    }
                }
            }
            feature.setCompatibility(match);
            return feature;
        }
        throw new InvalidBeanDefinitionException("Insufficient feature artifact information provided to determine the" +
                " feature: " + featureDefinition);
    }

    /**
     * Generates a Feature bean from the given string definition for a feature.
     *
     * @param featureDefinition String Feature definition
     * @return Feature
     * @throws InvalidBeanDefinitionException
     */
    public static Feature getFeature(String featureDefinition) throws InvalidBeanDefinitionException {
        String[] split = featureDefinition.split(":");
        if (split.length > 1) {
            Feature feature = new Feature();
            feature.setId(split[0]);
            feature.setVersion(split[1]);
            return feature;
        }
        throw new InvalidBeanDefinitionException("Insufficient feature information provided to determine the feature: "
                + featureDefinition);
    }

    /**
     * Generates a FeatureArtifact bean from the given string definition for a feature artifact.
     *
     * @param featureArtifactDefinition String definition for feature artifact
     * @return String feature artifact definition
     * @throws InvalidBeanDefinitionException
     */
    public static FeatureArtifact getFeatureArtifact(String featureArtifactDefinition)
            throws InvalidBeanDefinitionException {
        String[] split = featureArtifactDefinition.split(":");
        if (split.length > 1) {
            FeatureArtifact featureArtifact = new FeatureArtifact();
            featureArtifact.setGroupId(split[0]);
            featureArtifact.setArtifactId(split[1]);
            if (split.length == 3) {
                featureArtifact.setVersion(split[2]);
            }
            return featureArtifact;
        }
        throw new InvalidBeanDefinitionException("Insufficient artifact information provided to determine the feature: "
                + featureArtifactDefinition);
    }

    /**
     * Sets the version of a given feature artifact by analyzing project dependencies.
     *
     * @param feature FeatureArtifact object
     * @param project maven project
     * @throws ArtifactVersionNotFoundException
     */
    public static void resolveVersion(FeatureArtifact feature, MavenProject project)
            throws ArtifactVersionNotFoundException {
        if (feature.getVersion() == null) {
            List<Dependency> dependencies = project.getDependencies();
            for (Dependency dependency : dependencies) {
                if (dependency.getGroupId().equalsIgnoreCase(feature.getGroupId()) && dependency.getArtifactId().
                        equalsIgnoreCase(feature.getArtifactId())) {
                    feature.setVersion(dependency.getVersion());
                }
            }
        }

        if (feature.getVersion() == null) {
            List<Dependency> dependencies = project.getDependencyManagement().getDependencies();
            for (Dependency dependency : dependencies) {
                if (dependency.getGroupId().equalsIgnoreCase(feature.getGroupId()) && dependency.getArtifactId().
                        equalsIgnoreCase(feature.getArtifactId())) {
                    feature.setVersion(dependency.getVersion());
                }
            }
        }
        if (feature.getVersion() == null) {
            throw new ArtifactVersionNotFoundException("Could not find the version for " + feature.getGroupId() + ":" +
                    feature.getArtifactId());
        }
        Properties properties = project.getProperties();
        for (Map.Entry<Object, Object> obj : properties.entrySet()) {
            feature.setVersion(feature.getVersion().replaceAll(Pattern.quote("${" + obj.getKey() + "}"),
                    obj.getValue().toString()));
        }
    }
}
