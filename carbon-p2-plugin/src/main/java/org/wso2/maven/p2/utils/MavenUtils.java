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
package org.wso2.maven.p2.utils;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.repository.RepositorySystem;
import org.wso2.maven.p2.beans.CarbonArtifact;

import java.util.List;

public class MavenUtils {

    /**
     * Returns an artifact which represented by a the given CarbonArtifact.
     *
     * @param carbonArtifact     carbon artifact whose maven artifact needs to be resolved
     * @param repositorySystem   RepositorySystem object
     * @param remoteRepositories collection of remote repositories
     * @param localRepository    local repository representation
     * @return resolved maven artifact
     */
    public static Artifact getResolvedArtifact(CarbonArtifact carbonArtifact,
                                               RepositorySystem repositorySystem,
                                               List<ArtifactRepository> remoteRepositories,
                                               ArtifactRepository localRepository) {

        return getResolvedArtifact(carbonArtifact.getGroupId(), carbonArtifact.getArtifactId(),
                carbonArtifact.getVersion(), repositorySystem, remoteRepositories, localRepository,
                carbonArtifact.getType());

    }

    private static Artifact getResolvedArtifact(String groupId, String artifactId, String version,
                                                RepositorySystem repositorySystem,
                                                List<ArtifactRepository> remoteRepositories,
                                                ArtifactRepository localRepository, String type) {

        Artifact artifact = repositorySystem.createArtifact(groupId, artifactId, version, Artifact.SCOPE_RUNTIME, type);

        ArtifactResolutionRequest request = new ArtifactResolutionRequest();
        request.setArtifact(artifact);
        request.setLocalRepository(localRepository);
        request.setRemoteRepositories(remoteRepositories);
        repositorySystem.resolve(request);

        return artifact;
    }
}
