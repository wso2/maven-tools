# Synapse Static Code Analysis Plugin Configuration

This document describes the configuration options available for the Synapse Static Code Analysis Maven Plugin.

## Plugin Parameters

| Parameter Name                        | Property                                 | Type      | Default Value                      | Description                                                |
|---------------------------------------|------------------------------------------|-----------|------------------------------------|------------------------------------------------------------|
| basedir                              | synapse.analyzer.basedir                 | File      | ${project.basedir}                 | The base directory to analyze.                             |
| outputDirectory                      | synapse.analyzer.outputDirectory         | String    | target/static-analysis-report       | Directory where report files will be written.              |
| reportFormats                        | synapse.analyzer.reportFormats           | String[]  | html                               | Report formats to generate (html, json, sarif).            |
| failBuildOnIssues                    | synapse.analyzer.failBuildOnIssues       | boolean   | false                              | Fail the build if issues are found above threshold.        |
| skip                                 | synapse.analyzer.skip                    | boolean   | false                              | Skip running the analysis.                                 |
| includeFiles                         | synapse.analyzer.includeFiles            | String[]  |                                    | Filter: files to include in analysis (glob patterns).      |
| excludeFiles                         | synapse.analyzer.excludeFiles            | String[]  |                                    | Filter: files to exclude from analysis (glob patterns).    |
| verbose                              | synapse.analyzer.verbose                 | boolean   | false                              | Enable verbose logging.                                    |

## Example Usage in `pom.xml`

```xml
<plugin>
    <groupId>org.wso2.maven</groupId>
    <artifactId>synapse-static-code-analysis-plugin</artifactId>
    <version>5.4.1</version>
    <configuration>
        <outputDirectory>target/static-analysis-report</outputDirectory>
        <reportFormats>
            <reportFormat>html</reportFormat>
            <reportFormat>json</reportFormat>
        </reportFormats>
        <failBuildOnIssues>true</failBuildOnIssues>
        <skip>false</skip>
        <includeFiles>
            <includeFile>src/main/wso2/**/*.xml</includeFile>
        </includeFiles>
        <excludeFiles>
            <excludeFile>src/test/**</excludeFile>
        </excludeFiles>
        <verbose>true</verbose>
    </configuration>
    <executions>
        <execution>
            <phase>validate</phase>
            <goals>
                <goal>analyze</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

## How to Use Properties

You can configure the plugin properties in your Maven `pom.xml` file under the `<configuration>` section of the plugin, or you can inject them via command line using the `-D` syntax when running Maven commands.

### Example: Injecting Properties via Command Line

You can inject any property at build time using the `-D` flag. For example:

```
mvn clean install -Dsynapse.analyzer.failBuildOnIssues=true -Dsynapse.analyzer.verbose=true
```

This will run the analysis with `failBuildOnIssues` and `verbose` enabled.

## Notes
- `reportFormats` supports multiple formats: `html`, `json`, `sarif`.
- `includeFiles` and `excludeFiles` accept glob patterns.
- Set `failBuildOnIssues` to `true` to break the build on issues.
- Use `verbose` for detailed logs during analysis.

