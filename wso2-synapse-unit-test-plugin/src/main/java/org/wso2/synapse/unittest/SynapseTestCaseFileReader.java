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
import org.codehaus.plexus.util.Base64;
import org.codehaus.plexus.util.StringUtils;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.wso2.synapse.unittest.Constants.RELATIVE_PREVIOUS;

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
     * @param synapseTestCaseName synapse test case name
     * @return SynapseTestCase data which is ready to send to the server
     */
    static String processArtifactData(String synapseTestCaseFilePath, String synapseTestCaseName) {

        try {
            String synapseTestCaseFileAsString = FileUtils.readFileToString(new File(synapseTestCaseFilePath));
            OMElement importedXMLFile = AXIOMUtil.stringToOM(synapseTestCaseFileAsString);

            if (getLog().isDebugEnabled()) {
                getLog().debug("Checking SynapseTestCase file contains artifact data");
            }

            QName qualifiedTestCases = new QName("", Constants.TEST_CASES_TAG, "");
            OMElement testCasesNode = importedXMLFile.getFirstChildWithName(qualifiedTestCases);
            if (testCasesNode != null) {
                Iterator<OMElement> testCaseIterator = testCasesNode.getChildElements();
                if (!testCaseIterator.hasNext()) {
                    return Constants.NO_TEST_CASES;
                } else {
                    if (StringUtils.isNotBlank(synapseTestCaseName)) {
                        int numberOfTestCases = 0;
                        List<OMElement> testCasesToRemove = new ArrayList<>();
                        while (testCaseIterator.hasNext()) {
                            numberOfTestCases++;
                            OMElement testCase = testCaseIterator.next();
                            String testCaseName = testCase.getAttributeValue(new QName("name"));
                            if (!synapseTestCaseName.equals(testCaseName)) {
                                testCasesToRemove.add(testCase);
                            }
                        }
                        if (numberOfTestCases == testCasesToRemove.size()) {
                            return Constants.NO_TEST_CASES;
                        } else {
                            for (OMElement testCase : testCasesToRemove) {
                                testCase.detach();
                            }
                        }
                    }
                }
            } else {
                return Constants.NO_TEST_CASES;
            }

            QName qualifiedArtifacts = new QName("", Constants.ARTIFACTS, "");
            OMElement artifactsNode = importedXMLFile.getFirstChildWithName(qualifiedArtifacts);

            //Read main-test-artifact data
            processTestArtifactData(artifactsNode);

            //Read supportive-artifacts data
            processSupportiveArtifactData(artifactsNode);

            //Read registry resources data
            processRegistryResourcesData(artifactsNode);

            //Read connector resources data
            processConnectorResourcesData(artifactsNode);

            QName qualifiedMockServices = new QName("", Constants.MOCK_SERVICES, "");
            OMElement mockServicesNode = importedXMLFile.getFirstChildWithName(qualifiedMockServices);

            //Read mock-services data
            processMockServicesData(mockServicesNode);

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
            File testArtifactFile = new File(RELATIVE_PREVIOUS + File.separator +  testArtifactFilePath);
            if (!testArtifactFile.exists()) {
                testArtifactFile = new File(RELATIVE_PREVIOUS + File.separator +
                        RELATIVE_PREVIOUS + File.separator +  testArtifactFilePath);
                if (!testArtifactFile.exists()) {
                    if (testArtifactFilePath.startsWith(File.separator)) {
                        testArtifactFilePath = testArtifactFilePath.substring(1);
                    }
                    testArtifactFileAsString = FileUtils.readFileToString(new File(testArtifactFilePath));
                } else {
                    testArtifactFileAsString = FileUtils.readFileToString(testArtifactFile);
                }
            } else {
                testArtifactFileAsString = FileUtils.readFileToString(testArtifactFile);
            }

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
                File supportiveArtifactFile = new File(RELATIVE_PREVIOUS + File.separator +  artifactFilePath);
                if (!supportiveArtifactFile.exists()) {
                    supportiveArtifactFile = new File(RELATIVE_PREVIOUS + File.separator + 
                                    RELATIVE_PREVIOUS + File.separator +  artifactFilePath);
                    if (!supportiveArtifactFile.exists()) {
                        supportiveArtifactFileAsString = FileUtils.readFileToString(new File(artifactFilePath));
                    } else {
                        supportiveArtifactFileAsString = FileUtils.readFileToString(supportiveArtifactFile);
                    }
                } else {
                    supportiveArtifactFileAsString = FileUtils.readFileToString(supportiveArtifactFile);
                }
            } else {
                throw new IOException("Supportive artifact does not contain configuration file path");
            }

            if (supportiveArtifactFileAsString != null) {
                //add supportive-artifact data as a child
                OMElement supportiveArtifactDataNode = AXIOMUtil.stringToOM(supportiveArtifactFileAsString);
                artifact.removeChildren();
                artifact.addChild(supportiveArtifactDataNode);
            } else {
                throw new IOException("Supportive artifact does not contain any configuration data");
            }
        }
    }

    /**
     * Method of processing registry-resources data.
     * Reads registry resources from user defined file and append it to the artifact node
     *
     * @param artifactsNode artifact data contain node
     */
    private static void processRegistryResourcesData(OMElement artifactsNode) throws IOException, XMLStreamException {
        QName qualifiedRegistryResources = new QName("", Constants.REGISTRY_RESOURCES, "");
        OMElement registryResourcesNode = artifactsNode.getFirstChildWithName(qualifiedRegistryResources);

        List<OMElement> datamapperDataNodeList = new ArrayList<>();
        Iterator resourceIterator = Collections.emptyIterator();
        if (registryResourcesNode != null) {
            resourceIterator = registryResourcesNode.getChildElements();
        }

        while (resourceIterator.hasNext()) {
            OMElement resource = (OMElement) resourceIterator.next();

            QName qualifiedRegistryResourcesFile = new QName("", Constants.ARTIFACT, "");
            OMElement registryResourcesFileNode = resource.getFirstChildWithName(qualifiedRegistryResourcesFile);

            String registryResourceFileAsString;
            if (!registryResourcesFileNode.getText().isEmpty()) {
                if (registryResourcesFileNode.getText().endsWith(".ts") &&
                        registryResourcesFileNode.getText().contains(Constants.DATA_MAPPER)) {
                    processDataMapperResourcesData(datamapperDataNodeList, resource);
                }
                registryResourceFileAsString = getResourceFileAsString(registryResourcesFileNode);
            } else {
                throw new IOException("Registry resource does not contain configuration file path");
            }

            if (registryResourceFileAsString != null) {
                registryResourcesFileNode.setText(registryResourceFileAsString);
            } else {
                throw new IOException("Registry resource does not contain any configuration data");
            }
        }
        // add datamapper data to the registry resources
        for (OMElement child : datamapperDataNodeList) {
            registryResourcesNode.addChild(child);
        }
    }

    /**
     * Add entries for Datamapper resources
     *
     * @param datamapperDataNodeList list of datamapper data nodes
     * @param registryResourcesNode  registry resources node
     *
     */
    private static void processDataMapperResourcesData(List<OMElement> datamapperDataNodeList,
                                                       OMElement registryResourcesNode)
            throws IOException, XMLStreamException {

        OMElement dataMapperFileNodeName = registryResourcesNode.getFirstChildWithName(new QName("",
                Constants.FILE_NAME, ""));
        String registryPath = registryResourcesNode.getFirstChildWithName(new QName("",
                Constants.REGISTRY_PATH, "")).getText();
        String mediaType = registryResourcesNode.getFirstChildWithName(new QName("",
                Constants.MEDIA_TYPE, "")).getText();
        if (dataMapperFileNodeName.getText().isEmpty()) {
            return;
        }
        String dataMapperName = dataMapperFileNodeName.getText().replace(Constants.TS, "");
        createDataMapperResourceEntry(dataMapperName, dataMapperName + Constants.DMC, mediaType,
                datamapperDataNodeList, registryPath);
        createDataMapperResourceEntry(dataMapperName, dataMapperName + Constants.INPUT_SCHEMA, mediaType,
                datamapperDataNodeList, registryPath);
        createDataMapperResourceEntry(dataMapperName, dataMapperName + Constants.OUTPUT_SCHEMA, mediaType,
                datamapperDataNodeList, registryPath);
    }

    /**
     * Create data mapper resource entry which resides in the target directory
     *
     * @param dataMapperName data mapper name
     * @param resourceName   resource name
     * @param dataMapperNodes list of data mapper nodes
     *
     */
    private static void createDataMapperResourceEntry(String dataMapperName, String resourceName, String mediaType,
                                                      List<OMElement> dataMapperNodes, String registryPath)
            throws IOException, XMLStreamException {

        OMElement dmNode = AXIOMUtil.stringToOM("<registry-resource></registry-resource>");
        dmNode.addChild(AXIOMUtil.stringToOM("<file-name>" + resourceName + "</file-name>"));
        OMElement dmcArtifactNode = AXIOMUtil.stringToOM(
                "<artifact>target/datamapper/" + dataMapperName + "/" + resourceName + "</artifact>");
        String resourceAsString = getResourceFileAsString(dmcArtifactNode);
        dmcArtifactNode.setText(resourceAsString);
        dmNode.addChild(dmcArtifactNode);
        dmNode.addChild(AXIOMUtil.stringToOM("<registry-path>" + registryPath + "</registry-path>"));
        dmNode.addChild(AXIOMUtil.stringToOM("<media-type>" + mediaType + "</media-type>"));
        dataMapperNodes.add(dmNode);
    }

    /**
     * Get the resource file as a string
     *
     * @param registryResourcesFileNode registry resources file node
     * @return resource file as a string
     */
    private static String getResourceFileAsString(OMElement registryResourcesFileNode) throws IOException {
        String registryResourceFileAsString;
        String registryFilePath = registryResourcesFileNode.getText();
        File registryResourceFile = new File(RELATIVE_PREVIOUS + File.separator +  registryFilePath);
        if (!registryResourceFile.exists()) {
            registryResourceFile = new File(RELATIVE_PREVIOUS + File.separator +
                    RELATIVE_PREVIOUS + File.separator +  registryFilePath);
            if (!registryResourceFile.exists()) {
                registryResourceFileAsString = FileUtils.readFileToString(new File(registryFilePath));
            } else {
                registryResourceFileAsString = FileUtils.readFileToString(registryResourceFile);
            }
        } else {
            registryResourceFileAsString = FileUtils.readFileToString(registryResourceFile);
        }
        return registryResourceFileAsString;
    }

    /**
     * Method of processing connector-resources data.
     * Reads connector resources from user defined file and append it to the artifact node
     *
     * @param artifactsNode artifact data contain node
     */
    private static void processConnectorResourcesData(OMElement artifactsNode) throws IOException, XMLStreamException {
        QName qualifiedConnectorResources = new QName("", Constants.CONNECTOR_RESOURCES, "");
        OMElement connectorResourcesNode = artifactsNode.getFirstChildWithName(qualifiedConnectorResources);
        Iterator<?> connectorIterator = Collections.emptyIterator();

        if (connectorResourcesNode != null) {
            connectorIterator = connectorResourcesNode.getChildElements();
        }

        while (connectorIterator.hasNext()) {
            OMElement resource = (OMElement) connectorIterator.next();

            String encodedConnectorFile;
            if (!resource.getText().isEmpty()) {
                if (!resource.getText().endsWith(Constants.ZIP)) {
                    String connectorZipPath = Paths.get(Constants.TARGET, Constants.DEPENDENCY,
                            resource.getText() + Constants.ZIP).toString();
                    resource.setText(connectorZipPath);
                }
                String registryFilePath;
                File connectorResourceFile = new File(RELATIVE_PREVIOUS + File.separator + resource.getText());
                if (!connectorResourceFile.exists()) {
                    connectorResourceFile = new File(RELATIVE_PREVIOUS + File.separator +
                            RELATIVE_PREVIOUS + File.separator +  resource.getText());
                    if (!connectorResourceFile.exists()) {
                        registryFilePath = resource.getText();
                    } else {
                        registryFilePath = RELATIVE_PREVIOUS + File.separator + RELATIVE_PREVIOUS + File.separator + resource.getText();
                    }
                } else {
                    registryFilePath = RELATIVE_PREVIOUS + File.separator + resource.getText();
                }
                byte[] connectorInBytes = Files.readAllBytes(Paths.get(registryFilePath));
                byte[] encoded = Base64.encodeBase64(connectorInBytes);
                encodedConnectorFile = new String(encoded);
            } else {
                throw new IOException("Connector resource does not contain configuration file path");
            }

            if (encodedConnectorFile != null) {
                //add connector resource base64 data as a child
                resource.setText(encodedConnectorFile);
            } else {
                throw new IOException("Connector resource does not contain any configuration data");
            }
        }
    }

    /**
     * Method of processing mock service data.
     * Reads mock service configurations from user defined file and append it to the mock services node
     *
     * @param mockServicesNode artifact data contain node
     */
    private static void processMockServicesData(OMElement mockServicesNode) throws IOException, XMLStreamException {
        Iterator mockServiceIterator = Collections.emptyIterator();
        if (mockServicesNode != null) {
            mockServiceIterator = mockServicesNode.getChildElements();
        }

        List<OMElement> mockServiceDataNodeList = new ArrayList<>();
        while (mockServiceIterator.hasNext()) {
            OMElement mockServiceNode = (OMElement) mockServiceIterator.next();

            String mockServiceFileDataAsString;
            if (!mockServiceNode.getText().isEmpty()) {
                String mockServiceFilePath = mockServiceNode.getText();
                File mockServiceFile = new File(RELATIVE_PREVIOUS + File.separator +  mockServiceFilePath);
                if (!mockServiceFile.exists()) {
                    mockServiceFile = new File(RELATIVE_PREVIOUS + File.separator +
                            RELATIVE_PREVIOUS + File.separator +  mockServiceFilePath);
                    if (!mockServiceFile.exists()) {
                        mockServiceFileDataAsString = FileUtils.readFileToString(new File(mockServiceFilePath));
                    } else {
                        mockServiceFileDataAsString = FileUtils.readFileToString(mockServiceFile);
                    }
                    
                } else {
                    mockServiceFileDataAsString = FileUtils.readFileToString(mockServiceFile);
                }
            } else {
                throw new IOException("Mock service file does not contain configuration file path");
            }

            if (mockServiceFileDataAsString != null) {
                //add mock service data as a child
                OMElement mockServiceDataNode = AXIOMUtil.stringToOM(mockServiceFileDataAsString);
                mockServiceDataNodeList.add(mockServiceDataNode);

            } else {
                throw new IOException("Mock service does not contain any configuration data");
            }
        }

        //remove all child elements in mock-services tag
        mockServicesNode.removeChildren();

        //add mock services childs to the mock-services tag
        for (OMElement child : mockServiceDataNodeList) {
            mockServicesNode.addChild(child);
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
