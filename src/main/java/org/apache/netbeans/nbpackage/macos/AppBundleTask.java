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
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.netbeans.nbpackage.AbstractPackagerTask;
import org.apache.netbeans.nbpackage.Architecture;
import org.apache.netbeans.nbpackage.ExecutionContext;
import org.apache.netbeans.nbpackage.FileUtils;
import org.apache.netbeans.nbpackage.NBPackage;
import org.apache.netbeans.nbpackage.StringUtils;

/**
 *
 */
class AppBundleTask extends AbstractPackagerTask {

    static final String ARCH_X86_64 = "x86_64";
    static final String ARCH_ARM64 = "arm64";
    static final String ARCH_UNIVERSAL = "universal";

    private static final String DEFAULT_JAR_INTERNAL_BIN_GLOB = "**/*.{dylib,jnilib}";
    private static final String NATIVE_BIN_FILENAME = "nativeBinaries";
    private static final String JAR_BIN_FILENAME = "jarBinaries";
    private static final String ENTITLEMENTS_FILENAME = "sandbox.plist";
    private static final String LAUNCHER_SRC_DIRNAME = "macos-launcher-src";

    private String bundleName;
    private String bundleArch;

    AppBundleTask(ExecutionContext context) {
        super(context);
    }

    @Override
    protected void checkPackageRequirements() throws Exception {
        String[] cmds;
        if (context().getValue(MacOS.CODESIGN_ID).isEmpty()) {
            cmds = new String[]{"swift"};
        } else {
            cmds = new String[]{"swift", "codesign"};
        }
        validateTools(cmds);
    }

    @Override
    protected void customizeImage(Path image) throws Exception {
        Path bundle = image.resolve(bundleName() + ".app");
        Path contents = bundle.resolve("Contents");
        Path resources = contents.resolve("Resources");

        String execName = findLauncher(resources.resolve("APPDIR").resolve("bin"))
                .getFileName().toString();
        Files.move(resources.resolve("APPDIR"), resources.resolve(execName));

        Files.createDirectory(contents.resolve("MacOS"));
        setupIcons(resources, execName);
        setupInfo(contents, execName);
        setupLauncherSource(image);
    }

    @Override
    protected void finalizeImage(Path image) throws Exception {
        Path bundle = image.resolve(bundleName() + ".app");
        setupSigningConfiguration(image, bundle);
    }

    @Override
    protected Path buildPackage(Path image) throws Exception {
        Path bundle = image.resolve(bundleName() + ".app");

        String execName = FileUtils.find(bundle, "Contents/Resources/*/bin/*")
                .stream()
                .filter(path -> !path.toString().endsWith(".exe"))
                .findFirst()
                .map(path -> path.getFileName().toString())
                .orElseThrow();
        String arch = bundleArch();
        Path launcher = compileLauncher(image.resolve(LAUNCHER_SRC_DIRNAME), arch);
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
        String arch = bundleArch();
        if (ARCH_UNIVERSAL.equals(arch)) {
            return super.calculateImageName(input) + "-macOS-app";
        } else {
            return super.calculateImageName(input) + "-macOS-" + arch + "-app";
        }
    }

    @Override
    protected Path calculateAppPath(Path image) throws Exception {
        return image.resolve(bundleName() + ".app")
                .resolve("Contents")
                .resolve("Resources")
                .resolve("APPDIR");
    }

    @Override
    protected Path calculateRuntimePath(Path image, Path application) throws Exception {
        return image.resolve(bundleName() + ".app")
                .resolve("Contents")
                .resolve("Home");
    }

    @Override
    protected Path calculateRootPath(Path image) throws Exception {
        return image.resolve(bundleName() + ".app");
    }

    String bundleName() {
        if (bundleName == null) {
            String name = sanitize(context().getValue(NBPackage.PACKAGE_NAME).orElseThrow());
            if (name.length() > 15) {
                name = name.substring(0, 16);
            }
            bundleName = name;
        }
        return bundleName;
    }

