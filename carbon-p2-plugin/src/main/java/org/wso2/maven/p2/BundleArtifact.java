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

import org.apache.maven.plugin.MojoExecutionException;
import org.wso2.maven.p2.generate.feature.Bundle;

public class BundleArtifact extends Bundle {
	protected static BundleArtifact getBundleArtifact(String bundleArtifactDefinition, BundleArtifact bundleArtifact) throws MojoExecutionException{
		String[] split = bundleArtifactDefinition.split(":");
		if (split.length == 2 || split.length == 3){
			bundleArtifact.setGroupId(split[0].trim());
			bundleArtifact.setArtifactId(split[1].trim());
			if (split.length == 3) {
				bundleArtifact.setVersion(split[2].trim());
			}
			return bundleArtifact;
		}
		throw new MojoExecutionException("Invalid bundle artifact definition (expected groupId:artifactId[:version]): "+bundleArtifactDefinition) ; 
	}
}