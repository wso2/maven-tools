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

import java.net.URL;

import org.apache.maven.plugins.annotations.Parameter;

public class P2Repository {
    /**
     * URL of the Metadata Repository
     */
	@Parameter
    private URL metadataRepository;

    /**
     * URL of the Artifact Repository
     */
	@Parameter
    private URL artifactRepository;
    
    /**
     * URL of the P2 Repository
     */
	@Parameter
    private URL repository;
    
    /**
     * Generate P2 Repository on the fly
     */
	@Parameter
    private RepositoryGenMojo generateRepo;

	public void setGenerateRepo(RepositoryGenMojo generateRepo) {
		this.generateRepo = generateRepo;
	}

	public RepositoryGenMojo getGenerateRepo() {
		return generateRepo;
	}

	public void setRepository(URL repository) {
		this.repository = repository;
	}

	public URL getRepository() {
		return repository;
	}

	public void setArtifactRepository(URL artifactRepository) {
		this.artifactRepository = artifactRepository;
	}

	public URL getArtifactRepository() {
		return artifactRepository;
	}

	public void setMetadataRepository(URL metadataRepository) {
		this.metadataRepository = metadataRepository;
	}

	public URL getMetadataRepository() {
		return metadataRepository;
	} 
}
