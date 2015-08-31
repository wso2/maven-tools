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

import org.apache.maven.plugin.MojoExecutionException;
import org.wso2.maven.p2.beans.ImportFeature;
import org.wso2.maven.p2.beans.IncludedFeature;

public class FeatureUtils {

    /**
     * Generates IncludedFeature bean class from the given string definition for an included feature.
     *
     * @param definition String IncludeFeature definition
     * @return IncludedFeature bean
     */
    public static IncludedFeature getIncludedFeature(String definition) {
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
            return null;
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
            if ("optional".equals(segment))
                feature.setOptional(true);
        }

        return feature;
    }

    /**
     * Generates ImportFeature bean class from the given string definition for an import feature.
     *
     * @param featureDefinition String ImportFeature definition
     * @return ImportFeature bean
     */
    public static ImportFeature getImportFeature(String featureDefinition) throws MojoExecutionException {
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
//                        feature.setFeatureVersion(split[2]);
                    }
                } else {
                    feature.setFeatureVersion(BundleUtils.getOSGIVersion(split[1]));
//                    feature.setFeatureVersion(split[1]);
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
        throw new MojoExecutionException("Insufficient feature artifact information provided to determine the feature: "
                + featureDefinition);
    }
}
