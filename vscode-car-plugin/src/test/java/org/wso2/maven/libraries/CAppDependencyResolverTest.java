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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
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
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

public class CAppDependencyResolverTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    private CAppDependencyResolver resolver;

    public static List<String> readAllLines(File file) throws IOException {

        List<String> lines = new ArrayList<String>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return lines;
    }

    public static List<String[]> extractDependencies(File pomFile) throws Exception {

        List<String[]> dependencies = new ArrayList<String[]>();

        try (BufferedReader reader = new BufferedReader(new FileReader(pomFile))) {
            String line;
            boolean inDependency = false;

            String artifactId = "";
            String version = "";
            String type = "";

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.startsWith("<dependency")) {
                    inDependency = true;
                    artifactId = "";
                    version = "";
                    type = "";
                } else if (line.startsWith("</dependency>")) {
                    inDependency = false;

                    if (!artifactId.isEmpty() && !version.isEmpty() && !type.isEmpty()) {
                        dependencies.add(new String[]{artifactId, version, type});
                    }
                } else if (inDependency) {
                    if (line.startsWith("<artifactId>") && line.endsWith("</artifactId>")) {
                        artifactId = extractTagValue(line, "artifactId");
                    } else if (line.startsWith("<version>") && line.endsWith("</version>")) {
                        version = extractTagValue(line, "version");
                    } else if (line.startsWith("<type>") && line.endsWith("</type>")) {
                        type = extractTagValue(line, "type");
                    }
                }
            }
        }

        return dependencies;
    }

    private static String extractTagValue(String line, String tagName) {

        String openTag = "<" + tagName + ">";
        String closeTag = "</" + tagName + ">";
        return line.substring(openTag.length(), line.length() - closeTag.length()).trim();
    }

    public static MockedStatic<CAppDependencyResolver> setupCAppDependencyResolverMock(
            final TemporaryFolder tempFolder) {

        try {
            MockedStatic<CAppDependencyResolver> mockedStatic = Mockito.mockStatic(CAppDependencyResolver.class);
            mockedStatic.when(new MockedStatic.Verification() {
                public void apply() throws MavenInvocationException, MojoExecutionException {

                    CAppDependencyResolver.executeDependencyCopy(any(File.class), any(File.class), any(File.class));
                }
            }).thenAnswer(new org.mockito.stubbing.Answer<Object>() {
                public Object answer(org.mockito.invocation.InvocationOnMock invocation) throws Throwable {

                    File projectDir = (File) invocation.getArgument(0);
                    File pomFile = (File) invocation.getArgument(1);
                    File outputDir = (File) invocation.getArgument(2);

                    // Parse dependencies from pomFile
                    List<String[]> dependencies = extractDependencies(pomFile);

                    // Create dummy .car files
                    try {
                        for (String[] dep : dependencies) {
                            String artifactId = dep[0];
                            String version = dep[1];
                            String type = dep[2];

                            File sourceFile = new File(tempFolder.getRoot(), artifactId + "-" + version + "." + type);
                            File depFile = new File(outputDir, artifactId + "-" + version + "." + type);
                            Files.copy(sourceFile.toPath(), depFile.toPath());
                        }
                    } catch (IOException e) {
                        throw new MavenInvocationException("Failed to copy dependencies", e);
                    }
                    return null;
                }
            });

            mockedStatic.when(new MockedStatic.Verification() {
                @Override
                public void apply() throws Exception {

                    CAppDependencyResolver.fetchCarFileFromMavenRepo(any(File.class), any(File.class), anyString(),
                            anyString(), anyString(), any(CARMojo.class));
                }
            }).thenCallRealMethod();

            mockedStatic.when(new MockedStatic.Verification() {
                @Override
                public void apply() throws Exception {

                    CAppDependencyResolver.getResolvedDependentCAppFiles(any(File.class), any(File.class), anyString(),
                            anyString(), any(CARMojo.class));
                }
            }).thenCallRealMethod();

            mockedStatic.when(new MockedStatic.Verification() {
                @Override
                public void apply() throws Exception {

                    CAppDependencyResolver.collectDependentCAppFiles(any(File.class), any(File.class), any(File.class),
                            any(ArrayList.class), any(Set.class), any(CARMojo.class));
                }
            }).thenCallRealMethod();
            return mockedStatic;
        } catch (Exception e) {
            throw new RuntimeException("Failed to setup CAppDependencyResolver mock", e);
        }
    }

    private void writeDescriptorXmlToCar(File carFile, String descriptorContent) throws IOException {

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(carFile))) {
            ZipEntry entry = new ZipEntry("descriptor.xml");
            zos.putNextEntry(entry);
            zos.write(descriptorContent.getBytes());
            zos.closeEntry();
        }
    }

    private void createCarFileWithDescriptor(File carFile, String groupId, String artifactId, String version,
                                             String... dependencies) throws Exception {

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(carFile))) {
            zos.putNextEntry(new ZipEntry("descriptor.xml"));
            StringBuilder descriptor = new StringBuilder();
            descriptor.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<project>\n<id>")
                    .append(groupId).append("_").append(artifactId).append("_").append(version)
                    .append("</id>\n<dependencies>\n");
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

        CARMojo mojo = new MockCARMojo();
        CAppDependencyResolver.updateArtifactDependencies(depFile, deps, mojo);

        // Should not add duplicate
        assertEquals(1, deps.size());
    }

    @Test
    public void testUpdateArtifactDependencies_MissingFile() throws Exception {

        File missing = new File(tempFolder.getRoot(), "doesnotexist.xml");
        List<ArtifactDependency> deps = new ArrayList<>();
        try {
            CAppDependencyResolver.updateArtifactDependencies(missing, deps,
                    new CAppDependencyResolverTest.MockCARMojo());
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Artifacts XML file not found:"));
        }
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

        CAppDependencyResolver.handleConfigPropertiesFile(srcDir, targetDir);

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

        CAppDependencyResolver.handleConfigPropertiesFile(srcDir, targetDir);

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

        CAppDependencyResolver.handleConfigPropertiesFile(srcDir, targetDir);

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

        CAppDependencyResolver.handleConfigPropertiesFile(srcDir, targetDir);

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

        CAppDependencyResolver.mergePropertiesFiles(source, target);

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

        CAppDependencyResolver.mergePropertiesFiles(source, target);

        List<String> lines = readAllLines(target);
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

        CAppDependencyResolver.mergePropertiesFiles(source, target);

        List<String> lines = readAllLines(target);
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

        CAppDependencyResolver.mergePropertiesFiles(source, target);

        List<String> lines = readAllLines(target);
        assertEquals(1, lines.size());
        assertEquals("dup=1", lines.get(0));
    }

    @Test
    public void testGetResolvedDependentCAppFiles_ResolvesAllDependencies() throws Exception {

        String groupId = "com.example";
        File dependenciesDir = tempFolder.newFolder("dependencies");

        // carA depends on carB, carB depends on carC
        File carA = new File(dependenciesDir, "carA-1.0.0.car");
        String depB = "<dependency groupId=\"com.example\" artifactId=\"carB\" version=\"1.0.0\" type=\"car\"/>";
        String depC = "<dependency groupId=\"com.example\" artifactId=\"carC\" version=\"1.0.0\" type=\"car\"/>";
        createCarFileWithDescriptor(carA, groupId, "carA", "1.0.0", depB, depC);

        File carB = new File(tempFolder.getRoot(), "carB-1.0.0.car");
        String depD = "<dependency groupId=\"com.example\" artifactId=\"carD\" version=\"1.0.0\" type=\"car\"/>";
        createCarFileWithDescriptor(carB, groupId, "carB", "1.0.0", depD);
        File fetchedCarB = new File(dependenciesDir, "carB-1.0.0.car");

        File carC = new File(tempFolder.getRoot(), "carC-1.0.0.car");
        createCarFileWithDescriptor(carC, groupId, "carC", "1.0.0");
        File fetchedCarC = new File(dependenciesDir, "carC-1.0.0.car");

        File carD = new File(dependenciesDir, "carD-1.0.0.car");
        createCarFileWithDescriptor(carD, groupId, "carD", "1.0.0");

        try (MockedStatic<CAppDependencyResolver> mockedStatic = setupCAppDependencyResolverMock(tempFolder)) {
            ArrayList<File> result =
                    CAppDependencyResolver.getResolvedDependentCAppFiles(tempFolder.getRoot(), dependenciesDir, "carA",
                            "1.0.0", new CAppDependencyResolverTest.MockCARMojo());

            assertTrue(result.contains(carA));
            assertTrue(result.contains(fetchedCarB));
            assertTrue(result.contains(fetchedCarC));
            assertTrue(result.contains(carD));
            assertEquals(4, result.size());
        }
    }

    @Test
    public void testCollectDependentCAppFiles_ResolvesTransitiveDependencies() throws Exception {

        String groupId = "com.example";
        File dependenciesDir = tempFolder.newFolder("dependencies");

        // Create carA depends on carB
        File carA = new File(dependenciesDir, "carA-1.0.0.car");
        String depB = "<dependency groupId=\"com.example\" artifactId=\"carB\" version=\"1.0.0\" type=\"car\"/>";
        createCarFileWithDescriptor(carA, groupId, "carA", "1.0.0", depB);

        // Create carB depends on carC
        File carB = new File(tempFolder.getRoot(), "carB-1.0.0.car");
        String depC = "<dependency groupId=\"com.example\" artifactId=\"carC\" version=\"1.0.0\" type=\"car\"/>";
        createCarFileWithDescriptor(carB, groupId, "carB", "1.0.0", depC);
        File fetchedCarB = new File(dependenciesDir, "carB-1.0.0.car");

        // Create carC with no dependencies
        File carC = new File(tempFolder.getRoot(), "carC-1.0.0.car");
        createCarFileWithDescriptor(carC, groupId, "carC", "1.0.0");
        File fetchedCarC = new File(dependenciesDir, "carC-1.0.0.car");

        ArrayList<File> cAppFiles = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        MockCARMojo mojo = new MockCARMojo();

        try (MockedStatic<CAppDependencyResolver> mockedStatic = setupCAppDependencyResolverMock(tempFolder)) {
            // Start with carA
            CAppDependencyResolver.collectDependentCAppFiles(tempFolder.getRoot(), dependenciesDir, carA, cAppFiles,
                    visited, mojo);

            // Should collect carB and carC (transitive)
            assertTrue(cAppFiles.contains(fetchedCarB));
            assertTrue(cAppFiles.contains(fetchedCarC));
        }
    }

    @Test
    public void testCollectDependentCAppFiles_SkipsAlreadyVisited() throws Exception {

        String groupId = "com.example";
        File dependenciesDir = tempFolder.newFolder("dependencies");

        File carA = new File(tempFolder.getRoot(), "carA-1.0.0.car");
        String depB = "<dependency groupId=\"com.example\" artifactId=\"carB\" version=\"1.0.0\" type=\"car\"/>";
        String depC = "<dependency groupId=\"com.example\" artifactId=\"carC\" version=\"1.0.0\" type=\"car\"/>";
        createCarFileWithDescriptor(carA, groupId, "carA", "1.0.0", depB, depC);

        File carB = new File(tempFolder.getRoot(), "carB-1.0.0.car");
        createCarFileWithDescriptor(carB, groupId, "carB", "1.0.0", depC);
        File fetchedCarB = new File(dependenciesDir, "carB-1.0.0.car");

        File carC = new File(tempFolder.getRoot(), "carC-1.0.0.car");
        createCarFileWithDescriptor(carC, groupId, "carC", "1.0.0");
        File fetchedCarC = new File(dependenciesDir, "carC-1.0.0.car");

        ArrayList<File> cAppFiles = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        MockCARMojo mojo = new MockCARMojo();

        try (MockedStatic<CAppDependencyResolver> mockedStatic = setupCAppDependencyResolverMock(tempFolder)) {
            CAppDependencyResolver.collectDependentCAppFiles(tempFolder.getRoot(), dependenciesDir, carA, cAppFiles,
                    visited, mojo);

            // Should not loop infinitely
            assertEquals(2, cAppFiles.size());
            assertTrue(cAppFiles.contains(fetchedCarB));
            assertTrue(cAppFiles.contains(fetchedCarC));
        }
    }

    @Test
    public void testCollectDependentCAppFiles_MissingDependency() throws Exception {

        String groupId = "com.example";

        // carA depends on carMissing (not present)
        File carA = new File(tempFolder.getRoot(), "carA-1.0.0.car");
        String depMissing =
                "<dependency groupId=\"com.example\" artifactId=\"carMissing\" version=\"1.0.0\" type=\"car\"/>";
        createCarFileWithDescriptor(carA, groupId, "carA", "1.0.0", depMissing);

        ArrayList<File> cAppFiles = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        MockCARMojo mojo = new MockCARMojo();

        try (MockedStatic<CAppDependencyResolver> mockedStatic = setupCAppDependencyResolverMock(tempFolder)) {
            File dependenciesDir = tempFolder.newFolder("dependencies");
            CAppDependencyResolver.collectDependentCAppFiles(tempFolder.getRoot(), dependenciesDir, carA, cAppFiles,
                    visited, mojo);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Error while fetching .car from Maven repo:"));
        }
    }

    @Test
    public void testCollectDependentCAppFiles_FetchesFromMavenRepo() throws Exception {

        String groupId = "com.example";
        String artifactId = "carB";
        String version = "1.0.0";

        // carA depends on carB
        File carA = new File(tempFolder.getRoot(), "carA-1.0.0.car");
        String depB = "<dependency groupId=\"com.example\" artifactId=\"carB\" version=\"1.0.0\" type=\"car\"/>";
        createCarFileWithDescriptor(carA, groupId, "carA", "1.0.0", depB);

        File carB = new File(tempFolder.getRoot(), artifactId + "-" + version + ".car");
        createCarFileWithDescriptor(carB, groupId, artifactId, version);

        ArrayList<File> cAppFiles = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        MockCARMojo mojo = new MockCARMojo();

        try (MockedStatic<CAppDependencyResolver> mockedStatic = setupCAppDependencyResolverMock(tempFolder)) {
            File dependenciesDir = tempFolder.newFolder("dependencies");
            CAppDependencyResolver.collectDependentCAppFiles(tempFolder.getRoot(), dependenciesDir, carA, cAppFiles,
                    visited, mojo);

            File expectedCarB = new File(dependenciesDir, "carB-1.0.0.car");
            assertTrue(cAppFiles.contains(expectedCarB));
            assertTrue(expectedCarB.exists());
        }
    }

    @Test
    public void testFindCarFileInDependencies_FindsMatchingCar() throws Exception {

        File dependenciesDir = tempFolder.newFolder("dependencies");

        String artifactId = "my-artifact";
        String version = "1.0.0";

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

        String artifactId = "my-artifact";
        String version = "1.2.3";

        // No .car files in directory
        File dependenciesDir = tempFolder.newFolder("dependencies");
        File found = CAppDependencyResolver.findCarFileInDependencies(dependenciesDir, artifactId, version);
        assertNull(found);
    }

    @Test
    public void testFetchCarFileFromMavenRepo_FileExists() throws Exception {

        String groupId = "com.example";
        String artifactId = "test";
        String version = "1.0.0";
        File carFile = new File(tempFolder.getRoot(), artifactId + "-" + version + ".car");
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

        try (MockedStatic<CAppDependencyResolver> mockedStatic = setupCAppDependencyResolverMock(tempFolder)) {
            File dependenciesDir = tempFolder.newFolder("dependencies");
            File result =
                    CAppDependencyResolver.fetchCarFileFromMavenRepo(tempFolder.getRoot(), dependenciesDir, groupId,
                            artifactId, version, new MockCARMojo());
            File expectedFile = new File(dependenciesDir, carFile.getName());

            assertNotNull(result);
            assertTrue(result.exists());
            assertEquals("test-1.0.0.car", result.getName());
            assertTrue(expectedFile.exists());
            assertNotNull(new ZipFile(expectedFile).getEntry("descriptor.xml"));
        }
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

    // Minimal mock for CARMojo
    static class MockCARMojo extends CARMojo {

        @Override
        public void logInfo(String msg) {

        }

        @Override
        public void logError(String msg) {

        }
    }
}
