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
package org.apache.netbeans.nbpackage.deb;

import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.netbeans.nbpackage.Configuration;
import org.apache.netbeans.nbpackage.FileUtils;
import org.apache.netbeans.nbpackage.NBPackage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import static org.apache.netbeans.nbpackage.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DebPackager.
 */
public class DebPackagerTest {
    
    private @TempDir(cleanup = CleanupMode.ON_SUCCESS) Path tmpDir;
    
    @Test
    public void testImageWithoutRuntime() throws Exception {
        Path input = tmpDir.resolve("App-1.0-b1.zip");
        FileUtils.createZipArchive(
                buildFakeApp(tmpDir, "App-1.0-b1", "app"),
                input);
        Configuration config = Configuration.builder()
                .set(NBPackage.PACKAGE_NAME, "App")
                .set(NBPackage.PACKAGE_VERSION, "1.0-b1")
                .build();
        Path image = buildImage(new DebPackager(), input, config, tmpDir);
        assertTrue(image.getFileName().toString().endsWith("_all"));
        String control = Files.readString(resolve(image, "DEBIAN", "control"));
        assertTrue(control.contains("Package: app"));
        assertTrue(control.contains("Recommends: java17-sdk"));
        assertTrue(Files.exists(resolve(image, "usr", "bin", "app")));
        assertTrue(Files.exists(resolve(image, "usr", "share", "applications", "app.desktop")));
        assertTrue(Files.exists(resolve(image, "usr", "share", "icons", "hicolor", "48x48", "apps", "app.png")));
        assertTrue(Files.exists(resolve(image, "usr", "share", "icons", "hicolor", "scalable", "apps", "app.svg")));
    }
    
    @Test
    public void testImageWithX86_64Runtime() throws Exception {
        Path input = tmpDir.resolve("App-1.0-b1.zip");
        FileUtils.createZipArchive(
                buildFakeApp(tmpDir, "App-1.0-b1", "app"),
                input);
        String runtimeName = "OpenJDK24U-jdk_x64_linux_hotspot_24.0.1_9";
        Path runtime = tmpDir.resolve(runtimeName + ".zip");
        FileUtils.createZipArchive(
                buildFakeJDK(tmpDir, runtimeName, false),
                runtime);
        Configuration config = Configuration.builder()
                .set(NBPackage.PACKAGE_NAME, "App")
                .set(NBPackage.PACKAGE_VERSION, "1.0-b1")
                .set(NBPackage.PACKAGE_RUNTIME, runtime.toString())
                .build();
        Path image = buildImage(new DebPackager(), input, config, tmpDir);
        assertTrue(image.getFileName().toString().endsWith("_amd64"));
        String control = Files.readString(resolve(image, "DEBIAN", "control"));
        assertTrue(control.contains("Package: app"));
        assertFalse(control.contains("Recommends: java"));
        assertTrue(Files.exists(resolve(image, "usr", "bin", "app")));
        assertTrue(Files.exists(resolve(image, "usr", "lib", "app", "jdk", "bin", "java")));
        assertTrue(Files.exists(resolve(image, "usr", "share", "applications", "app.desktop")));
        assertTrue(Files.exists(resolve(image, "usr", "share", "icons", "hicolor", "48x48", "apps", "app.png")));
        assertTrue(Files.exists(resolve(image, "usr", "share", "icons", "hicolor", "scalable", "apps", "app.svg")));
    }

}
