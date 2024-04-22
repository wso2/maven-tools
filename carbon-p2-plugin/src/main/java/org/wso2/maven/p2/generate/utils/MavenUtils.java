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
package org.wso2.maven.p2.generate.utils;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.w3c.dom.Document;
import org.wso2.maven.p2.FeatureArtifact;
import org.wso2.maven.p2.P2Profile;
import org.wso2.maven.p2.generate.feature.Bundle;

public class MavenUtils {
	
	private static Artifact performResolution(Log log, RepoSystemHolder repoRefs, Artifact artifact) throws MojoExecutionException {
		
//		LocalRepositoryManager lrm = repoSession.getLocalRepositoryManager();
//		LocalArtifactRequest localReq = new LocalArtifactRequest(artifact, remoteRepositories, null);
//		LocalArtifactResult lat = lrm.find(repoSession,localReq);
		//if (log!=null) log.info("Locally available? "+lat.isAvailable());
		
		ArtifactRequest request = new ArtifactRequest();
		request.setArtifact(artifact).setRepositories(repoRefs.getRemoteRepositories());
		//if (log!=null) log.info("Current Artifact Request: "+request);
		ArtifactResult result = null; 
		try {
			result = repoRefs.getRepoSystem().resolveArtifact( repoRefs.getRepoSession(), request );
		} catch (ArtifactResolutionException e) {
			if (log!=null) log.error("Resolution error: "+e.getMessage());
			throw new MojoExecutionException("ERROR: "+e.getMessage(),e); 
		}
		//if (log!=null) log.info("result.isResolved() = "+result.isResolved());
		//TODO: use result.isResolved() to check the outcome of resolution
		return result.getArtifact();
	}

	public static Artifact getResolvedArtifact(RepoSystemHolder repoRefs, Bundle bundle) throws MojoExecutionException{
		Artifact artifact = new DefaultArtifact(bundle.getGroupId(), bundle.getArtifactId(), null /*org.apache.maven.artifact.Artifact.SCOPE_RUNTIME*/, "jar", bundle.getVersion());
		return MavenUtils.performResolution(null, repoRefs, artifact);
	}
	
	public static Artifact getResolvedArtifact(Log log, RepoSystemHolder repoRefs, FeatureArtifact featureArtifact) throws MojoExecutionException{
		Artifact artifact = new DefaultArtifact(featureArtifact.getGroupId(), featureArtifact.getArtifactId(), null /*org.apache.maven.artifact.Artifact.SCOPE_RUNTIME*/, "zip", featureArtifact.getVersion());
		return MavenUtils.performResolution(log, repoRefs, artifact);
	}
	
	public static Artifact getResolvedArtifact(Log log, RepoSystemHolder repoRefs, P2Profile p2Profile) throws MojoExecutionException{
		Artifact artifact = new DefaultArtifact(p2Profile.getGroupId(), p2Profile.getArtifactId(), null /*org.apache.maven.artifact.Artifact.SCOPE_RUNTIME*/, "zip", p2Profile.getVersion());
		return MavenUtils.performResolution(log, repoRefs, artifact);
	}

    public static Artifact getResolvedArtifact(Log log, RepoSystemHolder repoRefs, Artifact artifact) throws MojoExecutionException{
    	return MavenUtils.performResolution(log, repoRefs, artifact);
	}
    
    public static Document getManifestDocument() throws MojoExecutionException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder;
        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e1) {
            throw new MojoExecutionException("Unable to load feature manifest", e1);
        }
        Document document;
        document = documentBuilder.newDocument();
        return document;
    }
}
