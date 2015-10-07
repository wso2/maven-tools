/*
 *  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.maven.p2.utils;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.wso2.maven.p2.beans.CarbonArtifact;
import org.wso2.maven.p2.exceptions.OSGIInformationExtractionException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * DependencyResolver takes MavenProject object and resolve all the maven dependencies in the maven project into
 * internal bean representations.
 */
public class DependencyResolver {

    /**
     * Resolve the given project dependencies into CarbonArtifact objects. Dependencies are categorized into
     * OSGI bundles and Carbon features.
     *
     * @param project            MavenProject  Maven Project
     * @param repositorySystem   RepositorySystem object
     * @param remoteRepositories collection of remote repositories
     * @param localRepository    local repository representation
     * @return Return a List&lt;HashMap&lt;String, CarbonArtifact&gt;&gt;, 1st item being HashMap&lt;String,
     * CarbonArtifact&gt; containing osgi bundles specified as dependencies and 2nd item being HashMap&lt;String,
     * CarbonArtifact&gt; containing carbon features specified as dependencies.
     * @throws IOException
     * @throws OSGIInformationExtractionException
     */
    public static List<HashMap<String, CarbonArtifact>> getDependenciesForProject(MavenProject project, RepositorySystem
            repositorySystem, List<ArtifactRepository> remoteRepositories, ArtifactRepository localRepository)
            throws IOException, OSGIInformationExtractionException {

        List<HashMap<String, CarbonArtifact>> results = new ArrayList<>();
        HashMap<String, CarbonArtifact> bundles = new HashMap<>();
        HashMap<String, CarbonArtifact> features = new HashMap<>();
        results.add(bundles);
        results.add(features);
        List<Dependency> dependencies = project.getDependencies();

        DependencyManagement dependencyManagement = project.getDependencyManagement();
        if (dependencyManagement != null) {
            dependencies.addAll(dependencyManagement.getDependencies());
        }
        for (Dependency dependency : dependencies) {
            CarbonArtifact carbonArtifact = new CarbonArtifact();
            carbonArtifact.setGroupId(dependency.getGroupId());
            carbonArtifact.setArtifactId(dependency.getArtifactId());
            carbonArtifact.setVersion(dependency.getVersion());
            carbonArtifact.setType(dependency.getType());
            Artifact mavenArtifact = MavenUtils.getResolvedArtifact(carbonArtifact, repositorySystem,
                    remoteRepositories, localRepository);
            carbonArtifact.setArtifact(mavenArtifact);
            String key;
            if (carbonArtifact.getType().equals("jar")) {
                resolveOSGIInfo(carbonArtifact);
                key = carbonArtifact.getSymbolicName() + "_" + carbonArtifact.getBundleVersion();
                bundles.put(key, carbonArtifact);
            } else {
                key = carbonArtifact.getArtifactId() + "_" + carbonArtifact.getVersion();
                features.put(key, carbonArtifact);
            }
        }
        return results;
    }

    private static void resolveOSGIInfo(CarbonArtifact artifact) throws OSGIInformationExtractionException,
            IOException {
        String bundleVersionStr = "Bundle-Version";
        String bundleSymbolicNameStr = "Bundle-SymbolicName";

        try (JarFile jarFile = new JarFile(artifact.getArtifact().getFile())) {

            Manifest manifest = jarFile.getManifest();

            String bundleSymbolicName = manifest.getMainAttributes().getValue(bundleSymbolicNameStr);
            String bundleVersion = manifest.getMainAttributes().getValue(bundleVersionStr);
            if (bundleSymbolicName == null || bundleVersion == null) {
                throw new OSGIInformationExtractionException("Artifact doesn't contain OSGI info: " +
                        artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion());
            }
            String[] split = bundleSymbolicName.split(";");
            artifact.setSymbolicName(split[0]);
            artifact.setBundleVersion(bundleVersion);

        } catch (IOException e) {
            throw new IOException("Unable to retrieve OSGI bundle info: " + artifact.getGroupId() +
                    ":" + artifact.getArtifactId() + ":" + artifact.getVersion(), e);
        }
    }
}
