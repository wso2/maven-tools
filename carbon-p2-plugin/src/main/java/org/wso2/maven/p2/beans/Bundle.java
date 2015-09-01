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
package org.wso2.maven.p2.beans;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.IOException;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Bean class representing a bundle.
 */
public class Bundle {

    private String groupId;
    private String artifactId;
    private String version;
    private Artifact artifact;
    private String bundleSymbolicName;
    private String bundleVersion;
    private String compatibility;
    private boolean exclude;

    public boolean isExclude() {
        return exclude;
    }

    public void setExclude(boolean exclude) {
        this.exclude = exclude;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupId() {
        return this.groupId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getArtifactId() {
        return this.artifactId;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return this.version;
    }

    public void setCompatibility(String compatibility) {
        this.compatibility = compatibility;
    }

    public String getCompatibility() {
        return this.compatibility;
    }

    public void setArtifact(Artifact artifact) throws MojoExecutionException {
        this.artifact = artifact;
        resolveOSGIInfo();
    }

    public Artifact getArtifact() {
        return this.artifact;
    }

    public String toOSGIString() {
        return getBundleSymbolicName() + ":" + getBundleVersion();
    }

    public void setBundleSymbolicName(String bundleSymbolicName) {
        this.bundleSymbolicName = bundleSymbolicName;
    }

    public String getBundleSymbolicName() {
        return bundleSymbolicName;
    }

    public void setBundleVersion(String bundleVersion) {
        this.bundleVersion = bundleVersion;
    }

    public String getBundleVersion() {
        return bundleVersion;
    }

    public String toString() {
        String bundleString;
        if (getVersion() != null && !getVersion().equalsIgnoreCase("")) {
            bundleString = getGroupId() + ":" + getArtifactId() + ":" + getVersion();
        } else {
            bundleString = getGroupId() + ":" + getArtifactId();
        }
        return bundleString;
    }

    private void resolveOSGIInfo() throws MojoExecutionException {
        String bundleVersionStr = "Bundle-Version";
        String bundleSymbolicNameStr = "Bundle-SymbolicName";
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(getArtifact().getFile());
            Manifest manifest = jarFile.getManifest();
            if (getBundleSymbolicName() == null) {
                String value = manifest.getMainAttributes().getValue(bundleSymbolicNameStr);
                if (value == null) {
                    throw new MojoExecutionException(bundleSymbolicNameStr +
                            " cannot be found in the bundle: " + getArtifact().getFile());
                }
                String[] split = value.split(";");
                setBundleSymbolicName(split[0]);
            }
            if (getBundleVersion() == null) {
                setBundleVersion(manifest.getMainAttributes().getValue(bundleVersionStr));
            }
            if (getBundleSymbolicName() == null || getBundleVersion() == null) {
                throw new MojoExecutionException("Artifact doesn't contain OSGI info: " + getGroupId() + ":" +
                        getArtifactId() + ":" + getVersion());
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to retrieve OSGI bundle info: " + getGroupId() +
                    ":" + getArtifactId() + ":" + getVersion(), e);
        } finally {
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
