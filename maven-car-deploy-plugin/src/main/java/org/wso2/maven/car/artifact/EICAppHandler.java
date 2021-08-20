/*
 *
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.maven.car.artifact;

import org.apache.axiom.util.base64.Base64Utils;
import org.apache.commons.httpclient.Header;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.wso2.carbon.application.mgt.stub.upload.types.carbon.UploadedFileItem;
import org.wso2.carbon.stub.ApplicationAdminStub;
import org.wso2.carbon.stub.CarbonAppUploaderStub;
import org.wso2.maven.car.artifact.util.Constants;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.activation.DataHandler;

/**
 * This class implements CAppHandler to implement the CApp deploy/undeploy logic for EI Servers.
 */
public class EICAppHandler implements CAppHandler {

    private final Log logger;

    public EICAppHandler(Log logger) {

        this.logger = logger;
    }

    @Override
    public void deployCApp(String username, String password, String serverUrl, File carFile) throws Exception {

        CarbonAppUploaderStub carbonAppUploaderStub = getCarbonAppUploaderStub(username, password, serverUrl);
        UploadedFileItem uploadedFileItem = new UploadedFileItem();
        DataHandler param = new DataHandler(carFile.toURI().toURL());
        uploadedFileItem.setDataHandler(param);
        uploadedFileItem.setFileName(carFile.getName());
        uploadedFileItem.setFileType("jar");
        UploadedFileItem[] fileItems = new UploadedFileItem[]{uploadedFileItem};
        logger.info("Uploading " + carFile.getName() + " to " + serverUrl + "...");
        carbonAppUploaderStub.uploadApp(fileItems);
    }

    @Override
    public void unDeployCApp(String username, String password, String serverUrl, MavenProject project)
            throws Exception {

        ApplicationAdminStub appAdminStub = getApplicationAdminStub(serverUrl, username, password);
        String[] existingApplications = appAdminStub.listAllApplications();
        if (existingApplications != null && Arrays
                .asList(existingApplications).contains(project.getArtifactId() + "_" + project.getVersion())) {
            appAdminStub.deleteApplication(project.getArtifactId() + "_" + project.getVersion());
            logger.info(
                    "Located the C-App " + project.getArtifactId() + "_" + project.getVersion() + " and undeployed...");
        }
    }

    private CarbonAppUploaderStub getCarbonAppUploaderStub(String username,
                                                           String pwd, String url) throws Exception {

        CarbonAppUploaderStub carbonAppUploaderStub =
                new CarbonAppUploaderStub(url + "/services/CarbonAppUploader");
        Header header = createBasicAuthHeader(username, pwd);
        List<Header> list = new ArrayList();
        list.add(header);
        carbonAppUploaderStub._getServiceClient().getOptions()
                .setProperty(org.apache.axis2.transport.http.HTTPConstants.HTTP_HEADERS, list);
        return carbonAppUploaderStub;
    }

    private ApplicationAdminStub getApplicationAdminStub(String serverURL, String username, String pwd)
            throws Exception {

        ApplicationAdminStub appAdminStub = new ApplicationAdminStub(serverURL + "/services/ApplicationAdmin");
        Header header = createBasicAuthHeader(username, pwd);
        List<Header> list = new ArrayList();
        list.add(header);
        appAdminStub._getServiceClient().getOptions()
                .setProperty(org.apache.axis2.transport.http.HTTPConstants.HTTP_HEADERS, list);
        return appAdminStub;
    }

    private Header createBasicAuthHeader(String userName, String password) {

        return new Header(Constants.AUTHORIZATION_HEADER,
                Constants.BASIC + Base64Utils.encode((userName + ":" + password).getBytes()));
    }
}
