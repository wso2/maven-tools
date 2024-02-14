package org.wso2.maven.Model;

public class ArtifactDetails {
    private final String directory;
    private final String type;
    private final String serverRole;
    public ArtifactDetails(String directory, String type, String serverRole) {
        this.directory = directory;
        this.type = type;
        this.serverRole = serverRole;
    }
    public String getDirectory() {
        return directory;
    }
    public String getType() {
        return type;
    }
    public String getServerRole() {
        return serverRole;
    }
}
