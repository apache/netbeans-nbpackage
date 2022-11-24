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
package org.apache.netbeans.nbpackage.rpm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.netbeans.nbpackage.AbstractPackagerTask;
import org.apache.netbeans.nbpackage.ExecutionContext;
import org.apache.netbeans.nbpackage.NBPackage;
import org.apache.netbeans.nbpackage.StringUtils;

class RpmTask extends AbstractPackagerTask {

    private static final String RPM = "rpm";
    private static final String RPMBUILD = "rpmbuild";

    private String packageName;
    private String packageVersion;
    private String packageArch;

    RpmTask(ExecutionContext context) {
        super(context);
    }

    @Override
    public void validateCreateImage() throws Exception {
        super.validateCreateImage();
        if (context().isImageOnly()) {
            validateTools(RPM);
        }
    }

    @Override
    public void validateCreatePackage() throws Exception {
        validateTools(RPM, RPMBUILD);
    }

    @Override
    public Path createImage(Path input) throws Exception {
        Path image = super.createImage(input);
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
        Path desktopFile = setupDesktopFile(share, "/" + base + "/bin/" + execName, pkgName);

        Path buildRootDir = image.resolve("BUILDROOT");
        Files.createDirectories(buildRootDir);
        String buildName = packageName() + "-" + packageVersion() + "-" + "0" + "." + packageArch();
        Path buildNameDir = buildRootDir.resolve(buildName);
        Files.createDirectories(buildNameDir);
        Path buildUsrDir = buildNameDir.resolve(base);
        Files.move(baseDir, buildUsrDir);

        Path rpmsDir = image.resolve("RPMS");
        Files.createDirectories(rpmsDir);

        Path specsDir = image.resolve("SPECS");
        Files.createDirectories(specsDir);
        setupSpecFile(specsDir, execName, desktopFile.getFileName().toString());

        return image;
    }

    @Override
    public Path createPackage(Path image) throws Exception {
        Path spec = image.resolve("SPECS").resolve(packageName() + ".spec");
        int result = context().exec(RPMBUILD, "--target", packageArch(),
                "--define", "_topdir " + image.toAbsolutePath().toString(),
                "-bb", spec.toAbsolutePath().toString(),
                "--noclean");
        if (result != 0) {
            throw new Exception();
        }
        Path rpmFile;
        try (var stream = Files.newDirectoryStream(image.resolve("RPMS").resolve(packageArch()),
                "*.rpm")) {
            var itr = stream.iterator();
            if (!itr.hasNext()) {
                throw new Exception(image.toString());
            }
            rpmFile = itr.next();
        }
        Path output = context().destination().resolve(rpmFile.getFileName());
        Files.move(rpmFile, output);
        return output;
    }

    @Override
    protected String imageName(Path input) throws Exception {
        return packageName() + "-" + packageVersion() + "." + packageArch();
    }

    @Override
    protected Path applicationDirectory(Path image) throws Exception {
        return image.resolve("usr").resolve("lib").resolve("APPDIR");
    }

    private void validateTools(String... tools) throws Exception {
        if (context().isVerbose()) {
            context().infoHandler().accept(MessageFormat.format(
                    RpmPackager.MESSAGES.getString("message.validatingtools"),
                    Arrays.toString(tools)));
        }
        for (String tool : tools) {
            if (context().exec(List.of("which", tool)) != 0) {
                throw new IllegalStateException(
                        RpmPackager.MESSAGES.getString("message.missingrpmtools"));
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
            packageVersion = version;
        }
        return packageVersion;
    }

    private String packageArch() throws Exception {
        if (packageArch == null) {
            if (context().getValue(NBPackage.PACKAGE_RUNTIME).isPresent()) {
                packageArch = context()
                        .execAndGetOutput(RPM, "--eval '%{_arch}'")
                        .strip();
            } else {
                packageArch = "noarch";
            }
        }
        return packageArch;
    }

    private String sanitize(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\+\\-\\.]", "-");
    }

    private String sanitizeVersion(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\+\\-\\.\\~]", "-");
    }

    private Path findLauncher(Path binDir) throws IOException {
        try ( var files = Files.list(binDir)) {
            return files.filter(f -> !f.getFileName().toString().endsWith(".exe"))
                    .findFirst().orElseThrow(IOException::new);
        }
    }

    private void setupLauncher(Path binDir, String packageLocation, String execName)
            throws IOException {
        String template = RpmPackager.LAUNCHER_TEMPLATE.load(context());
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
        Path icon = context().getValue(RpmPackager.ICON_PATH).orElse(null);
        Path svg = context().getValue(RpmPackager.SVG_ICON_PATH).orElse(null);
        if (svg != null && icon == null) {
            context().warningHandler().accept(RpmPackager.MESSAGES.getString("message.svgnoicon"));
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

    private Path setupDesktopFile(Path share, String exec, String pkgName) throws IOException {
        String template = RpmPackager.DESKTOP_TEMPLATE.load(context());
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
        String desktopFileName = context().getValue(RpmPackager.DESKTOP_FILENAME)
                .map(name -> sanitize(name))
                .orElse(pkgName);
        Path desktopFile = desktopDir.resolve(desktopFileName + ".desktop");
        Files.writeString(desktopFile, desktop, StandardOpenOption.CREATE_NEW);
        return desktopFile;
    }

    private void setupSpecFile(Path specsDir, String execName, String desktopName) throws Exception {
        String template = RpmPackager.SPEC_TEMPLATE.load(context());
        String spec = StringUtils.replaceTokens(template, Map.ofEntries(
                Map.entry("RPM_PACKAGE", packageName()),
                Map.entry("RPM_VERSION", packageVersion()),
                Map.entry("RPM_ARCH", packageArch()),
                Map.entry("RPM_SUMMARY_LINE", context().getValue(NBPackage.PACKAGE_DESCRIPTION)
                        .map(value -> "Summary: " + value)
                        .orElse("")),
                Map.entry("RPM_LICENSE_LINE", context().getValue(RpmPackager.RPM_LICENSE)
                        .map(value -> "License: " + value)
                        .orElse("")),
                Map.entry("RPM_GROUP_LINE", context().getValue(RpmPackager.RPM_GROUP)
                        .map(value -> "Group: " + value)
                        .orElse("")),
                Map.entry("RPM_URL_LINE", context().getValue(NBPackage.PACKAGE_URL)
                        .map(value -> "URL: " + value)
                        .orElse("")),
                Map.entry("RPM_VENDOR_LINE", context().getValue(NBPackage.PACKAGE_PUBLISHER)
                        .map(value -> "Vendor: " + value)
                        .orElse("")),
                Map.entry("RPM_MAINTAINER_LINE", context().getValue(RpmPackager.RPM_MAINTAINER)
                        .map(value -> "Packager: " + value)
                        .orElse("")),
                Map.entry("RPM_RECOMMENDS_LINE", context().getValue(NBPackage.PACKAGE_RUNTIME)
                        .map(value -> "Recommends: java-devel >= 11")
                        .orElse("")),
                Map.entry("RPM_DESCRIPTION", context().getValue(NBPackage.PACKAGE_DESCRIPTION)
                        .orElse("")),
                Map.entry("RPM_EXEC_NAME", execName),
                Map.entry("RPM_DESKTOP_NAME", desktopName)
        ));

        Path specFile = specsDir.resolve(packageName + ".spec");
        Files.writeString(specFile, spec, StandardOpenOption.CREATE_NEW);
    }
}
