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
import org.w3c.dom.Document;
import org.wso2.maven.p2.beans.Bundle;
import org.wso2.maven.p2.beans.FeatureArtifact;
import org.wso2.maven.p2.beans.IncludedFeature;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.List;

public class MavenUtils {

    /**
     * Returns an artifact which represent by a bundle
     *
     * @param bundle             bundle which need to resolve the artifact
     * @param repositorySystem   RepositorySystem object
     * @param remoteRepositories collection of remote repositories
     * @param localRepository    local repository representation
     * @return the resolved Artifact
     */
    public static Artifact getResolvedArtifact(Bundle bundle, RepositorySystem repositorySystem,
                                               List<ArtifactRepository> remoteRepositories,
                                               ArtifactRepository localRepository) {
        return getResolvedArtifact(bundle.getGroupId(), bundle.getArtifactId(), bundle.getVersion(), repositorySystem,
                remoteRepositories, localRepository, "jar");
    }

    /**
     * Returns an artifact which represented by a feature.
     *
     * @param feature            which need to resolve the artifact
     * @param repositorySystem   RepositorySystem object
     * @param remoteRepositories collection of remote repositories
     * @param localRepository    local repository representation
     * @return the resolved Artifact
     */
    public static Artifact getResolvedArtifact(IncludedFeature feature, RepositorySystem repositorySystem,
                                               List<ArtifactRepository> remoteRepositories,
                                               ArtifactRepository localRepository) {
        return getResolvedArtifact(feature.getGroupId(), feature.getArtifactId(), feature.getArtifactVersion(),
                repositorySystem, remoteRepositories, localRepository, "zip");
    }

    /**
     * Returns an artifact which represent by a bundle
     *
     * @param feature            bundle which need to resolve the artifact
     * @param repositorySystem   RepositorySystem object
     * @param remoteRepositories collection of remote repositories
     * @param localRepository    local repository representation
     * @return the resolved Artifact
     */
    public static Artifact getResolvedArtifact(FeatureArtifact feature, RepositorySystem repositorySystem,
                                               List<ArtifactRepository> remoteRepositories,
                                               ArtifactRepository localRepository) {
        return getResolvedArtifact(feature.getGroupId(), feature.getArtifactId(), feature.getVersion(), repositorySystem,
                remoteRepositories, localRepository, "zip");
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

    /**
     * Returns a blank Document
     *
     * @return org.w3c.dom.Document object
     * @throws ParserConfigurationException
     */
    public static Document getManifestDocument() throws ParserConfigurationException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder;
        documentBuilder = documentBuilderFactory.newDocumentBuilder();
        return documentBuilder.newDocument();
    }
}
