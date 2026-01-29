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
package org.wso2.maven.p2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

public class Category {
	
    /**
     * Category Id
     */
	@Parameter(required = true)
	private String id;
	
    /**
     * Category Label
     */
	@Parameter
	private String label;

	/**
     * Category description
     */
	@Parameter
	private String description;
	
    /**
     * List of features contained in the category
     */
	@Parameter(required = true)
	private ArrayList<CatFeature> features;

	private ArrayList<CatFeature> processedFeatures;

	public ArrayList<CatFeature> getFeatures() {
		return features;
	}
	
    public ArrayList<CatFeature> getProcessedFeatures(MavenProject project, ArtifactFactory artifactFactory, List remoteRepositories, ArtifactRepository localRepository, ArtifactResolver resolver) throws MojoExecutionException{
        if (processedFeatures != null)
            return processedFeatures;
        if (features == null || features.size() == 0) return null;
        processedFeatures = new ArrayList<CatFeature>();
        Iterator<CatFeature> iter = features.iterator();
        while (iter.hasNext()) {
            CatFeature f = (CatFeature)iter.next();       
            processedFeatures.add(f);
            f.replaceProjectKeysInVersion(project); 
        }        
        return processedFeatures;
    }

	public String getId() {
		return id;
	}

	public String getLabel() {
		if (label==null){
			return getId();
		}else{
			return label;
		}
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		if (description==null){
			return getLabel();
		}else{
			return description;
		}
	}
}
