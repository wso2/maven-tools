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

import java.util.List;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

public class RepoSystemHolder {
	
	private final RepositorySystem repoSystem;
	private final RepositorySystemSession repoSession;
	private final List<RemoteRepository> remoteRepositories;

	public RepoSystemHolder(RepositorySystem repoSystem, RepositorySystemSession repoSession, List<RemoteRepository> remoteRepositories) {
		this.repoSystem = repoSystem;
		this.repoSession = repoSession;
		this.remoteRepositories = remoteRepositories;
	}

	public RepositorySystem getRepoSystem() {
		return repoSystem;
	}

	public RepositorySystemSession getRepoSession() {
		return repoSession;
	}

	public List<RemoteRepository> getRemoteRepositories() {
		return remoteRepositories;
	}
	
}
