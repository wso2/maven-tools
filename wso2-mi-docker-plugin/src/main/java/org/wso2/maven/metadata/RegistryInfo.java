/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.maven.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class RegistryInfo {

    public static final String FILE = "file";
    public static final String ITEM = "item";
    public static final String DUMP = "dump";
    public static final String COLLECTION = "collection";
    public static final String ASSOCIATION = "association";
    public static final String PATH = "path";
    public static final String DIRECTORY = "directory";
    public static final String SOURCE_PATH = "sourcePath";
    public static final String TARGET_PATH = "targetPath";
    public static final String TYPE = "type";
    public static final String REGISTRY_TYPE = "registry-type";
    public static final String MEDIA_TYPE = "mediaType";

    private final List<Resource> resources = new ArrayList<>();
    private final List<Dump> dumps = new ArrayList<>();
    private final List<Collection> collections = new ArrayList<>();
    private final List<Association> associations = new ArrayList<>();

    private String extractedPath;
    private String parentArtifactName;
    private String configFileName;
    private String appName;

    public void addCollection(String path, String directory, String registryType, Properties properties) {
        collections.add(new Collection(path, directory, registryType, properties));
    }

    public void addResource(String path, String fileName, String registryType, String mediaType, Properties properties) {
        resources.add(new Resource(path, fileName, registryType, mediaType, properties));
    }

    public void addAssociation(String sourcePath, String targetPath, String type, String registryType) {
        associations.add(new Association(sourcePath, targetPath, type, registryType));
    }

    public void addDump(String path, String directory, String registryType) {
        dumps.add(new Dump(path, directory, registryType));
    }

    public String getExtractedPath() {
        return extractedPath;
    }

    public void setExtractedPath(String extractedPath) {
        this.extractedPath = extractedPath;
    }

    public String getParentArtifactName() {
        return parentArtifactName;
    }

    public void setParentArtifactName(String parentArtifactName) {
        this.parentArtifactName = parentArtifactName;
    }

    public String getConfigFileName() {
        return configFileName;
    }

    public void setConfigFileName(String configFileName) {
        this.configFileName = configFileName;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public List<Resource> getResources() {
        return resources;
    }

    public List<Collection> getCollections() {
        return collections;
    }

    public static class Resource {
        private final String path;
        private final String fileName;
        private final String registryType;
        private final String mediaType;
        private final Properties properties;

        public Resource(String path, String fileName, String regType, String mediaType, Properties properties) {
            this.path = path;
            this.fileName = fileName;
            this.registryType = regType;
            this.mediaType = mediaType;
            this.properties = properties;
        }

        public String getPath() {
            return path;
        }

        public String getFileName() {
            return fileName;
        }

        public String getRegistryType() {
            return registryType;
        }

        public String getMediaType() {
            return mediaType;
        }

        public Properties getProperties() {
            return properties;
        }
    }

    /**
     * This class represents a dump in a the reg-config.xml
     */
    public static class Dump {
        private final String path;
        private final String dumpFileName;
        private final String registryType;

        public Dump(String path, String fileName, String regType) {
            this.path = path;
            this.dumpFileName = fileName;
            this.registryType = regType;
        }

        public String getPath() {
            return path;
        }

        public String getDumpFileName() {
            return dumpFileName;
        }

        public String getRegistryType() {
            return registryType;
        }
    }

    /**
     * This class represents a collection in a the reg-config.xml
     */
    public static class Collection {
        private final String path;
        private final String directory;
        private final String registryType;
        private final Properties properties;

        public Collection(String path, String directory, String regType, Properties properties) {
            this.path = path;
            this.directory = directory;
            this.registryType = regType;
            this.properties = properties;
        }

        public String getPath() {
            return path;
        }

        public String getDirectory() {
            return directory;
        }

        public String getRegistryType() {
            return registryType;
        }

        public Properties getProperties() {
            return properties;
        }
    }

    /**
     * This class represents an association in a the reg-config.xml
     */
    public static class Association {
        private final String sourcePath;
        private final String targetPath;
        private final String associationType;
        private final String registryType;

        public Association(String sourcePath, String targetPath, String assType, String regType) {
            this.sourcePath = sourcePath;
            this.targetPath = targetPath;
            this.associationType = assType;
            this.registryType = regType;
        }

        public String getSourcePath() {
            return sourcePath;
        }

        public String getTargetPath() {
            return targetPath;
        }

        public String getAssociationType() {
            return associationType;
        }

        public String getRegistryType() {
            return registryType;
        }
    }
}
