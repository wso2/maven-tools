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

import com.google.gson.Gson;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.maven.Utils;

import java.util.List;
import java.util.Map;

public class UtilsTest {

    @Test(dataProvider = "input")
    public void testMerge(String input, String output, String expected) {

        Map inputMap = new Gson().fromJson(input, Map.class);
        Map outputMap = new Gson().fromJson(output, Map.class);
        Map expectedMap = new Gson().fromJson(expected, Map.class);
        Utils.mergeMaps(inputMap, outputMap, false);
        assertMaps(outputMap, expectedMap);
    }

    @DataProvider(name = "input")
    public Object[][] getData() {

        return new Object[][]{
                {"{\"a\": \"b\"}", "{\"b\": \"c\"}", "{\"a\":\"b\",\"b\": \"c\"}"},
                {"{\"a\": \"b\"}", "{\"a\": \"c\"}", "{\"a\": \"c\"}"},
                {"{\"a\": \"b\", \"c\": { \"e\": \"b\"}}", "{\"b\": \"c\", \"c\": {\"d\": \"b\"}}", "{\"a\": \"b\"," +
                        "\"b\": \"c\",\"c\": {\"e\": \"b\", \"d\": \"b\"}}"},
                {"{\"a\": \"b\", \"c\": { \"d\": \"b\"}}", "{\"b\": \"c\", \"c\": {\"d\": \"c\"}}", "{\"a\": \"b\"," +
                        "\"b\": \"c\",\"c\": {\"d\": \"c\"}}"},
                {"{\"a\": \"b\", \"c\": [\"a\",\"b\"]}", "{\"b\": \"c\", \"c\": [\"b\",\"c\"]}", "{\"a\": \"b\"," +
                        "\"b\": " +
                        "\"c\", \"c\": [\"b\",\"c\"]}"}
        };
    }

    public static void assertMaps(Map actual, Map expected) {

        actual.forEach((k, v) -> {
            Assert.assertNotNull(expected.get(k));
            if (v instanceof List) {
                Assert.assertEqualsNoOrder(((List) v).toArray(), ((List) expected.get(k)).toArray());
            } else if (v instanceof Map) {
                assertMaps((Map) v, (Map) expected.get(k));
            }
        });
    }
}

