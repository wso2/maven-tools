/*
 * Copyright (c) 2023, WSO2 LLC (http://www.wso2.com).
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

package org.wso2.maven.plugin.synapse.utils;

import org.apache.axiom.om.OMElement;

import javax.xml.namespace.QName;

/**
 * {@link SynapseDependentArtifactExporter} corresponding to <localEntry/> tag.
 */
public class ProxyServiceArtifactExporter extends AbstractSynapseDependentArtifactExporter {

    /**
     * {@inheritDoc}
     */
    protected String getArtifactName(OMElement artifactDefinition) {

        return artifactDefinition.getAttributeValue(new QName("name"));
    }

    /**
     * {@inheritDoc}
     */
    protected String getArtifactType() {

        return "synapse/proxy-service";
    }
}
