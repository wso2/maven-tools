/*
*  Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.maven.analytics.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.wso2.maven.analytics.AnalyticsArtifact;
import org.wso2.maven.analytics.AnalyticsProjectArtifacts;


public class AnalyticsMavenUtils {
	
	private static List<String> excludeList=new ArrayList<String>();

	static {
		excludeList.add(".svn");
	}
	
	public static List<AnalyticsArtifact> retrieveArtifacts(File path){
		return retrieveArtifacts(new File(path, "artifact.xml"), new ArrayList<AnalyticsArtifact>());
	}

	private static List<AnalyticsArtifact> retrieveArtifacts(File path,List<AnalyticsArtifact> artifacts){
		if (path.exists()){
			if (path.isFile()){
				AnalyticsProjectArtifacts artifact = new AnalyticsProjectArtifacts();
				try {
					artifact.fromFile(path);
					for (AnalyticsArtifact analyticsArtifact : artifact.getAllAnalyticsArtifacts()) {
						if (analyticsArtifact.getVersion()!=null && analyticsArtifact.getType()!=null){
							artifacts.add(analyticsArtifact);
						}
                    }
				} catch (Exception e) {
					//not an artifact
				}
			}else{
				File[] files = path.listFiles();
				for (File file : files) {
					if (!excludeList.contains(file.getName())) {
	                    retrieveArtifacts(file, artifacts);
                    }
				}
			}
		}
		return artifacts;
	}

}
