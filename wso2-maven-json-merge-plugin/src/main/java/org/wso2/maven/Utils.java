/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Utils {

    public static Map getReadMap(String path) throws MojoExecutionException {

        if (Paths.get(path).toFile().exists()) {
            Gson gson = new Gson();
            try (FileInputStream fileInputStream = new FileInputStream(path)) {
                Reader input = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8);
                return gson.fromJson(input, Map.class);
            } catch (IOException e) {
                throw new MojoExecutionException("Error while reading json file", e);
            }
        } else {
            return new HashMap();
        }
    }

    public static String convertIntoJson(Map input) {

        Gson gson = new GsonBuilder().setPrettyPrinting().setLenient().create();
        return gson.toJson(input);
    }

    public static Map<String, Object> mergeMaps(Map<String, Object> baseMap, Map<String, Object> input, boolean isChildMergeEnabled) {
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            Object returnedValue = baseMap.get(key);
            if (returnedValue == null) {
                baseMap.put(key, value);
            } else if (returnedValue instanceof Map && isChildMergeEnabled) {
                value = mergeMaps((Map<String, Object>) returnedValue, (Map<String, Object>) value, true);
                baseMap.put(key, value);
            }
        }
        return baseMap;
    }

}
