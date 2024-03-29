/*
 * Copyright (c) 2023, WSO2 LLC (http://www.wso2.com).
 *
 * WSO2 LLC licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.maven.plugin.dataservice.utils;

import org.wso2.maven.plugin.dataservice.DSSArtifact;
import org.wso2.maven.plugin.dataservice.DSSProjectArtifacts;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DSSMavenUtils {

    private static List<String> excludeList = new ArrayList<String>();

    static {
        excludeList.add(".svn");
    }

    public static List<DSSArtifact> retrieveArtifacts(File path) {

        return retrieveArtifacts(new File(path, "artifact.xml"), new ArrayList<DSSArtifact>());
    }

    private static List<DSSArtifact> retrieveArtifacts(File path, List<DSSArtifact> artifacts) {

        if (path.exists()) {
            if (path.isFile()) {
                DSSProjectArtifacts artifact = new DSSProjectArtifacts();
                try {
                    artifact.fromFile(path);
                    for (DSSArtifact dssArtifact : artifact.getAllDSSArtifacts()) {
                        if (dssArtifact.getVersion() != null && dssArtifact.getType() != null) {
                            artifacts.add(dssArtifact);
                        }
                    }
                } catch (Exception e) {
                    //not an artifact
                }
            } else {
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
