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
package org.wso2.maven.p2.repo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import java.util.ArrayList;

public class Category {

    /**
     * Category Id
     *
     * @parameter
     * @required
     */
    private String id;

    /**
     * Category Label
     *
     * @parameter
     */
    private String label;

    /**
     * Category description
     *
     * @parameter
     */
    private String description;

    /**
     * List of features contained in the category
     *
     * @parameter
     * @required
     */
    private ArrayList<CatFeature> features;

    private ArrayList<CatFeature> processedFeatures;

    public ArrayList<CatFeature> getFeatures() {
        return features;
    }

    public ArrayList<CatFeature> getProcessedFeatures(MavenProject project) throws MojoExecutionException {
        if (processedFeatures != null) {
            return processedFeatures;
        }
        if (features == null || features.size() == 0) {
            return null;
        }
        processedFeatures = new ArrayList<CatFeature>();
        for (CatFeature f : features) {
            processedFeatures.add(f);
            f.replaceProjectKeysInVersion(project);
        }
        return processedFeatures;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        if (label == null) {
            return getId();
        } else {
            return label;
        }
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        if (description == null) {
            return getLabel();
        } else {
            return description;
        }
    }
}
