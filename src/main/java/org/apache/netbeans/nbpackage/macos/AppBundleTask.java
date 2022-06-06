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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.netbeans.nbpackage.AbstractPackagerTask;
import org.apache.netbeans.nbpackage.ExecutionContext;
import org.apache.netbeans.nbpackage.FileUtils;
import org.apache.netbeans.nbpackage.NBPackage;
import org.apache.netbeans.nbpackage.StringUtils;

/**
 *
 */
class AppBundleTask extends AbstractPackagerTask {
    
    static final String DEFAULT_BIN_GLOB = "{*.dylib,*.jnilib,**/nativeexecution/MacOSX-*/*,Contents/Home/bin/*,Contents/Home/lib/jspawnhelper}";
    static final String DEFAULT_JAR_BIN_GLOB = "{jna-5*.jar,junixsocket-native-common-*.jar,launcher-common-*.jar,jansi-*.jar,nbi-engine.jar}";
    static final String DEFAULT_JAR_INTERNAL_BIN_GLOB = "**/*.{dylib,jnilib}";
    
    private static final String NATIVE_BIN_FILENAME = "nativeBinaries";
    private static final String JAR_BIN_FILENAME = "jarBinaries";
    private static final String ENTITLEMENTS_FILENAME = "sandbox.plist";
    
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
        setupSigningConfiguration(image, bundle);
        
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
        Path icnsFile = context().getValue(MacOS.ICON_PATH).orElse(null);
        Path dstFile = resources.resolve(execName + ".icns");
        if (icnsFile != null) {
            Files.copy(icnsFile, dstFile);
        } else {
            Files.copy(getClass().getResourceAsStream(
                    "/org/apache/netbeans/nbpackage/apache-netbeans.icns"), dstFile);
        }
    }
    
    private void setupInfo(Path contents, String execName) throws IOException {
        String template = MacOS.INFO_TEMPLATE.load(context());
        
        var tokenMap = Map.of(
                "BUNDLE_NAME", getBundleName(),
                "BUNDLE_DISPLAY", context().getValue(NBPackage.PACKAGE_NAME).orElseThrow(),
                "BUNDLE_VERSION", context().getValue(NBPackage.PACKAGE_VERSION).orElseThrow(),
                "BUNDLE_EXEC", execName,
                "BUNDLE_ID", context().getValue(MacOS.BUNDLE_ID)
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
        
        String packageSwift = MacOS.LAUNCHER_PACKAGE_TEMPLATE.load(context());
        String mainSwift = MacOS.LAUNCHER_TEMPLATE.load(context());
        
        Files.writeString(launcherProject.resolve("Package.swift"),
                packageSwift, StandardOpenOption.CREATE_NEW);
        Files.writeString(sourceDir.resolve("main.swift"),
                mainSwift, StandardOpenOption.CREATE_NEW);
    }
    
    private void setupSigningConfiguration(Path image, Path bundle) throws IOException {
        Files.writeString(image.resolve(ENTITLEMENTS_FILENAME),
                MacOS.ENTITLEMENTS_TEMPLATE.load(context())
                , StandardOpenOption.CREATE_NEW);
        var nativeBinaries = FileUtils.find(bundle, DEFAULT_BIN_GLOB);
        Files.writeString(image.resolve(NATIVE_BIN_FILENAME),
                nativeBinaries.stream()
                        .map(path -> image.relativize(path))
                        .map(Path::toString)
                        .collect(Collectors.joining("\n", "", "\n")),
                StandardOpenOption.CREATE_NEW);
        var jarBinaries = FileUtils.find(bundle, DEFAULT_JAR_BIN_GLOB);
        Files.writeString(image.resolve(JAR_BIN_FILENAME),
                jarBinaries.stream()
                        .map(path -> image.relativize(path))
                        .map(Path::toString)
                        .collect(Collectors.joining("\n", "", "\n")),
                StandardOpenOption.CREATE_NEW);
    }
    
}
