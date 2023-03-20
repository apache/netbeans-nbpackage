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
package org.apache.netbeans.nbpackage.zip;

import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.netbeans.nbpackage.AbstractPackagerTask;
import org.apache.netbeans.nbpackage.ExecutionContext;
import org.apache.netbeans.nbpackage.FileUtils;

class ZipPackageTask extends AbstractPackagerTask {

    ZipPackageTask(ExecutionContext context) {
        super(context);
    }

    @Override
    protected void customizeImage(Path image) throws Exception {
        // no op
    }

    @Override
    protected Path buildPackage(Path image) throws Exception {
        Path dst = context().destination().resolve(image.getFileName().toString() + ".zip");
        FileUtils.createZipArchive(image, dst);
        return dst;
    }

    @Override
    protected Path runtimeDirectory(Path image, Path application) throws Exception {
        if (Files.exists(application.resolve("bin").resolve("netbeans"))) {
            context().warningHandler().accept(ZipPackager.MESSAGES.getString("zip.nbruntime.warning"));
        }
        return super.runtimeDirectory(image, application);
    }

}
