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

package org.wso2.maven.libraries;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.mockito.Mockito.*;
import org.wso2.maven.CARMojo;
import org.wso2.maven.model.ArtifactDependency;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CAppDependencyResolverTest {

    // Minimal mock for CARMojo
    static class MockCARMojo extends CARMojo {
        @Override public void logInfo(String msg) {}
        @Override public void logError(String msg) {}
    }

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private void writeDescriptorXmlToCar(File carFile, String descriptorContent) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(carFile))) {
            ZipEntry entry = new ZipEntry("descriptor.xml");
            zos.putNextEntry(entry);
            zos.write(descriptorContent.getBytes());
            zos.closeEntry();
        }
    }

    private void createCarFileWithDescriptor(File carFile, String groupId, String artifactId, String version, String... dependencies) throws Exception {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(carFile))) {
            zos.putNextEntry(new ZipEntry("descriptor.xml"));
            StringBuilder descriptor = new StringBuilder();
            descriptor.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<project>\n<id>")
                    .append(groupId).append("_").append(artifactId).append("_").append(version).append("</id>\n<dependencies>\n");
            for (String dep : dependencies) {
                descriptor.append(dep).append("\n");
            }
            descriptor.append("</dependencies>\n</project>");
            zos.write(descriptor.toString().getBytes());
            zos.closeEntry();
        }
    }

    @Test
    public void testUpdateArtifactDependencies_AddsNewDependencies() throws Exception {

        File depFile = tempFolder.newFile("artifacts.xml");
        String xml =
                "<?xml version=\"1.0\"?>"
                        + "<artifacts>"
                        + "<artifact>"
                        + "<dependency artifact=\"foo\" version=\"1.0\" include=\"true\" serverRole=\"role1\"/>"
                        + "</artifact>"
                        + "</artifacts>";
        try (FileWriter w = new FileWriter(depFile)) {
            w.write(xml);
        }
        List<ArtifactDependency> deps = new ArrayList<>();
        CAppDependencyResolver.updateArtifactDependencies(depFile, deps, new CAppDependencyResolverTest.MockCARMojo());

        assertEquals(1, deps.size());
        ArtifactDependency ad = deps.get(0);
        assertEquals("foo", ad.getArtifact());
        assertEquals("1.0", ad.getVersion());
        assertEquals("role1", ad.getServerRole());
        assertEquals(Boolean.TRUE, ad.getInclude());
    }

    @Test
    public void testUpdateArtifactDependencies_SkipsDuplicateDependencies() throws Exception {

        File depFile = tempFolder.newFile("artifacts.xml");
        String xml =
                "<?xml version=\"1.0\"?>" +
                        "<artifacts>" +
                        "<artifact>" +
                        "<dependency artifact=\"foo\" version=\"1.0\" include=\"true\" serverRole=\"role1\"/>" +
                        "</artifact>" +
                        "</artifacts>";
        try (FileWriter w = new FileWriter(depFile)) {
            w.write(xml);
        }

        List<ArtifactDependency> deps = new ArrayList<>();
        // Add the same dependency manually
        deps.add(new ArtifactDependency("foo", "1.0", "role1", true));

        // Capture error log
        final StringBuilder errorLog = new StringBuilder();
        CARMojo mojo = new MockCARMojo() {
            @Override
            public void logError(String msg) {
                errorLog.append(msg);
            }
        };

        CAppDependencyResolver.updateArtifactDependencies(depFile, deps, mojo);

        // Should not add duplicate
        assertEquals(1, deps.size());
        // Assert error log contains duplicate message
        assertTrue(errorLog.toString().contains("already exists in between dependencies or between a dependency and your project."));
    }

    @Test
    public void testUpdateArtifactDependencies_MissingFile() {
        File missing = new File(tempFolder.getRoot(), "doesnotexist.xml");
        List<ArtifactDependency> deps = new ArrayList<>();
        CAppDependencyResolver.updateArtifactDependencies(missing, deps, new CAppDependencyResolverTest.MockCARMojo());
        // No exception, no dependencies added
        assertTrue(deps.isEmpty());
    }

    @Test
    public void testHandleConfigPropertiesFile_CopiesWhenTargetDirDoesNotExist() throws Exception {
        File srcDir = tempFolder.newFolder("srcConfigNew");
        File targetDir = new File(tempFolder.getRoot(), "targetConfigNew"); // Do not create this directory
        File srcConfig = new File(srcDir, "config.properties");
        try (FileWriter writer = new FileWriter(srcConfig)) {
            writer.write("keyA=valueA\n");
        }
        // Ensure targetDir does not exist
        if (targetDir.exists()) {
            for (File file : targetDir.listFiles()) {
                file.delete();
            }
            targetDir.delete();
        }

        CAppDependencyResolver.handleConfigPropertiesFile(srcDir, targetDir, new CAppDependencyResolverTest.MockCARMojo());

        File targetConfig = new File(targetDir, "config.properties");
        assertTrue(targetDir.exists());
        assertTrue(targetConfig.exists());
        try (BufferedReader reader = new BufferedReader(new FileReader(targetConfig))) {
            assertEquals("keyA=valueA", reader.readLine());
        }
    }

    @Test
    public void testHandleConfigPropertiesFile_CopiesWhenTargetDoesNotExist() throws Exception {

        File srcDir = tempFolder.newFolder("srcConfig");
        File targetDir = tempFolder.newFolder("targetConfig");
        File srcConfig = new File(srcDir, "config.properties");
        try (FileWriter writer = new FileWriter(srcConfig)) {
            writer.write("key1=value1\n");
        }
        File targetConfig = new File(targetDir, "config.properties");
        if (targetConfig.exists()) targetConfig.delete();

        CAppDependencyResolver.handleConfigPropertiesFile(srcDir, targetDir, new CAppDependencyResolverTest.MockCARMojo());

        assertTrue(targetConfig.exists());
        try (BufferedReader reader = new BufferedReader(new FileReader(targetConfig))) {
            assertEquals("key1=value1", reader.readLine());
        }
    }

    @Test
    public void testHandleConfigPropertiesFile_MergesWhenTargetExists() throws Exception {

        File srcDir = tempFolder.newFolder("srcConfig2");
        File targetDir = tempFolder.newFolder("targetConfig2");
        File srcConfig = new File(srcDir, "config.properties");
        File targetConfig = new File(targetDir, "config.properties");
        try (FileWriter writer = new FileWriter(srcConfig)) {
            writer.write("key1=value1\nkey2=value2\n");
        }
        try (FileWriter writer = new FileWriter(targetConfig)) {
            writer.write("key2=value2\nkey3=value3\n");
        }

        CAppDependencyResolver.handleConfigPropertiesFile(srcDir, targetDir, new CAppDependencyResolverTest.MockCARMojo());

        Set<String> lines = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(targetConfig))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        assertTrue(lines.contains("key1=value1"));
        assertTrue(lines.contains("key2=value2"));
        assertTrue(lines.contains("key3=value3"));
        assertEquals(3, lines.size());
    }

    @Test
    public void testHandleConfigPropertiesFile_SkipsIfNoSourceConfig() throws Exception {

        File srcDir = tempFolder.newFolder("srcConfig3");
        File targetDir = tempFolder.newFolder("targetConfig3");
        // No config.properties in srcDir
        File targetConfig = new File(targetDir, "config.properties");
        try (FileWriter writer = new FileWriter(targetConfig)) {
            writer.write("keyX=valueX\n");
        }

        CAppDependencyResolver.handleConfigPropertiesFile(srcDir, targetDir, new CAppDependencyResolverTest.MockCARMojo());

        // Target file should remain unchanged
        try (BufferedReader reader = new BufferedReader(new FileReader(targetConfig))) {
            assertEquals("keyX=valueX", reader.readLine());
            assertNull(reader.readLine());
        }
    }

    @Test
    public void testMergePropertiesFiles_MergesUniqueLines() throws Exception {

        File source = tempFolder.newFile("source.properties");
        File target = tempFolder.newFile("target.properties");
        try (FileWriter sw = new FileWriter(source)) {
            sw.write("a=1\nb=2\n");
        }
        try (FileWriter tw = new FileWriter(target)) {
            tw.write("b=2\nc=3\n");
        }

        CAppDependencyResolver.mergePropertiesFiles(source, target, new CAppDependencyResolverTest.MockCARMojo());

        Set<String> lines = new HashSet<>();
        try (BufferedReader r = new BufferedReader(new FileReader(target))) {
            String line;
            while ((line = r.readLine()) != null) lines.add(line);
        }
        assertTrue(lines.contains("a=1"));
        assertTrue(lines.contains("b=2"));
        assertTrue(lines.contains("c=3"));
        assertEquals(3, lines.size());
    }

    @Test
    public void testMergePropertiesFiles_SourceEmpty() throws Exception {

        File source = tempFolder.newFile("sourceEmpty.properties");
        File target = tempFolder.newFile("targetNonEmpty.properties");
        try (FileWriter tw = new FileWriter(target)) {
            tw.write("x=1\ny=2\n");
        }

        CAppDependencyResolver.mergePropertiesFiles(source, target, new CAppDependencyResolverTest.MockCARMojo());

        List<String> lines = Files.readAllLines(target.toPath());
        assertTrue(lines.contains("x=1"));
        assertTrue(lines.contains("y=2"));
        assertEquals(2, lines.size());
    }

    @Test
    public void testMergePropertiesFiles_TargetEmpty() throws Exception {

        File source = tempFolder.newFile("sourceNonEmpty.properties");
        File target = tempFolder.newFile("targetEmpty.properties");
        try (FileWriter sw = new FileWriter(source)) {
            sw.write("foo=bar\n");
        }

        CAppDependencyResolver.mergePropertiesFiles(source, target, new CAppDependencyResolverTest.MockCARMojo());

        List<String> lines = Files.readAllLines(target.toPath());
        assertTrue(lines.contains("foo=bar"));
        assertEquals(1, lines.size());
    }

    @Test
    public void testMergePropertiesFiles_DuplicateLines() throws Exception {

        File source = tempFolder.newFile("sourceDup.properties");
        File target = tempFolder.newFile("targetDup.properties");
        try (FileWriter sw = new FileWriter(source)) {
            sw.write("dup=1\n");
        }
        try (FileWriter tw = new FileWriter(target)) {
            tw.write("dup=1\n");
        }

        CAppDependencyResolver.mergePropertiesFiles(source, target, new CAppDependencyResolverTest.MockCARMojo());

        List<String> lines = Files.readAllLines(target.toPath());
        assertEquals(1, lines.size());
        assertEquals("dup=1", lines.get(0));
    }

    @Test
    public void testGetResolvedDependentCAppFiles_ResolvesAllDependencies() throws Exception {

        File mavenRepo = tempFolder.newFolder("m2repo");
        File targetDir = tempFolder.newFolder("target");
        File dependenciesDir = tempFolder.newFolder("dependencies");
        String groupId = "com.example";

        // carA depends on carB, carB depends on carC
        File carA = new File(targetDir, "carA-1.0.0.car");
        String depB = "<dependency groupId=\"com.example\" artifactId=\"carB\" version=\"1.0.0\" type=\"car\"/>";
        String depC = "<dependency groupId=\"com.example\" artifactId=\"carC\" version=\"1.0.0\" type=\"car\"/>";
        createCarFileWithDescriptor(carA, groupId, "carA", "1.0.0", depB, depC);

        File carB = new File(dependenciesDir, "carB-1.0.0.car");
        String depD = "<dependency groupId=\"com.example\" artifactId=\"carD\" version=\"1.0.0\" type=\"car\"/>";
        createCarFileWithDescriptor(carB, groupId, "carB", "1.0.0", depD);

        File carC = new File(dependenciesDir, "carC-1.0.0.car");
        createCarFileWithDescriptor(carC, groupId, "carC", "1.0.0");

        File carD = new File(dependenciesDir, "carD-1.0.0.car");
        createCarFileWithDescriptor(carD, groupId, "carD", "1.0.0");

        ArrayList<File> result = CAppDependencyResolver.getResolvedDependentCAppFiles(
                mavenRepo.getAbsolutePath(), dependenciesDir, "carA", "1.0.0",
                new CAppDependencyResolverTest.MockCARMojo());

        assertTrue(result.contains(carB));
        assertTrue(result.contains(carC));
        assertTrue(result.contains(carD));
        assertEquals(3, result.size());
    }

    @Test
    public void testCollectDependentCAppFiles_ResolvesTransitiveDependencies() throws Exception {

        File dependenciesDir = tempFolder.newFolder("dependencies");
        String groupId = "com.example";

        // Create carA depends on carB
        File carA = new File(dependenciesDir, "carA-1.0.0.car");
        String depB = "<dependency groupId=\"com.example\" artifactId=\"carB\" version=\"1.0.0\" type=\"car\"/>";
        createCarFileWithDescriptor(carA, groupId, "carA", "1.0.0", depB);

        // Create carB depends on carC
        File carB = new File(dependenciesDir, "carB-1.0.0.car");
        String depC = "<dependency groupId=\"com.example\" artifactId=\"carC\" version=\"1.0.0\" type=\"car\"/>";
        createCarFileWithDescriptor(carB, groupId, "carB", "1.0.0", depC);

        // Create carC with no dependencies
        File carC = new File(dependenciesDir, "carC-1.0.0.car");
        createCarFileWithDescriptor(carC, groupId, "carC", "1.0.0");

        ArrayList<File> cAppFiles = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        MockCARMojo mojo = new MockCARMojo();

        // Start with carA
        CAppDependencyResolver.collectDependentCAppFiles(
                tempFolder.getRoot().getAbsolutePath(), dependenciesDir, carA, cAppFiles, visited, mojo);

        // Should collect carB and carC (transitive)
        assertTrue(cAppFiles.contains(carB));
        assertTrue(cAppFiles.contains(carC));
    }

    @Test
    public void testCollectDependentCAppFiles_SkipsAlreadyVisited() throws Exception {

        File dependenciesDir = tempFolder.newFolder("dependencies");
        String groupId = "com.example";

        File carA = new File(dependenciesDir, "carA-1.0.0.car");
        String depB = "<dependency groupId=\"com.example\" artifactId=\"carB\" version=\"1.0.0\" type=\"car\"/>";
        String depC = "<dependency groupId=\"com.example\" artifactId=\"carC\" version=\"1.0.0\" type=\"car\"/>";
        createCarFileWithDescriptor(carA, groupId, "carA", "1.0.0", depB, depC);

        File carB = new File(dependenciesDir, "carB-1.0.0.car");
        createCarFileWithDescriptor(carB, groupId, "carB", "1.0.0", depC);

        File carC = new File(dependenciesDir, "carC-1.0.0.car");
        createCarFileWithDescriptor(carC, groupId, "carC", "1.0.0");

        ArrayList<File> cAppFiles = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        MockCARMojo mojo = new MockCARMojo();

        CAppDependencyResolver.collectDependentCAppFiles(
                tempFolder.getRoot().getAbsolutePath(), dependenciesDir, carA, cAppFiles, visited, mojo);

        // Should not loop infinitely
        assertEquals(2, cAppFiles.size());
        assertTrue(cAppFiles.contains(carB));
        assertTrue(cAppFiles.contains(carC));
    }

    @Test
    public void testCollectDependentCAppFiles_MissingDependency() throws Exception {
        File dependenciesDir = tempFolder.newFolder("dependencies");
        String groupId = "com.example";

        // carA depends on carMissing (not present)
        File carA = new File(dependenciesDir, "carA-1.0.0.car");
        String depMissing = "<dependency groupId=\"com.example\" artifactId=\"carMissing\" version=\"1.0.0\" type=\"car\"/>";
        createCarFileWithDescriptor(carA, groupId, "carA", "1.0.0", depMissing);

        ArrayList<File> cAppFiles = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        MockCARMojo mojo = new MockCARMojo();

        CAppDependencyResolver.collectDependentCAppFiles(
                tempFolder.getRoot().getAbsolutePath(), dependenciesDir, carA, cAppFiles, visited, mojo);

        // No additional files should be added
        assertTrue(cAppFiles.isEmpty());
    }

    @Test
    public void testCollectDependentCAppFiles_FetchesFromMavenRepo() throws Exception {
        File mavenRepo = tempFolder.newFolder("m2repo");
        File dependenciesDir = tempFolder.newFolder("dependencies");
        String groupId = "com.example";
        String artifactId = "carB";
        String version = "1.0.0";

        // carA depends on carB
        File carA = new File(dependenciesDir, "carA-1.0.0.car");
        String depB = "<dependency groupId=\"com.example\" artifactId=\"carB\" version=\"1.0.0\" type=\"car\"/>";
        createCarFileWithDescriptor(carA, groupId, "carA", "1.0.0", depB);

        // carB exists only in the Maven repo
        String relPath = groupId.replace('.', File.separatorChar) + File.separator + artifactId + File.separator + version;
        File carBDir = new File(mavenRepo, relPath);
        carBDir.mkdirs();
        File carB = new File(carBDir, artifactId + "-" + version + ".car");
        createCarFileWithDescriptor(carB, groupId, artifactId, version);

        ArrayList<File> cAppFiles = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        MockCARMojo mojo = new MockCARMojo();

        // Should fetch carB from Maven repo and add to cAppFiles
        CAppDependencyResolver.collectDependentCAppFiles(
                mavenRepo.getAbsolutePath(), dependenciesDir, carA, cAppFiles, visited, mojo);

        File expectedCarB = new File(dependenciesDir, "carB-1.0.0.car");
        assertTrue(cAppFiles.contains(expectedCarB));
        assertTrue(expectedCarB.exists());
    }

    @Test
    public void testFindCarFileInDependencies_FindsMatchingCar() throws Exception {
        File dependenciesDir = tempFolder.newFolder("dependencies");
        String groupId = "com.example";
        String artifactId = "my-artifact";
        String version = "1.0.0";
        String idValue = groupId + "_" + artifactId + "_" + version;

        File carFile = new File(dependenciesDir, artifactId + "-" + version + ".car");
        writeDescriptorXmlToCar(carFile, (
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<project>\n" +
                        "    <id>com.example_my-artifact_1.0.0</id>\n" +
                        "    <dependencies>\n" +
                        "        <dependency groupId=\"com.example\" artifactId=\"TestProjectConfigs1\" version=\"1.0.0-SNAPSHOT\" type=\"car\"/>\n" +
                        "        <dependency groupId=\"com.example\" artifactId=\"TestProjectRegistryResources1\" version=\"1.0.0-SNAPSHOT\" type=\"car\"/>\n" +
                        "    </dependencies>\n" +
                        "</project>\n"
        ));

        File found = CAppDependencyResolver.findCarFileInDependencies(dependenciesDir, artifactId, version);
        assertNotNull(found);
        assertEquals(carFile.getName(), found.getName());
    }

    @Test
    public void testFindCarFileInDependencies_NoMatchReturnsNull() throws Exception {
        File dependenciesDir = tempFolder.newFolder("dependencies");
        String groupId = "com.example";
        String artifactId = "my-artifact";
        String version = "1.2.3";

        // No .car files in directory
        File found = CAppDependencyResolver.findCarFileInDependencies(dependenciesDir, artifactId, version);
        assertNull(found);
    }

    @Test
    public void testFetchCarFileFromMavenRepoFileExists() throws Exception {
        File mavenRepo = tempFolder.newFolder("m2repo");
        File dependenciesDir = tempFolder.newFolder("dependencies");
        String groupId = "com.example";
        String artifactId = "test";
        String version = "1.0.0";
        String relPath = groupId.replace('.', File.separatorChar) + File.separator + artifactId + File.separator + version;
        File carDir = new File(mavenRepo, relPath);
        carDir.mkdirs();
        File carFile = new File(carDir, artifactId + "-" + version + ".car");
        writeDescriptorXmlToCar(carFile, (
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project>\n" +
                "    <id>com.example_test_1.0.0-SNAPSHOT</id>\n" +
                "    <dependencies>\n" +
                "        <dependency groupId=\"com.example\" artifactId=\"TestProjectConfigs1\" version=\"1.0.0-SNAPSHOT\" type=\"car\"/>\n" +
                "        <dependency groupId=\"com.example\" artifactId=\"TestProjectRegistryResources1\" version=\"1.0.0-SNAPSHOT\" type=\"car\"/>\n" +
                "    </dependencies>\n" +
                "</project>\n"
        ));

        File result = CAppDependencyResolver.fetchCarFileFromMavenRepo(
                mavenRepo.getAbsolutePath(), dependenciesDir, groupId, artifactId, version);
        File expectedFile = new File(dependenciesDir, carFile.getName());


        assertNotNull(result);
        assertTrue(result.exists());
        assertEquals("test-1.0.0.car", result.getName());
        assertTrue(expectedFile.exists());
        assertTrue(new java.util.zip.ZipFile(expectedFile).getEntry("descriptor.xml") != null);
    }

    @Test
    public void testUnzipFile() throws IOException {
        // Create a zip file in memory with a test entry
        File tempZip = tempFolder.newFile("test.car");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempZip))) {
            ZipEntry entry = new ZipEntry("test.txt");
            zos.putNextEntry(entry);
            zos.write("hello".getBytes());
            zos.closeEntry();
        }
        File extractDir = tempFolder.newFolder("extracted");

        CAppDependencyResolver.unzipFile(tempZip, extractDir);

        File extractedFile = new File(extractDir, "test.txt");
        assertTrue(extractDir.exists() && extractDir.isDirectory());
        assertTrue(extractedFile.exists());
    }
}