    @SuppressWarnings("removal")
    String bundleArch() {
        if (bundleArch == null) {
            bundleArch = context().getValue(NBPackage.PACKAGE_ARCH)
                    .map(arch -> {
                        if (Architecture.X86_64.isSynonym(arch)) {
                            return ARCH_X86_64;
                        } else if (Architecture.AARCH64.isSynonym(arch)) {
                            return ARCH_ARM64;
                        } else {
                            if (!arch.equalsIgnoreCase(ARCH_UNIVERSAL)) {
                                context().warningHandler().accept(
                                        MacOS.MESSAGES.getString("message.unknownarch"));
                            }
                            return ARCH_UNIVERSAL;
                        }
                    })
                    .orElseGet(() -> {
                        Optional<Path> runtime = context().getValue(NBPackage.PACKAGE_RUNTIME);
                        if (runtime.isPresent()) {
                            return Architecture.detectFromPath(
                                    runtime.get()).map(a -> {
                                return switch (a) {
                                    case AARCH64 ->
                                        ARCH_ARM64;
                                    case X86_64 ->
                                        ARCH_X86_64;
                                };
                            }).orElseGet(() -> {
                                context().warningHandler().accept(
                                        MacOS.MESSAGES.getString("message.unknownarch"));
                                return ARCH_UNIVERSAL;
                            });
                        } else {
                            return ARCH_UNIVERSAL;
                        }
                    });
        }
        return bundleArch;
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
        try (var files = Files.list(binDir)) {
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

        Map<String, String> tokenMap = Map.of(
                "BUNDLE_NAME", bundleName(),
                "BUNDLE_DISPLAY", context().getValue(NBPackage.PACKAGE_NAME).orElseThrow(),
                "BUNDLE_VERSION", context().getValue(NBPackage.PACKAGE_VERSION).orElseThrow(),
                "BUNDLE_EXEC", execName,
                "BUNDLE_ID", context().getValue(MacOS.BUNDLE_ID)
                        .orElse(sanitizeBundleID(bundleName())),
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
                MacOS.ENTITLEMENTS_TEMPLATE.load(context()),
                StandardOpenOption.CREATE_NEW);
        List<Path> nativeBinaries = FileUtils.find(bundle,
                context().getValue(MacOS.SIGNING_FILES).orElseThrow());
        Files.writeString(image.resolve(NATIVE_BIN_FILENAME),
                nativeBinaries.stream()
                        .map(path -> image.relativize(path))
                        .map(Path::toString)
                        .collect(Collectors.joining("\n", "", "\n")),
                StandardOpenOption.CREATE_NEW);
        List<Path> jarBinaries = FileUtils.find(bundle,
                context().getValue(MacOS.SIGNING_JARS).orElseThrow());
        Files.writeString(image.resolve(JAR_BIN_FILENAME),
                jarBinaries.stream()
                        .map(path -> image.relativize(path))
                        .map(Path::toString)
                        .collect(Collectors.joining("\n", "", "\n")),
                StandardOpenOption.CREATE_NEW);
    }

    private Path compileLauncher(Path launcherProject, String arch) throws IOException, InterruptedException {
        final ProcessBuilder pb;
        pb = switch (arch) {
            case ARCH_X86_64 ->
                new ProcessBuilder("swift", "build",
                "--configuration", "release",
                "--arch", "x86_64");
            case ARCH_ARM64 ->
                new ProcessBuilder("swift", "build",
                "--configuration", "release",
                "--arch", "arm64");
            default ->
                new ProcessBuilder("swift", "build",
                "--configuration", "release",
                "--arch", "arm64",
                "--arch", "x86_64");
        };
        pb.directory(launcherProject.toFile());
        context().exec(pb);
        var output = FileUtils.find(launcherProject.resolve(".build"), "**/{R,r}elease/AppLauncher");
        if (output.isEmpty()) {
            throw new IOException(launcherProject.toString());
        }
        return output.get(0);
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
                .toList();
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
            throws IOException {
        Path nativeFiles = image.resolve(NATIVE_BIN_FILENAME);
        if (!Files.exists(nativeFiles)) {
            return;
        }
        List<Path> files = Files.readString(nativeFiles).lines()
                .filter(l -> !l.isBlank())
                .map(Path::of)
                .map(image::resolve)
                .toList();
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
