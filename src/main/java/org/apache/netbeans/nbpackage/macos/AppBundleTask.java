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
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
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
    
    private static final String DEFAULT_JAR_INTERNAL_BIN_GLOB = "**/*.{dylib,jnilib}";
    private static final String NATIVE_BIN_FILENAME = "nativeBinaries";
    private static final String JAR_BIN_FILENAME = "jarBinaries";
    private static final String ENTITLEMENTS_FILENAME = "sandbox.plist";
    private static final String LAUNCHER_SRC_DIRNAME = "macos-launcher-src";
    
    private String bundleName;
    
    AppBundleTask(ExecutionContext context) {
        super(context);
    }
    
    @Override
    protected void checkPackageRequirements() throws Exception {
        String[] cmds;
        if (context().getValue(MacOS.CODESIGN_ID).isEmpty()) {
            cmds = new String[] {"swift"};
        } else {
            cmds = new String[] {"swift", "codesign"};
        }
        validateTools(cmds);
    }
    
    @Override
    protected void customizeImage(Path image) throws Exception {
        Path bundle = image.resolve(getBundleName() + ".app");
        Path contents = bundle.resolve("Contents");
        Path resources = contents.resolve("Resources");
        
        String execName = findLauncher(resources.resolve("APPDIR").resolve("bin"))
                .getFileName().toString();
        Files.move(resources.resolve("APPDIR"), resources.resolve(execName));
        
        Files.createDirectory(contents.resolve("MacOS"));
        setupIcons(resources, execName);
        setupInfo(contents, execName);
        setupLauncherSource(image);
        setupSigningConfiguration(image, bundle);
        
    }
    
    @Override
    protected Path buildPackage(Path image) throws Exception {
        Path bundle = image.resolve(getBundleName() + ".app");
        
        String execName = FileUtils.find(bundle, "Contents/Resources/*/bin/*")
                .stream()
                .filter(path -> !path.toString().endsWith(".exe"))
                .findFirst()
                .map(path -> path.getFileName().toString())
                .orElseThrow();
        Path launcher = compileLauncher(image.resolve(LAUNCHER_SRC_DIRNAME));
        Files.copy(launcher, bundle.resolve("Contents")
                .resolve("MacOS").resolve(execName),
                StandardCopyOption.COPY_ATTRIBUTES);
        
        String signID = context().getValue(MacOS.CODESIGN_ID).orElse("");
        if (signID.isBlank()) {
            context().warningHandler().accept(
                    MacOS.MESSAGES.getString("message.nocodesignid"));
            return bundle;
        }
        Path entitlements = image.resolve(ENTITLEMENTS_FILENAME);
        signBinariesInJARs(image, entitlements, signID);
        signNativeBinaries(image, entitlements, signID);
        codesign(bundle, entitlements, signID);
        return bundle;
    }
    
    @Override
    protected String calculateImageName(Path input) throws Exception {
        return super.calculateImageName(input) + "-macOS-app";
    }
    
    @Override
    protected Path calculateAppPath(Path image) throws Exception {
        return image.resolve(getBundleName() + ".app")
                .resolve("Contents")
                .resolve("Resources")
                .resolve("APPDIR");
    }
    
    @Override
    protected Path calculateRuntimePath(Path image, Path application) throws Exception {
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
    
    void validateTools(String... tools) throws Exception {
        if (context().isVerbose()) {
            context().infoHandler().accept(MessageFormat.format(
                    MacOS.MESSAGES.getString("message.validatingtools"),
                    Arrays.toString(tools)));
        }
        for (String tool : tools) {
            if (context().exec(List.of("which", tool)) != 0) {
                throw new IllegalStateException(MessageFormat.format(
                        MacOS.MESSAGES.getString("message.missingtool"),
                        tool));
            }
        }
    }
    
    String sanitize(String name) {
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
        Path launcherProject = image.resolve(LAUNCHER_SRC_DIRNAME);
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
        var nativeBinaries = FileUtils.find(bundle,
                context().getValue(MacOS.SIGNING_FILES).orElseThrow());
        Files.writeString(image.resolve(NATIVE_BIN_FILENAME),
                nativeBinaries.stream()
                        .map(path -> image.relativize(path))
                        .map(Path::toString)
                        .collect(Collectors.joining("\n", "", "\n")),
                StandardOpenOption.CREATE_NEW);
        var jarBinaries = FileUtils.find(bundle,
                context().getValue(MacOS.SIGNING_JARS).orElseThrow());
        Files.writeString(image.resolve(JAR_BIN_FILENAME),
                jarBinaries.stream()
                        .map(path -> image.relativize(path))
                        .map(Path::toString)
                        .collect(Collectors.joining("\n", "", "\n")),
                StandardOpenOption.CREATE_NEW);
    }
    
    private Path compileLauncher(Path launcherProject) throws IOException, InterruptedException {
        var pb = new ProcessBuilder("swift", "build",
                "--configuration", "release",
                "--arch", "x86_64");
        pb.directory(launcherProject.toFile());
        context().exec(pb);
        Path launcher = launcherProject.resolve(".build/release/AppLauncher");
        if (!Files.exists(launcher)) {
            throw new IOException(launcher.toString());
        }
        return launcher;
    }
    
    private void signBinariesInJARs(Path image, Path entitlements, String id)
            throws IOException {
        Path jarFiles = image.resolve(JAR_BIN_FILENAME);
        if (!Files.exists(jarFiles)) {
            return;
        }
        List<Path> jars = Files.readString(jarFiles).lines()
                .filter(l -> !l.isBlank())
                .map(Path::of)
                .map(image::resolve)
                .collect(Collectors.toList());
        for (Path jar : jars) {
            FileUtils.processJarContents(jar,
                    DEFAULT_JAR_INTERNAL_BIN_GLOB,
                    (file, path) -> {
                        codesign(file, entitlements, id);
                        return true;
                    }
            );
        }
    }
    
    private void signNativeBinaries(Path image, Path entitlements, String id)
            throws IOException  {
        Path nativeFiles = image.resolve(NATIVE_BIN_FILENAME);
        if (!Files.exists(nativeFiles)) {
            return;
        }
        List<Path> files = Files.readString(nativeFiles).lines()
                .filter(l -> !l.isBlank())
                .map(Path::of)
                .map(image::resolve)
                .collect(Collectors.toList());
        for (Path file : files) {
            codesign(file, entitlements, id);
        }
    }

    private void codesign(Path file, Path entitlements, String id)
            throws IOException {
        try {
            context().exec("codesign",
                    "--force",
                    "--timestamp",
                    "--options=runtime",
                    "--entitlements", entitlements.toString(),
                    "-s", id,
                    "-v",
                    file.toString());
        } catch (InterruptedException ex) {
            throw new IOException(ex);
        }
    }
    
}
