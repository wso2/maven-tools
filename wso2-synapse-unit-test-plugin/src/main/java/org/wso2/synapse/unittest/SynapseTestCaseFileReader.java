/*
 Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.synapse.unittest;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;

/**
 * SynapseTestCase file read class in unit test framework.
 */
class SynapseTestCaseFileReader {

    /**
     * private constructor of the SynapseTestCaseFileReader class.
     */
    private SynapseTestCaseFileReader() {
    }

    private static Log log;

    /**
     * Check that SynapseTestCase data includes artifact data.
     * If not read artifact from given file and append data into the artifact data
     *
     * @param synapseTestCaseFilePath synapse test case file path
     * @return SynapseTestCase data which is ready to send to the server
     */
    static String processArtifactData(String synapseTestCaseFilePath) {

        try {
            String synapseTestCaseFileAsString = FileUtils.readFileToString(new File(synapseTestCaseFilePath));
            OMElement importedXMLFile = AXIOMUtil.stringToOM(synapseTestCaseFileAsString);

            getLog().info("Checking SynapseTestCase file contains artifact data");
            QName qualifiedArtifacts = new QName("", Constants.ARTIFACTS, "");
            OMElement artifactsNode = importedXMLFile.getFirstChildWithName(qualifiedArtifacts);

            processTestArtifactData(artifactsNode);

            //Read supportive-artifacts data
            processSupportiveArtifactData(artifactsNode);

            return importedXMLFile.toString();

        } catch (IOException | XMLStreamException e) {
            getLog().error("Artifact data reading failed ", e);
        }

        return null;
    }

    /**
     * Method of processing test-artifact data.
     * Reads artifacts from user defined file and append it to the artifact node
     *
     * @param artifactsNode artifact data contain node
     */
    private static void processTestArtifactData(OMElement artifactsNode) throws IOException, XMLStreamException {
        //Read artifacts from SynapseTestCase file
        QName qualifiedTestArtifact = new QName("", Constants.TEST_ARTIFACT, "");
        OMElement testArtifactNode = artifactsNode.getFirstChildWithName(qualifiedTestArtifact);

        //Read test-artifact data
        QName qualifiedArtifact = new QName("", Constants.ARTIFACT, "");
        OMElement testArtifactFileNode = testArtifactNode.getFirstChildWithName(qualifiedArtifact);

        String testArtifactFileAsString;
        if (!testArtifactFileNode.getText().isEmpty()) {
            String testArtifactFilePath = testArtifactFileNode.getText();
            testArtifactFileAsString = FileUtils.readFileToString(new File(testArtifactFilePath));
        } else {
            throw new IOException("Test artifact does not contain configuration file path");
        }

        if (testArtifactFileAsString != null) {
            //add test-artifact data as a child
            OMElement testArtifactDataNode = AXIOMUtil.stringToOM(testArtifactFileAsString);
            testArtifactFileNode.removeChildren();
            testArtifactFileNode.addChild(testArtifactDataNode);
        } else {
            throw new IOException("Test artifact does not contain any configuration data");
        }
    }

    /**
     * Method of processing supportive-artifact data.
     * Reads artifacts from user defined file and append it to the artifact node
     *
     * @param artifactsNode artifact data contain node
     */
    private static void processSupportiveArtifactData(OMElement artifactsNode) throws IOException, XMLStreamException {
        QName qualifiedSupportiveArtifact = new QName("", Constants.SUPPORTIVE_ARTIFACTS, "");
        OMElement supportiveArtifactNode = artifactsNode.getFirstChildWithName(qualifiedSupportiveArtifact);

        Iterator artifactIterator = Collections.emptyIterator();
        if (supportiveArtifactNode != null) {
            artifactIterator = supportiveArtifactNode.getChildElements();
        }

        while (artifactIterator.hasNext()) {
            OMElement artifact = (OMElement) artifactIterator.next();

            String supportiveArtifactFileAsString;
            if (!artifact.getText().isEmpty()) {
                String artifactFilePath = artifact.getText();
                supportiveArtifactFileAsString = FileUtils.readFileToString(new File(artifactFilePath));
            } else {
                throw new IOException("Supportive artifact does not contain configuration file path");
            }

            if (supportiveArtifactFileAsString != null) {
                //add test-artifact data as a child
                OMElement supportiveArtifactDataNode = AXIOMUtil.stringToOM(supportiveArtifactFileAsString);
                artifact.removeChildren();
                artifact.addChild(supportiveArtifactDataNode);
            } else {
                throw new IOException("Supportive artifact does not contain any configuration data");
            }
        }
    }

    /**
     * Method of initiating logger.
     */
    private static Log getLog() {
        if (log == null) {
            log = new SystemStreamLog();
        }

        return log;
    }
}
