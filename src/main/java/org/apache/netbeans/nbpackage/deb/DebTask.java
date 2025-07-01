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

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.apache.netbeans.nbpackage.AbstractPackagerTask;
import org.apache.netbeans.nbpackage.Architecture;
import org.apache.netbeans.nbpackage.ExecutionContext;
import org.apache.netbeans.nbpackage.NBPackage;
import org.apache.netbeans.nbpackage.StringUtils;

class DebTask extends AbstractPackagerTask {

    private static final String DPKG = "dpkg";
    private static final String DPKG_DEB = "dpkg-deb";
    private static final String FAKEROOT = "fakeroot";
    private static final String ARCH_ALL = "all";
    private static final String ARCH_AMD64 = "amd64";
    private static final String ARCH_ARM64 = "arm64";

    private String packageName;
    private String packageVersion;
    private String packageArch;

    DebTask(ExecutionContext context) {
        super(context);
    }

    @Override
    protected void checkPackageRequirements() throws Exception {
        validateTools(DPKG, DPKG_DEB, FAKEROOT);
    }

    @Override
    protected void customizeImage(Path image) throws Exception {
        String pkgName = packageName();

        // @TODO support other installation bases
        String base = "usr";
        Path baseDir = image.resolve(base);
        Path appDir = baseDir.resolve("lib").resolve(pkgName);
        Files.move(baseDir.resolve("lib").resolve("APPDIR"),
                appDir);

        String execName = findLauncher(appDir.resolve("bin")).getFileName().toString();
        String packageLocation = "/" + base + "/lib/" + pkgName;
        Path binDir = baseDir.resolve("bin");
        Files.createDirectories(binDir);
        setupLauncher(binDir, packageLocation, execName);

        Path share = baseDir.resolve("share");
        Files.createDirectories(share);
        setupIcons(share, pkgName);
        setupDesktopFile(share, "/" + base + "/bin/" + execName, pkgName);
        // @TODO setup lintian override

        Path DEBIAN = image.resolve("DEBIAN");
        Files.createDirectories(DEBIAN);
        setupControlFile(DEBIAN);

    }

    @Override
    protected Path buildPackage(Path image) throws Exception {
        String targetName = image.getFileName().toString() + ".deb";
        Path target = context().destination().resolve(targetName).toAbsolutePath();
        if (Files.exists(target)) {
            throw new FileAlreadyExistsException(target.toString());
        }
        int result = context().exec(FAKEROOT, DPKG_DEB, "--build",
                image.toAbsolutePath().toString(),
                target.toString());
        if (result != 0) {
            throw new Exception();
        } else {
            return target;
        }
    }

    @Override
    protected String calculateImageName(Path input) throws Exception {
        return packageName() + "_" + packageVersion() + "_" + packageArch();
    }

    @Override
    protected Path calculateAppPath(Path image) throws Exception {
        return image.resolve("usr").resolve("lib").resolve("APPDIR");
    }

    private void validateTools(String... tools) throws Exception {
        if (context().isVerbose()) {
            context().infoHandler().accept(MessageFormat.format(
                    DebPackager.MESSAGES.getString("message.validatingtools"),
                    Arrays.toString(tools)));
        }
        for (String tool : tools) {
            if (context().exec(List.of("which", tool)) != 0) {
                throw new IllegalStateException(
                        DebPackager.MESSAGES.getString("message.missingdebtools"));
            }
        }
    }

    private String packageName() {
        if (packageName == null) {
            var name = sanitize(context().getValue(NBPackage.PACKAGE_NAME).orElseThrow());
            if (name.length() < 2 || !Character.isLetter(name.charAt(0))) {
                throw new IllegalArgumentException();
            }
            packageName = name;
        }
        return packageName;
    }

    private String packageVersion() {
        if (packageVersion == null) {
            var version = sanitizeVersion(context().getValue(NBPackage.PACKAGE_VERSION)
                    .orElse("1.0"));
            packageVersion = version + "-1";
        }
        return packageVersion;
    }

    private String packageArch() {
        if (packageArch == null) {
            packageArch = context().getValue(NBPackage.PACKAGE_ARCH)
                    .orElseGet(() -> {
                        Optional<Path> runtime = context().getValue(NBPackage.PACKAGE_RUNTIME);
                        if (runtime.isPresent()) {
                            return Architecture.detectFromPath(
                                    runtime.get()).map(a -> {
                                return switch (a) {
                                    case AARCH64 ->
                                        ARCH_ARM64;
                                    case X86_64 ->
                                        ARCH_AMD64;
                                };
                            }).orElseGet(() -> {
                                context().warningHandler().accept(
                                        DebPackager.MESSAGES.getString("message.unknownarch"));
                                return ARCH_ALL;
                            });
                        } else {
                            return ARCH_ALL;
                        }
                    });
        }
        return packageArch;
    }

