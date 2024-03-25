/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.maven.p2;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.sisu.equinox.launching.LaunchConfiguration;

/**
 * This is an exact copy of org.eclipse.sisu.equinox.launching.internal.EquinoxInstallationLaunchConfiguration
 * apart from the String Prefix used to find the launcher jar.
 */
public class CustomEquinoxInstallationLaunchConfiguration implements LaunchConfiguration {

	private final File equinoxDirectory;
    private final String[] programArguments;
    private final String[] vmArguments = {
    		"-Djdk.util.zip.disableZip64ExtraFieldValidation=true",
    		"-Djdk.nio.zipfs.allowDotZipEntry=true",
    		"--add-opens=java.base/java.net=ALL-UNNAMED" 
    };
    private final File launcherJar;

    public CustomEquinoxInstallationLaunchConfiguration(File equinoxDirectory, List<String> programArguments) {
        this.equinoxDirectory = equinoxDirectory;
        this.programArguments = programArguments.toArray(new String[0]);
        this.launcherJar = findLauncherJar(equinoxDirectory);
    }

    public static File findLauncherJar(File equinoxDirectory) {
        File pluginsDir = new File(equinoxDirectory, "plugins");
        File[] launchers = pluginsDir
                .listFiles((FilenameFilter) (dir, name) -> name.startsWith("org.eclipse.equinox.launcher-"));

        if (launchers == null || launchers.length == 0)
            throw new IllegalArgumentException("No launcher bundle found in " + pluginsDir);
        else if (launchers.length > 1)
            throw new IllegalArgumentException("Multiple versions of the launcher bundle found in " + pluginsDir);
        else
            return launchers[0];
    }

    public static File findConfigurationArea(File location) {
        return new File(location, "configuration");
    }

    @Override
    public File getWorkingDirectory() {
        return equinoxDirectory;
    }

    @Override
    public String getJvmExecutable() {
        return null;
    }

    @Override
    public File getLauncherJar() {
        return launcherJar;
    }

    @Override
    public String[] getVMArguments() {
        return vmArguments;
    }

    @Override
    public String[] getProgramArguments() {
        return programArguments;
    }

    @Override
    public Map<String, String> getEnvironment() {
        return Collections.emptyMap();
    }

}
