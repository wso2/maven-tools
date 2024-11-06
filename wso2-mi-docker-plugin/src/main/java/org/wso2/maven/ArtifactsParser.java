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

package org.wso2.maven;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.wso2.maven.metadata.Application;
import org.wso2.maven.metadata.Artifact;
import org.wso2.maven.metadata.Dependency;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.FileInputStream;
import java.util.Iterator;

public class ArtifactsParser {
    public Application parse(String filePath) throws Exception {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        try (FileInputStream fis = new FileInputStream(filePath)) {
            XMLStreamReader reader = factory.createXMLStreamReader(fis);
            OMElement documentElement = OMXMLBuilderFactory.createStAXOMBuilder(reader).getDocumentElement();

            Application application = new Application();
            Iterator<?> artifactIterator = documentElement.getChildrenWithLocalName("artifact");
            while (artifactIterator.hasNext()) {
                OMElement artifactElement = (OMElement) artifactIterator.next();
                Artifact artifact = parseArtifact(artifactElement);
                if (Constants.CARBON_APP_TYPE.equals(artifact.getType())) {
                    application.setApplicationArtifact(artifact);
                    application.setAppName(artifact.getName());
                    application.setAppVersion(artifact.getVersion());
                    application.setMainSequence(artifact.getMainSequence());
                    // TODO: How do we match the server roles and keep the relevant dependencies?
                    break;
                }
            }
            return application;
        }
    }

    public Artifact parseArtifact(OMElement documentElement) {
            Artifact artifact = new Artifact();
            artifact.setName(documentElement.getAttributeValue(new QName("name")));
            artifact.setVersion(documentElement.getAttributeValue(new QName("version")));
            artifact.setMainSequence(documentElement.getAttributeValue(new QName("mainSequence")));
            artifact.setType(documentElement.getAttributeValue(new QName("type")));
            artifact.setServerRole(documentElement.getAttributeValue(new QName("serverRole")));

            Iterator<?> dependencyIterator = documentElement.getChildrenWithLocalName("dependency");
            while (dependencyIterator.hasNext()) {
                OMElement dependencyElement = (OMElement) dependencyIterator.next();
                Dependency dependency = new Dependency();
                dependency.setArtifactName(dependencyElement.getAttributeValue(new QName("artifact")));
                dependency.setVersion(dependencyElement.getAttributeValue(new QName("version")));
                dependency.setInclude(Boolean.parseBoolean(dependencyElement.getAttributeValue(new QName("include"))));
                dependency.setServerRole(dependencyElement.getAttributeValue(new QName("serverRole")));
                artifact.addDependency(dependency);
            }

            // read the subArtifacts
            OMElement subArtifactsElement = documentElement
                    .getFirstChildWithName(new QName("subArtifacts"));
            if (subArtifactsElement != null) {
                Iterator<?> subArtifactIterator = subArtifactsElement.getChildrenWithLocalName("artifact");
                while (subArtifactIterator.hasNext()) {
                    Artifact subArtifact = parseArtifact((OMElement) subArtifactIterator.next());
                    artifact.addSubArtifact(subArtifact);
                }

            }

            // read the file element
            OMElement fileElement = documentElement.getFirstChildWithName(new QName("file"));
            if (fileElement != null) {
                artifact.setFile(fileElement.getText());
            }

            return artifact;
    }
}
