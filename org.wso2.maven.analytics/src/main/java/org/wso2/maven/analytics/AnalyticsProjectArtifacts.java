/*
*  Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.maven.analytics;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.xml.stream.FactoryConfigurationError;

import org.apache.axiom.om.OMDocument;
import org.apache.axiom.om.OMElement;
import org.wso2.maven.core.model.AbstractXMLDoc;

public class AnalyticsProjectArtifacts extends AbstractXMLDoc implements Observer{
	
	List<AnalyticsArtifact> analyticsArtifacts=new ArrayList<AnalyticsArtifact>();
	private File source;

	public void update(Observable o, Object arg) {
		
	}

	@Override
	protected void deserialize(OMElement documentElement) throws Exception {
		List<OMElement> artifactElements = getChildElements(documentElement, "artifact");
		for (OMElement omElement : artifactElements) {
			AnalyticsArtifact artifact=new AnalyticsArtifact();
	        artifact.setName(getAttribute(omElement, "name"));
	        artifact.setVersion(getAttribute(omElement, "version"));
	        artifact.setType(getAttribute(omElement, "type"));
	        artifact.setServerRole(getAttribute(omElement, "serverRole"));
	        artifact.setGroupId(getAttribute(omElement, "groupId"));
	        artifact.setFile(getChildElements(omElement, "file").size()>0?getChildElements(omElement, "file").get(0).getText():null);    
	        analyticsArtifacts.add(artifact);
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
			e.printStackTrace();
			return null;
		}
		return result;
	}

	@Override
	protected String getDefaultName() {
		return null;
	}
	
	public void addAnalyticsArtifact(AnalyticsArtifact artifact){
		analyticsArtifacts.add(artifact);
	}
	
	public boolean removeAnalyticsArtifact(AnalyticsArtifact artifact){
		return analyticsArtifacts.remove(artifact);
	}
	
	public List<AnalyticsArtifact> getAllAnalyticsArtifacts(){
		return Collections.unmodifiableList(analyticsArtifacts);
	}
	
	public OMElement getDocumentElement() {
		OMElement documentElement = getElement("artifacts", "");
		
		for (AnalyticsArtifact dssArtifact : analyticsArtifacts) {
			OMElement artifactElement = getElement("artifact", "");
			
			if (!dssArtifact.isAnonymous()){
				addAttribute(artifactElement, "name", dssArtifact.getName());
			}
			
			if (!dssArtifact.isAnonymous() && dssArtifact.getGroupId() != null){
				addAttribute(artifactElement, "groupId", dssArtifact.getGroupId());
			}
	        
			if (!dssArtifact.isAnonymous() && dssArtifact.getVersion() != null){
				addAttribute(artifactElement, "version", dssArtifact.getVersion());
			}
			
			if (dssArtifact.getType() != null){
				addAttribute(artifactElement, "type", dssArtifact.getType());
			}
			
			if (dssArtifact.getServerRole() != null){
				addAttribute(artifactElement, "serverRole", dssArtifact.getServerRole());
			}
			
			if (dssArtifact.getFile() != null){
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

	public void fromFile(File file) throws FactoryConfigurationError, Exception{
		setSource(file);
		if (getSource().exists()){
			deserialize(getSource());
		}
	}

}
