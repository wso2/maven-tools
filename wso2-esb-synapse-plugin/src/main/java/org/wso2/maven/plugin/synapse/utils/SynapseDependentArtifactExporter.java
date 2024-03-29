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
import org.wso2.maven.capp.model.CAppArtifactDependency;

import java.io.File;

/**
 * Interface for synapse dependent artifact exporters.
 */
public interface SynapseDependentArtifactExporter {

    /**
     * Exports the specified dependent artifact into the directory specified by
     * <strong>contentLocation</strong> argument. This method will update the
     * <strong>synapseArtifact</strong> to reflect the newly added dependency.
     *
     * @param artifactDefinition   artifact definition {@link OMElement}.
     * @param synapseArtifactClone clone of the synapse artifact from which this dependent
     *                             artifact was extracted from.
     * @param workDir              temporary working directory.
     * @throws Exception if an error occurs while exporting the artifact.
     */
    CAppArtifactDependency export(OMElement artifactDefinition, CAppArtifactDependency synapseArtifactClone,
                                  File workDir)
            throws Exception;

}
