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

import java.io.FileInputStream;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.w3c.dom.Document;
import org.wso2.maven.p2.FeatureArtifact;
import org.wso2.maven.p2.P2Profile;
import org.wso2.maven.p2.generate.feature.Bundle;

public class MavenUtils {

	public static Artifact getResolvedArtifact(Bundle bundle, ArtifactFactory artifactFactory, List remoteRepositories, ArtifactRepository localRepository, ArtifactResolver resolver) throws MojoExecutionException{
		Artifact artifact = artifactFactory.createArtifact(bundle.getGroupId(),bundle.getArtifactId(),bundle.getVersion(),Artifact.SCOPE_RUNTIME,"jar");
		try {
			resolver.resolve(artifact,remoteRepositories,localRepository);
		} catch (ArtifactResolutionException e) {
			throw new MojoExecutionException("ERROR",e); 
		} catch (ArtifactNotFoundException e) {
			throw new MojoExecutionException("ERROR",e); 
		}
		return artifact;
	}
	
	public static Artifact getResolvedArtifact(FeatureArtifact featureArtifact, ArtifactFactory artifactFactory, List remoteRepositories, ArtifactRepository localRepository, ArtifactResolver resolver) throws MojoExecutionException{
		Artifact artifact = artifactFactory.createArtifact(featureArtifact.getGroupId(),featureArtifact.getArtifactId(),featureArtifact.getVersion(),Artifact.SCOPE_RUNTIME,"zip");
		try {
			resolver.resolve(artifact,remoteRepositories,localRepository);
		} catch (ArtifactResolutionException e) {
			throw new MojoExecutionException("ERROR",e); 
		} catch (ArtifactNotFoundException e) {
			throw new MojoExecutionException("ERROR",e); 
		}
		return artifact;
	}
	
	public static Artifact getResolvedArtifact(P2Profile p2Profile, ArtifactFactory artifactFactory, List remoteRepositories, ArtifactRepository localRepository, ArtifactResolver resolver) throws MojoExecutionException{
		Artifact artifact = artifactFactory.createArtifact(p2Profile.getGroupId(),p2Profile.getArtifactId(),p2Profile.getVersion(),Artifact.SCOPE_RUNTIME,"zip");
		try {
			resolver.resolve(artifact,remoteRepositories,localRepository);
		} catch (ArtifactResolutionException e) {
			throw new MojoExecutionException("ERROR",e); 
		} catch (ArtifactNotFoundException e) {
			throw new MojoExecutionException("ERROR",e); 
		}
		return artifact;
	}

    public static Artifact getResolvedArtifact(Artifact artifact, List remoteRepositories, ArtifactRepository localRepository, ArtifactResolver resolver) throws MojoExecutionException{
		try {
			resolver.resolve(artifact,remoteRepositories,localRepository);
		} catch (ArtifactResolutionException e) {
			throw new MojoExecutionException("ERROR",e);
		} catch (ArtifactNotFoundException e) {
			throw new MojoExecutionException("ERROR",e);
		}
		return artifact;
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
