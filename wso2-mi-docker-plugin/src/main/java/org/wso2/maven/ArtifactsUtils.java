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

import java.io.UnsupportedEncodingException;

public class ArtifactsUtils {

    /**
     * Format the string paths to match any platform.. windows, linux etc..
     *
     * @param path - input file path
     * @return formatted file path
     */
    public static String formatPath(String path) throws UnsupportedEncodingException {
        // removing white spaces
        String pathformatted = path.replaceAll("\\b\\s+\\b", "%20");
        try {
            pathformatted = java.net.URLDecoder.decode(pathformatted, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new UnsupportedEncodingException("Unsupported Encoding in the path :"+ pathformatted);
        }
        // replacing all "\" with "/"
        return pathformatted.replace('\\', '/');
    }
}
