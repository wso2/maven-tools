package org.wso2.maven;

import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;

/**
 * Generates the component.xml files for the components.
 */
public class ComponentXmlGenerator {

    /**
     * Generates the component.xml files for all components in the components directory.
     */
    public static void generateComponentXmls(ConnectorMojo connectorMojo) {

        String componentsPath = "src" + File.separator + "main" + File.separator + "resources";

        // Get the list of folders in the components directory
        File componentsFolder = new File(componentsPath);
        for (File folder : componentsFolder.listFiles()) {
            if (folder.isDirectory()) {
                // check if folder contains component.xml
                if (new File(folder.getPath() + File.separator + "component.xml").exists()) {
                    continue;
                }
                // check if folder contains xml files
                boolean containsXml = false;
                for (File file : folder.listFiles()) {
                    if (file.isFile() && file.getName().endsWith(".xml")) {
                        containsXml = true;
                        break;
                    }
                }
                if (containsXml) {
                    String componentName = folder.getName();
                    generateComponentXml(componentName, folder.getPath(), connectorMojo);
                }
            }
        }
    }

    private static void generateComponentXml(String componentName, String folderPath, ConnectorMojo connectorMojo) {

        String targetComponentsPath = "target" + File.separator + "classes" + File.separator + componentName;

        // Specify the output file name
        String outputFileName = targetComponentsPath + File.separator + "component.xml";

        try (FileWriter writer = new FileWriter(outputFileName)) {
            // Write the header, comment, and start the <component> tag
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<!-- Auto-generated file. Do not modify manually. -->\n");
            writer.write("<component name=\"" + componentName + "\" type=\"synapse/template\">\n");
            writer.write("    <subComponents>\n");

            // Get the list of files in the folder
            File folder = new File(folderPath);
            File[] files = folder.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {

                    return name.endsWith(".xml");
                }
            });
            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName();
                    String component = fileName.substring(0, fileName.lastIndexOf('.')); // Remove .xml extension
                    String description = extractDescription(file);

                    // Write each component entry
                    writer.write("        <component name=\"" + component + "\">\n");
                    writer.write("            <file>" + fileName + "</file>\n");
                    writer.write("            <description>" + description + "</description>\n");
                    writer.write("        </component>\n");
                }
            }
            // Close the <subComponents> and <component> tags
            writer.write("    </subComponents>\n");
            writer.write("</component>\n");
        } catch (Exception e) {
            connectorMojo.getLog().error("Error generating component.xml for " + componentName + ": " + e.getMessage());
        }
    }

    /**
     * Extracts the description from the given XML file.
     * If no description element is found, returns a default message.
     *
     * @param file The XML file to process.
     * @return The extracted description or a default message.
     */
    private static String extractDescription(File file) throws Exception {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(file);

        // Normalize the document
        document.getDocumentElement().normalize();

        // Get the description element
        NodeList nodeList = document.getElementsByTagName("description");
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent().trim();
        }
        return "";
    }
}
