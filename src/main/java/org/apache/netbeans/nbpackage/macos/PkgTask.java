/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.netbeans.nbpackage.macos;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.netbeans.nbpackage.ExecutionContext;
import org.apache.netbeans.nbpackage.NBPackage;

/**
 *
 */
class PkgTask extends AppBundleTask {

    PkgTask(ExecutionContext context) {
        super(context);
    }
    
    @Override
    protected void checkPackageRequirements() throws Exception {
        super.checkPackageRequirements();
        validateTools("pkgbuild");
    }

    @Override
    protected Path buildPackage(Path image) throws Exception {
        Path bundle = super.buildPackage(image);
        String name = context().getValue(NBPackage.PACKAGE_NAME).orElseThrow();
        String version = context().getValue(NBPackage.PACKAGE_VERSION).orElseThrow();
        Path output = context().destination().resolve(
                sanitize(name) + " " + sanitize(version) + ".pkg");
        String signingID = context().getValue(MacOS.PKGBUILD_ID).orElse("");
        List<String> command = new ArrayList<>();
        command.add("pkgbuild");
        command.add("--component");
        command.add(bundle.toString());
        command.add("--version");
        command.add(version);
        command.add("--install-location");
        command.add("/Applications");
        
        if (signingID.isBlank()) {
            context().warningHandler().accept(
                    MacOS.MESSAGES.getString("message.nopkgbuildid"));
        } else {
            command.add("--sign");
            command.add(signingID);
        }
        
        command.add(output.toString());
        int result = context().exec(command);
        if (result != 0) {
            throw new Exception();
        } else {
            return output;
        }
    }

    @Override
    protected String calculateImageName(Path input) throws Exception {
        return super.calculateImageName(input) + "-pkg";
    }

}
