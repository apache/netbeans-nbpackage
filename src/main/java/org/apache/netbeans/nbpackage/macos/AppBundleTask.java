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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.netbeans.nbpackage.AbstractPackagerTask;
import org.apache.netbeans.nbpackage.ExecutionContext;
import org.apache.netbeans.nbpackage.NBPackage;
import org.apache.netbeans.nbpackage.StringUtils;

import static org.apache.netbeans.nbpackage.macos.PkgPackager.*;

/**
 *
 */
class AppBundleTask extends AbstractPackagerTask {

    private String bundleName;

    AppBundleTask(ExecutionContext context) {
        super(context);
    }

    @Override
    public void validateCreatePackage() throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Path createImage(Path input) throws Exception {
        Path image = super.createImage(input);
        Path bundle = image.resolve(getBundleName() + ".app");
        Path contents = bundle.resolve("Contents");
        Path resources = contents.resolve("Resources");

        String execName = findLauncher(
                resources
                        .resolve("APPDIR")
                        .resolve("bin"))
                .getFileName().toString();
        Files.move(resources.resolve("APPDIR"), resources.resolve(execName));

        Files.createDirectory(contents.resolve("MacOS"));
        setupIcons(resources, execName);
        setupInfo(contents, execName);
        setupLauncherSource(image);

        return image;

    }

    @Override
    public Path createPackage(Path image) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected String imageName(Path input) throws Exception {
        return super.imageName(input) + "-macOS-app";
    }

    @Override
    protected Path applicationDirectory(Path image) throws Exception {
        return image.resolve(getBundleName() + ".app")
                .resolve("Contents")
                .resolve("Resources")
                .resolve("APPDIR");
    }

    @Override
    protected Path runtimeDirectory(Path image, Path application) throws Exception {
        return image.resolve(getBundleName() + ".app")
                .resolve("Contents")
                .resolve("Home");
    }

    String getBundleName() {
        if (bundleName == null) {
            var name = sanitize(context().getValue(NBPackage.PACKAGE_NAME).orElseThrow());
            if (name.length() > 15) {
                name = name.substring(0, 16);
            }
            bundleName = name;
        }
        return bundleName;
    }

    private String sanitize(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String sanitizeBundleID(String name) {
        return name.replaceAll("[^a-zA-Z0-9-\\.]", "-");
    }

    private Path findLauncher(Path binDir) throws IOException {
        try ( var files = Files.list(binDir)) {
            return files.filter(f -> !f.getFileName().toString().endsWith(".exe"))
                    .findFirst().orElseThrow(IOException::new);
        }
    }

    private void setupIcons(Path resources, String execName) throws IOException {
        Path icnsFile = context().getValue(MACOS_ICON).orElse(null);
        Path dstFile = resources.resolve(execName + ".icns");
        if (icnsFile != null) {
            Files.copy(icnsFile, dstFile);
        } else {
            Files.copy(getClass().getResourceAsStream(
                    "/org/apache/netbeans/nbpackage/apache-netbeans.icns"), dstFile);
        }
    }

    private void setupInfo(Path contents, String execName) throws IOException {
        Path templateFile = context().getValue(MACOS_INFO_TEMPLATE).orElse(null);
        String template;
        try ( var reader = templateFile != null
                ? Files.newBufferedReader(templateFile)
                : new BufferedReader(
                        new InputStreamReader(
                                getClass().getResourceAsStream("Info.plist.template"),
                                StandardCharsets.UTF_8))) {
            template = reader.lines().collect(Collectors.joining("\n", "", "\n"));
        }

        var tokenMap = Map.of(
                "BUNDLE_NAME", getBundleName(),
                "BUNDLE_DISPLAY", context().getValue(NBPackage.PACKAGE_NAME).orElseThrow(),
                "BUNDLE_VERSION", context().getValue(NBPackage.PACKAGE_VERSION).orElseThrow(),
                "BUNDLE_EXEC", execName,
                "BUNDLE_ID", context().getValue(MACOS_BUNDLE_ID)
                        .orElse(sanitizeBundleID(getBundleName())),
                "BUNDLE_ICON", execName + ".icns"
        );

        String info = StringUtils.replaceTokens(template, tokenMap);

        Files.writeString(contents.resolve("Info.plist"), info,
                StandardOpenOption.CREATE_NEW);

    }

    private void setupLauncherSource(Path image) throws IOException {
        Path launcherProject = image.resolve("macos-launcher-src");
        Files.createDirectories(launcherProject);
        Path sourceDir = launcherProject.resolve("Sources").resolve("AppLauncher");
        Files.createDirectories(sourceDir);

        String packageSwift;
        try ( var reader = new BufferedReader(
                new InputStreamReader(
                        getClass().getResourceAsStream("Package.swift.template"),
                        StandardCharsets.UTF_8
                ))) {
            packageSwift = reader.lines().collect(Collectors.joining("\n", "", "\n"));
        }

        String mainSwift;
        try ( var reader = new BufferedReader(
                new InputStreamReader(
                        getClass().getResourceAsStream("main.swift.template"),
                        StandardCharsets.UTF_8
                ))) {
            mainSwift = reader.lines().collect(Collectors.joining("\n", "", "\n"));
        }

        Files.writeString(launcherProject.resolve("Package.swift"),
                packageSwift, StandardOpenOption.CREATE_NEW);
        Files.writeString(sourceDir.resolve("main.swift"),
                mainSwift, StandardOpenOption.CREATE_NEW);
    }

}
