/*
 *  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.maven.p2.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class composed of utility methods pertaining to a Bundles.
 */
public class BundleUtils {

    private static final Pattern OSGI_VERSION_PATTERN = Pattern.compile("[0-9]+\\.[0-9]+\\.[0-9]+(\\.[0-9A-Za-z_-]+)?");
    private static final Pattern ONLY_NUMBERS = Pattern.compile("[0-9]+");

    /**
     * Returns the OSGI version for the given artifact version.
     *
     * @param version artifact version
     * @return OSGI version
     */
    public static String getOSGIVersion(String version) {
        // This method is taken from org.apache.maven.shared.osgi.DefaultMaven2OsgiConverter to get the OSGI version
        // from a give version artifact version.
        String osgiVersion;

        // Matcher m = P_VERSION.matcher(version);
        // if (m.matches()) {
        // osgiVersion = m.group(1) + "." + m.group(3);
        // }

        /* TODO need a regexp guru here */

        Matcher m;

        /* if it's already OSGi compliant don't touch it */
        m = OSGI_VERSION_PATTERN.matcher(version);
        if (m.matches()) {
            return version;
        }

        osgiVersion = version;

        /* check for dated snapshot versions with only major or major and minor */
        Pattern DATED_SNAPSHOT = Pattern.compile("([0-9])(\\.([0-9]))?(\\.([0-9]))?\\-([0-9]{8}\\.[0-9]{6}\\-[0-9]*)");
        m = DATED_SNAPSHOT.matcher(osgiVersion);
        if (m.matches()) {
            String major = m.group(1);
            String minor = (m.group(3) != null) ? m.group(3) : "0";
            String service = (m.group(5) != null) ? m.group(5) : "0";
            String qualifier = m.group(6).replaceAll("-", "_").replaceAll("\\.", "_");
            osgiVersion = major + "." + minor + "." + service + "." + qualifier;
        }

        /* else transform first - to . and others to _ */
        osgiVersion = osgiVersion.replaceFirst("-", "\\.");
        osgiVersion = osgiVersion.replaceAll("-", "_");
        m = OSGI_VERSION_PATTERN.matcher(osgiVersion);
        if (m.matches()) {
            return osgiVersion;
        }

        /* remove dots in the middle of the qualifier */
        Pattern DOTS_IN_QUALIFIER = Pattern.compile("([0-9])(\\.[0-9])?\\.([0-9A-Za-z_-]+)\\.([0-9A-Za-z_-]+)");
        m = DOTS_IN_QUALIFIER.matcher(osgiVersion);
        if (m.matches()) {
            String s1 = m.group(1);
            String s2 = m.group(2);
            String s3 = m.group(3);
            String s4 = m.group(4);

            Matcher qualifierMatcher = ONLY_NUMBERS.matcher(s3);
            /*
             * if last portion before dot is only numbers then it's not in the middle of the
             * qualifier
             */
            if (!qualifierMatcher.matches()) {
                osgiVersion = s1 + s2 + "." + s3 + "_" + s4;
            }
        }

        /* convert
         * 1.string   -> 1.0.0.string
         * 1.2.string -> 1.2.0.string
         * 1          -> 1.0.0
         * 1.1        -> 1.1.0
         */
        //Pattern NEED_TO_FILL_ZEROS = Pattern.compile( "([0-9])(\\.([0-9]))?\\.([0-9A-Za-z_-]+)" );
        Pattern NEED_TO_FILL_ZEROS = Pattern.compile("([0-9])(\\.([0-9]))?(\\.([0-9A-Za-z_-]+))?");
        m = NEED_TO_FILL_ZEROS.matcher(osgiVersion);
        if (m.matches()) {
            String major = m.group(1);
            String minor = m.group(3);
            String service = null;
            String qualifier = m.group(5);

            /* if there's no qualifier just fill with 0s */
            if (qualifier == null) {
                osgiVersion = getOSGIVersion(major, minor, null, null);
            } else {
                /* if last portion is only numbers then it's not a qualifier */
                Matcher qualifierMatcher = ONLY_NUMBERS.matcher(qualifier);
                if (qualifierMatcher.matches()) {
                    if (minor == null) {
                        minor = qualifier;
                    } else {
                        service = qualifier;
                    }
                    osgiVersion = getOSGIVersion(major, minor, service, null);
                } else {
                    osgiVersion = getOSGIVersion(major, minor, null, qualifier);
                }
            }
        }

        m = OSGI_VERSION_PATTERN.matcher(osgiVersion);
        /* if still its not OSGi version then add everything as qualifier */
        if (!m.matches()) {
            String major = "0";
            String minor = "0";
            String service = "0";
            String qualifier = osgiVersion.replaceAll("\\.", "_");
            osgiVersion = major + "." + minor + "." + service + "." + qualifier;
        }

        return osgiVersion;
    }

    private static String getOSGIVersion(String major, String minor, String service, String qualifier) {
        StringBuilder sb = new StringBuilder();
        sb.append(major != null ? major : "0");
        sb.append('.');
        sb.append(minor != null ? minor : "0");
        sb.append('.');
        sb.append(service != null ? service : "0");
        if (qualifier != null) {
            sb.append('.');
            sb.append(qualifier);
        }
        return sb.toString();
    }

}
