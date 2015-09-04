/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.maven.p2.utils;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.wso2.maven.p2.repo.CatFeature;
import org.wso2.maven.p2.repo.Category;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class P2Utils {
    private static String[] matchList = new String[]{"perfect", "equivalent", "compatible", "greaterOrEqual", "patch", "optional"};

    public static int getLastIndexOfProperties(File p2InfFile) throws IOException {
        int min = -1;
        if (p2InfFile.exists()) {
            BufferedReader in = null;
            try {
                in = new BufferedReader(new FileReader(p2InfFile));
                String line;
                while ((line = in.readLine()) != null) {
                    String[] split = line.split("=");
                    String[] split2 = split[0].split(Pattern.quote("."));
                    if (split2[0].equalsIgnoreCase("properties")) {
                        int index = Integer.parseInt(split2[1]);
                        if (index > min)
                            min = index;
                    }
                }
            } catch (FileNotFoundException e) {
                throw e;
            } finally {
                if (in != null) in.close();
            }
        }
        return min;
    }

    public static boolean isMatchString(String matchStr) {
        for (String match : matchList) {
            if (matchStr.equalsIgnoreCase(match)) {
                return true;
            }
        }
        return false;
    }

    public static String getMatchRule(String matchStr) {
        if (isPatch(matchStr)) {
            return "perfect";
        }
        for (String match : matchList) {
            if (matchStr.equalsIgnoreCase(match)) {
                return match;
            }
        }
        return null;
    }

    public static boolean isPatch(String matchStr) {
        return matchStr.equalsIgnoreCase("patch");
    }

    public static void createCategoryFile(MavenProject project, ArrayList categories, File categoryFile) throws MojoExecutionException {

        Map featureCategories = new HashMap();

        Document doc = MavenUtils.getManifestDocument();
        Element rootElement = doc.getDocumentElement();

        if (rootElement == null) {
            rootElement = doc.createElement("site");
            doc.appendChild(rootElement);
        }

        for (Object object : categories) {
            if (object instanceof Category) {
                Category cat = (Category) object;
                Element categoryDef = doc.createElement("category-def");
                categoryDef.setAttribute("name", cat.getId());
                categoryDef.setAttribute("label", cat.getLabel());
                rootElement.appendChild(categoryDef);
                Element descriptionElement = doc.createElement("description");
                descriptionElement.setTextContent(cat.getDescription());
                categoryDef.appendChild(descriptionElement);
                ArrayList<CatFeature> processedFeatures = cat.getProcessedFeatures(project);
                for (CatFeature feature : processedFeatures) {
                    if (!featureCategories.containsKey(feature.getId() + feature.getVersion())) {
                        ArrayList list = new ArrayList();
                        featureCategories.put((feature.getId() + feature.getVersion()), list);
                        list.add(feature);
                    }
                    ArrayList list = (ArrayList) featureCategories.get(feature.getId() + feature.getVersion());
                    list.add(cat.getId());
                }
            }
        }

        for (Object key : featureCategories.keySet()) {
            Object object = featureCategories.get(key);
            if (object instanceof List) {
                List list = (List) object;
                CatFeature feature = (CatFeature) list.get(0);
                list.remove(0);

                Element featureDef = doc.createElement("feature");
                featureDef.setAttribute("id", feature.getId());
                featureDef.setAttribute("version", BundleUtils.getOSGIVersion(feature.getVersion()));
                for (Object catId : list) {
                    Element category = doc.createElement("category");
                    category.setAttribute("name", catId.toString());
                    featureDef.appendChild(category);
                }
                rootElement.appendChild(featureDef);
            }
        }

        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer;
            transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(categoryFile);
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(source, result);
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to create feature manifest", e);
        }
    }
}
