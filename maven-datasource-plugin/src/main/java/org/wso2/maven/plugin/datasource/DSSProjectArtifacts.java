/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.maven.plugin.datasource;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.FactoryConfigurationError;

import org.apache.axiom.om.OMDocument;
import org.apache.axiom.om.OMElement;
import org.wso2.maven.core.model.AbstractXMLDoc;

public class DSSProjectArtifacts extends AbstractXMLDoc implements Observer {

    List<DSSArtifact> dssArtifacts = new ArrayList<DSSArtifact>();
    private File source;
    private final static Logger LOGGER = Logger.getLogger(DSSProjectArtifacts.class.getName());

    public void update(Observable o, Object arg) {
    }

    @Override
    protected void deserialize(OMElement documentElement) throws Exception {
        List<OMElement> artifactElements = getChildElements(documentElement, "artifact");
        for (OMElement omElement : artifactElements) {
            DSSArtifact artifact = new DSSArtifact();
            artifact.setName(getAttribute(omElement, "name"));
            artifact.setVersion(getAttribute(omElement, "version"));
            artifact.setType(getAttribute(omElement, "type"));
            artifact.setServerRole(getAttribute(omElement, "serverRole"));
            artifact.setGroupId(getAttribute(omElement, "groupId"));
            artifact.setFile(getChildElements(omElement, "file").size() > 0 ? getChildElements(omElement, "file").get(0).getText() : null);
            dssArtifacts.add(artifact);
        }
    }

    @Override
    protected String serialize() throws Exception {
        String result = null;
        OMDocument document = factory.createOMDocument();
        OMElement documentElement = getDocumentElement();
        document.addChild(documentElement);
        try {
            result = getPretifiedString(documentElement);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Unable to Serialize Artifact");
            return null;
        }
        return result;
    }

    @Override
    protected String getDefaultName() {
        return null;
    }

    public void addDSSArtifact(DSSArtifact artifact) {
        dssArtifacts.add(artifact);
    }

    public boolean removeDSSArtifact(DSSArtifact artifact) {
        return dssArtifacts.remove(artifact);
    }

    public List<DSSArtifact> getAllDSSArtifacts() {
        return Collections.unmodifiableList(dssArtifacts);
    }

    public OMElement getDocumentElement() {
        OMElement documentElement = getElement("artifacts", "");

        for (DSSArtifact dssArtifact : dssArtifacts) {
            OMElement artifactElement = getElement("artifact", "");

            if (!dssArtifact.isAnonymous()) {
                addAttribute(artifactElement, "name", dssArtifact.getName());
            }

            if (!dssArtifact.isAnonymous() && dssArtifact.getGroupId() != null) {
                addAttribute(artifactElement, "groupId", dssArtifact.getGroupId());
            }

            if (!dssArtifact.isAnonymous() && dssArtifact.getVersion() != null) {
                addAttribute(artifactElement, "version", dssArtifact.getVersion());
            }

            if (dssArtifact.getType() != null) {
                addAttribute(artifactElement, "type", dssArtifact.getType());
            }

            if (dssArtifact.getServerRole() != null) {
                addAttribute(artifactElement, "serverRole", dssArtifact.getServerRole());
            }

            if (dssArtifact.getFile() != null) {
                artifactElement.addChild(getElement("file", dssArtifact.getFile()));
            }

            documentElement.addChild(artifactElement);
        }

        return documentElement;
    }

    public void setSource(File source) {
        this.source = source;
    }

    public File getSource() {
        return source;
    }

    public File toFile() throws Exception {
        File savedFile = new File(toFile(getSource()).toString());
        return savedFile;
    }

    public void fromFile(File file) throws FactoryConfigurationError, Exception {
        setSource(file);
        if (getSource().exists()) {
            deserialize(getSource());
        }
    }

}
