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

/**
 * Constants used in kubernetes extension.
 */
public class KubernetesConstants {
    public static final String KUBERNETES_SVC_PROTOCOL = "TCP";
    public static final String KUBERNETES_SELECTOR_KEY = "app";
    public static final String SVC_POSTFIX = "-svc";
    public static final String CONFIG_MAP_POSTFIX = "-config-map";
    public static final String SECRET_POSTFIX = "-secret";
    public static final String DEPLOYMENT_POSTFIX = "-deployment";
    public static final String HPA_POSTFIX = "-hpa";
    public static final String DEPLOYMENT_FILE_POSTFIX = "_deployment";
    public static final String SVC_FILE_POSTFIX = "_svc";
    public static final String SECRET_FILE_POSTFIX = "_secret";
    public static final String CONFIG_MAP_FILE_POSTFIX = "_config_map";
    public static final String VOLUME_CLAIM_FILE_POSTFIX = "_volume_claim";
    public static final String HPA_FILE_POSTFIX = "_hpa";
    public static final String YAML = ".yaml";
    public static final String MI_HOME = "/home/wso2carbon";
    public static final String MI_TMP_HOME = "/tmp";
    public static final String MI_CONF_MOUNT_PATH = MI_TMP_HOME + "/conf";
    public static final String MI_CONF_SECRETS_MOUNT_PATH = MI_TMP_HOME + "/secrets";
    public static final String MI_CONF_FILE_NAME = "env";
    public static final String KEY_REF = "key_ref";
    public static final String MIN_MEMORY = "min_memory";
    public static final String MIN_CPU = "min_cpu";
    public static final String MEMORY = "memory";
    public static final String CPU = "cpu";

    public static final String REGISTRY_SEPARATOR = "/";

    /**
     * Service type enum.
     */
    public enum ServiceType {
        ClusterIP,
        NodePort,
    }
}