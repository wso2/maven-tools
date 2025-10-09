/*
 * Copyright (c) 2025, WSO2 LLC (http://www.wso2.com).
 *
 * WSO2 LLC licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.maven;

import org.apache.axiom.om.OMElement;

import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.wso2.maven.libraries.CAppDependencyResolver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CAppHandlerTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    static class MockCARMojo extends CARMojo {
        private final StringBuilder logs = new StringBuilder();
        @Override public void logInfo(String msg) { logs.append(msg).append("\n"); }
        @Override public void logError(String msg) { logs.append("ERROR: ").append(msg).append("\n"); }
    }

    @Test
    public void testCreateDependencyDescriptorXml_ReturnsCorrectXml() {

        String projectName = "com.example_test_1.0.0";
        CARMojo mojo = new CAppHandlerTest.MockCARMojo();
        CAppHandler handler = new CAppHandler("test", mojo);

        OMElement result = handler.createDependencyDescriptorXml(projectName,false, java.util.Collections.<CAppDependency>emptyList(), false);

        assertNotNull(result);
        assertEquals("project", result.getLocalName());
        OMElement idElement = result.getFirstChildWithName(new javax.xml.namespace.QName("id"));
        assertNotNull(idElement);
        assertEquals(projectName, idElement.getText());
        OMElement dependenciesElement = result.getFirstChildWithName(new javax.xml.namespace.QName("dependencies"));
        assertNotNull(dependenciesElement);
    }

    @Test
    public void testCreateDependencyDescriptorXml_WithDependencies() {
        String projectName = "com.example_test_1.0.0";
        CARMojo mojo = new MockCARMojo();
        CAppHandler handler = new CAppHandler("test", mojo);

        List<CAppDependency> deps = Arrays.asList(new CAppDependency("group1", "artifact1", "1.0.0"),
                new CAppDependency("group2", "artifact2", "2.0.0"));

        OMElement result = handler.createDependencyDescriptorXml(projectName, false, deps, false);

        OMElement dependenciesElement = result.getFirstChildWithName(new javax.xml.namespace.QName("dependencies"));
        assertNotNull(dependenciesElement);

        OMElement depElem1 = dependenciesElement.getFirstElement();
        assertNotNull(depElem1);
        assertEquals("group1", depElem1.getAttributeValue(new javax.xml.namespace.QName("groupId")));
        assertEquals("artifact1", depElem1.getAttributeValue(new javax.xml.namespace.QName("artifactId")));
        assertEquals("1.0.0", depElem1.getAttributeValue(new javax.xml.namespace.QName("version")));
        assertEquals("car", depElem1.getAttributeValue(new javax.xml.namespace.QName("type")));

        OMElement depElem2 = (OMElement) depElem1.getNextOMSibling();
        assertNotNull(depElem2);
        assertEquals("group2", depElem2.getAttributeValue(new javax.xml.namespace.QName("groupId")));
        assertEquals("artifact2", depElem2.getAttributeValue(new javax.xml.namespace.QName("artifactId")));
        assertEquals("2.0.0", depElem2.getAttributeValue(new javax.xml.namespace.QName("version")));
        assertEquals("car", depElem2.getAttributeValue(new javax.xml.namespace.QName("type")));
        assertEquals(2, dependenciesElement.getChildElements().hasNext() ? 2 : 0);
    }

    @Test
    public void testCreateDependencyDescriptorXml_versionedDeployment() {

        String projectName = "com.example_test_1.0.0";
        CARMojo mojo = new CAppHandlerTest.MockCARMojo();
        CAppHandler handler = new CAppHandler("test", mojo);

        OMElement result = handler.createDependencyDescriptorXml(projectName,true, java.util.Collections.<CAppDependency>emptyList(), false);

        assertNotNull(result);
        assertEquals("project", result.getLocalName());
        OMElement idElement = result.getFirstChildWithName(new javax.xml.namespace.QName("id"));
        assertNotNull(idElement);
        assertEquals(projectName, idElement.getText());
        OMElement versionedDeploymentElement = result.getFirstChildWithName(new javax.xml.namespace.QName("versionedDeployment"));
        assertNotNull(idElement);
        assertEquals("true", versionedDeploymentElement.getText());
        OMElement dependenciesElement = result.getFirstChildWithName(new javax.xml.namespace.QName("dependencies"));
        assertNotNull(dependenciesElement);
    }
}
