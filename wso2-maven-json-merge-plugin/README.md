Usage
================

Used to merge multiple json files to a based json. Target json file path can provide to save result json to a file. Based json is used to override keys that are defined in other components.

ex:
-----------------
base: Base json file or product level key/values to define new configuration or override configuration
include: param array to define multiple json values to include it's content to base json
target: Destination file name
mergeChildren: Can merge it's children elements as well if make it to true. example- default json no need to merge children but unit-resolve.json need to merge it's children.

```
<plugin>
        <groupId>org.wso2.maven</groupId>
        <artifactId>wso2-maven-json-merge-plugin</artifactId>
        <version>5.2.0</version>
        <version>5.2.2-SNAPSHOT</version>
        <executions>
            <execution>
                <phase>prepare-package</phase>
                <goals>
                    <goal>merge</goal>
                </goals>
                <configuration>
                    <tasks>
                        <task>
                            <base>${basedir}/src/main/resources/conf/default.json</base>
                            <include>
                                <param>
                                    ${basedir}/../../p2-profile/product/target/wso2carbon-core-${carbon.kernel.version}/repository/resources/conf/default.json
                                </param>
                                <param>
                                    ${basedir}/../../p2-profile/product/target/wso2carbon-core-${carbon.kernel.version}/repository/resources/conf/.apimgt.core.default.json
                                </param>
                                <param>
                                    ${basedir}/../../p2-profile/product/target/wso2carbon-core-${carbon.kernel.version}/repository/resources/conf/databridge.agent.default.json
                                </param>
                                <param>
                                    ${basedir}/../../p2-profile/product/target/wso2carbon-core-${carbon.kernel.version}/repository/resources/conf/databridge.core.default.json
                                </param>
                                <param>
                                    ${basedir}/src/main/resources/conf/templates/repository/conf/identity/default.json
                                </param>
                            </include>
                            <target>
                                ${basedir}/../../p2-profile/product/target/wso2carbon-core-${carbon.kernel.version}/repository/resources/conf/default.json
                            </target>
                            <mergeChildren>false</mergeChildren>
                        </task>
                    </tasks>
                </configuration>
            </execution>
        </executions>
    </plugin>
```