    private String sanitize(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\+\\-\\.]", "-");
    }

    private String sanitizeVersion(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\+\\.\\~]", "~");
    }

    private Path findLauncher(Path binDir) throws IOException {
        try ( var files = Files.list(binDir)) {
            return files.filter(f -> !f.getFileName().toString().endsWith(".exe"))
                    .findFirst().orElseThrow(IOException::new);
        }
    }

    private void setupLauncher(Path binDir, String packageLocation, String execName)
            throws IOException {
        String template = DebPackager.LAUNCHER_TEMPLATE.load(context());
        String script = StringUtils.replaceTokens(template,
                Map.of("PACKAGE", packageLocation, "EXEC", execName));
        Path bin = binDir.resolve(execName);
        Files.writeString(bin, script, StandardOpenOption.CREATE_NEW);
        Files.setPosixFilePermissions(bin, PosixFilePermissions.fromString("rwxr-xr-x"));
    }

    private void setupIcons(Path share, String pkgName) throws IOException {
        Path iconDir = share.resolve("icons")
                .resolve("hicolor")
                .resolve("48x48")
                .resolve("apps");
        Path svgDir = share.resolve("icons")
                .resolve("hicolor")
                .resolve("scalable")
                .resolve("apps");
        Path icon = context().getValue(DebPackager.ICON_PATH).orElse(null);
        Path svg = context().getValue(DebPackager.SVG_ICON_PATH).orElse(null);
        if (svg != null && icon == null) {
            context().warningHandler().accept(DebPackager.MESSAGES.getString("message.svgnoicon"));
            svg = null;
        }
        Files.createDirectories(iconDir);
        if (icon != null) {
            Files.copy(icon, iconDir.resolve(pkgName + ".png"));
        } else {
            Files.copy(getClass().getResourceAsStream(
                    "/org/apache/netbeans/nbpackage/apache-netbeans-48x48.png"),
                    iconDir.resolve(pkgName + ".png"));
        }
        if (svg != null) {
            Files.createDirectories(svgDir);
            Files.copy(svg, svgDir.resolve(pkgName + ".svg"));
        } else if (icon == null) {
            Files.createDirectories(svgDir);
            Files.copy(getClass().getResourceAsStream(
                    "/org/apache/netbeans/nbpackage/apache-netbeans.svg"),
                    svgDir.resolve(pkgName + ".svg"));
        }
    }

    private void setupDesktopFile(Path share, String exec, String pkgName) throws IOException {
        String template = DebPackager.DESKTOP_TEMPLATE.load(context());
        Map<String, String> tokens = Map.of("EXEC", exec, "ICON", pkgName);
        String desktop = StringUtils.replaceTokens(template,
                key -> {
                    var ret = tokens.get(key);
                    if (ret != null) {
                        return ret;
                    } else {
                        return context().tokenReplacementFor(key);
                    }
                });
        Path desktopDir = share.resolve("applications");
        Files.createDirectories(desktopDir);
        String desktopFileName = context().getValue(DebPackager.DESKTOP_FILENAME)
                .map(name -> sanitize(name))
                .orElse(pkgName);
        Path desktopFile = desktopDir.resolve(desktopFileName + ".desktop");
        Files.writeString(desktopFile, desktop, StandardOpenOption.CREATE_NEW);
    }

    private void setupControlFile(Path DEBIAN) throws Exception {
        String template = DebPackager.CONTROL_TEMPLATE.load(context());
        String maintainer = context().getValue(DebPackager.DEB_MAINTAINER)
                .orElse("");
        if (maintainer.isBlank()) {
            context().warningHandler().accept(DebPackager.MESSAGES.getString("message.nomaintainer"));
        }
        String description = context().getValue(NBPackage.PACKAGE_DESCRIPTION).orElse("");
        String recommends = context().getValue(NBPackage.PACKAGE_RUNTIME).isPresent()
                ? ""
                : "java17-sdk";

        String control = StringUtils.replaceTokens(template, Map.of(
                "DEB_PACKAGE", packageName(),
                "DEB_VERSION", packageVersion(),
                "DEB_ARCH", packageArch(),
                "DEB_MAINTAINER", maintainer,
                "DEB_DESCRIPTION", description,
                "DEB_RECOMMENDS", recommends
        ));

        Path controlFile = DEBIAN.resolve("control");
        Files.writeString(controlFile, control, StandardOpenOption.CREATE_NEW);
    }
}
