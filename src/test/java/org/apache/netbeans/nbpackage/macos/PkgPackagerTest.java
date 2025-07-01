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
 * Tests for PkgPackager.
 */
public class PkgPackagerTest {

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
        Path image = buildImage(new PkgPackager(), input, config, tmpDir);
        Path app = resolve(image, "App.app");
        assertTrue(Files.isDirectory(app));
        assertTrue(Files.isDirectory(resolve(image, "macos-launcher-src")));
        assertTrue(Files.exists(resolve(image, "jarBinaries")));
        assertTrue(Files.exists(resolve(image, "nativeBinaries")));
        assertTrue(Files.exists(resolve(image, "sandbox.plist")));
        assertTrue(Files.exists(resolve(app, "Contents", "Info.plist")));
        assertFalse(Files.exists(resolve(app, "Contents", "Home")));
        assertTrue(Files.exists(resolve(app, "Contents", "Resources", "app.icns")));
        assertTrue(Files.exists(resolve(app, "Contents", "Resources", "app", "bin", "app")));
    }

    @Test
    public void testImageWithX86_64Runtime() throws Exception {
        Path input = tmpDir.resolve("App-1.0-b1.zip");
        FileUtils.createZipArchive(
                buildFakeApp(tmpDir, "App-1.0-b1", "app"),
                input);
        String runtimeName = "OpenJDK24U-jdk_aarch64_mac_hotspot_24.0.1_9";
        Path runtime = tmpDir.resolve(runtimeName + ".zip");
        FileUtils.createZipArchive(
                buildFakeJDK(tmpDir, runtimeName, false),
                runtime);
        Configuration config = Configuration.builder()
                .set(NBPackage.PACKAGE_NAME, "App")
                .set(NBPackage.PACKAGE_VERSION, "1.0-b1")
                .set(NBPackage.PACKAGE_RUNTIME, runtime.toString())
                .build();
        Path image = buildImage(new PkgPackager(), input, config, tmpDir);
        assertTrue(image.getFileName().toString().contains("arm64"));
        Path app = resolve(image, "App.app");
        assertTrue(Files.isDirectory(app));
        assertTrue(Files.isDirectory(resolve(image, "macos-launcher-src")));
        assertTrue(Files.exists(resolve(image, "jarBinaries")));
        assertTrue(Files.exists(resolve(image, "nativeBinaries")));
        assertTrue(Files.exists(resolve(image, "sandbox.plist")));
        assertTrue(Files.exists(resolve(app, "Contents", "Info.plist")));
        assertTrue(Files.exists(resolve(app, "Contents", "Home", "bin", "java")));
        assertTrue(Files.exists(resolve(app, "Contents", "Resources", "app.icns")));
        assertTrue(Files.exists(resolve(app, "Contents", "Resources", "app", "bin", "app")));
    }

}
