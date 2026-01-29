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
package org.wso2.maven.p2.generate.feature;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.wso2.maven.p2.generate.utils.P2Utils;

public class ImportFeature{

	/**
     * Feature Id of the feature
     */
    @Parameter
	private String featureId;

	/**
     * Version of the feature
     */
    @Parameter(defaultValue = "")
	private String featureVersion;
	
    /**
     * Version Compatibility of the Feature
     */
	@Parameter
	private String compatibility;

	private Artifact artifact;

    private boolean isOptional;

	public void setFeatureId(String featureId) {
		this.featureId = featureId;
	}

	public String getFeatureId() {
		return featureId;
	}

	public void setCompatibility(String compatibility) {
		this.compatibility = compatibility;
	}

	public String getCompatibility() {
		return compatibility;
	}

    public boolean isOptional() {
        return isOptional;
    }

    public void setOptional(boolean optional) {
        isOptional = optional;
    }

    protected static ImportFeature getFeature(String featureDefinition) throws MojoExecutionException{
    	if (featureDefinition == null || featureDefinition.trim().isEmpty()) {
    		throw new MojoExecutionException("Feature definition must be non-empty");
    	}
    	String[] split = featureDefinition.split(":");
		ImportFeature feature=new ImportFeature();
		if (split.length>0){
			feature.setFeatureId(split[0]);
			String match="equivalent";
			if (split.length>1){
				if (P2Utils.isMatchString(split[1])){
					match=split[1].toUpperCase();
                    if(match.equalsIgnoreCase("optional"))
                        feature.setOptional(true);
					if (split.length>2)
						feature.setFeatureVersion(split[2]);
				}else{
					feature.setFeatureVersion(split[1]);
					if (split.length>2) {
						if  (P2Utils.isMatchString(split[2])) {
                            match=split[2].toUpperCase();
                            if(match.equalsIgnoreCase("optional"))
                                feature.setOptional(true);
                        }
                    }
				}
			}
			feature.setCompatibility(match);
			return feature;
		}
		throw new MojoExecutionException("Insufficient feature artifact information provided to determine the feature: "+featureDefinition) ; 
	}	

	public void setFeatureVersion(String version) {
        if(featureVersion == null || featureVersion.equals(""))
            featureVersion = Bundle.getOSGIVersion(version);
    }

	public String getFeatureVersion() {
		return featureVersion;
	}
}
