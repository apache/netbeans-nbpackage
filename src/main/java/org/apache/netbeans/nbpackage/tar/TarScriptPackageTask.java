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
package org.apache.netbeans.nbpackage.tar;

import org.apache.netbeans.nbpackage.AbstractPackagerTask;
import org.apache.netbeans.nbpackage.Architecture;
import org.apache.netbeans.nbpackage.ExecutionContext;
import org.apache.netbeans.nbpackage.FileUtils;
import org.apache.netbeans.nbpackage.NBPackage;
import org.apache.netbeans.nbpackage.StringUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import static org.apache.netbeans.nbpackage.Architecture.AARCH64;
import static org.apache.netbeans.nbpackage.Architecture.X86_64;

class TarScriptPackageTask extends AbstractPackagerTask {

    private static final String ARCH_AARCH64 = "aarch64";
    private static final String ARCH_X86_64 = "x86_64";
    private static final String ARCH_NOARCH = "noarch";

    private String packageArch;

    TarScriptPackageTask(ExecutionContext context) {
        super(context);
    }

    @Override
    protected void customizeImage(Path image) throws Exception {
        String appDir = findLauncher(
                image.resolve("APPDIR").resolve("bin"))
                .getFileName().toString();

        Path launcherDir = calculateAppPath(image).resolve("launcher");
        Files.createDirectories(launcherDir);

        Path icon = context().getValue(TarScriptPackager.ICON_PATH).orElse(null);
        Path svg = context().getValue(TarScriptPackager.SVG_ICON_PATH).orElse(null);
        if (svg != null && icon == null) {
            context().warningHandler().accept(TarScriptPackager.MESSAGES.getString("message.svgnoicon"));
            svg = null;
        }

        if (icon != null) {
            Files.copy(icon, launcherDir.resolve(appDir + ".png"));
        } else {
            Files.copy(getClass().getResourceAsStream(
                    "/org/apache/netbeans/nbpackage/apache-netbeans-48x48.png"),
                    launcherDir.resolve(appDir + ".png"));
        }
        if (svg != null) {
            Files.copy(svg, launcherDir.resolve(appDir + ".svg"));
        } else if (icon == null) {
            Files.copy(getClass().getResourceAsStream(
                    "/org/apache/netbeans/nbpackage/apache-netbeans.svg"),
                    launcherDir.resolve(appDir + ".svg"));
        }
    }

    @Override
    protected Path buildPackage(Path image) throws Exception {
        String appDir = findLauncher(
                image.resolve("APPDIR").resolve("bin"))
                .getFileName().toString();

        String appName = context().getValue(NBPackage.PACKAGE_NAME).orElse(appDir);
        String appNameSafe = sanitize(appName);

        Path dst = context().destination().resolve(image.getFileName().toString() + ".sh");

        String desktop = setupDesktopFile("exe", "icon");

        String launcher = TarScriptPackager.LAUNCHER_TEMPLATE.load(context());

        String template = TarScriptPackager.TAR_SCRIPT_TEMPLATE.load(context());
        Map<String, String> tokens = Map.of("package.tar.app_name_safe", appNameSafe,
                                            "package.tar.app_name", appName, "package.tar.app_dir", appDir,
                                            "package.tar.desktop", desktop, "package.tar.launcher", launcher);
        String script = StringUtils.replaceTokens(template,
                key -> {
                    var ret = tokens.get(key);
                    if (ret != null) {
                        return ret;
                    } else {
                        return context().tokenReplacementFor(key);
                    }
                });

        FileUtils.createEmbeddedTarScript(script, image, dst);

        try {
            Files.setPosixFilePermissions(dst, PosixFilePermissions.fromString("rwxr-xr-x"));
        } catch (UnsupportedOperationException ex) {
            context().warningHandler().accept("UnsupportedOperationException : PosixFilePermissions");
        }
        return dst;
    }

    @Override
    protected String calculateImageName(Path input) throws Exception {
        return super.calculateImageName(input) + "." + packageArch();
    }

    @Override
    protected Path calculateRuntimePath(Path image, Path application) throws Exception {
        return application.resolve("jdk");
    }

    @Override
    protected Path calculateAppPath(Path image) throws IOException {
        return image.resolve("APPDIR");
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
                                        ARCH_AARCH64;
                                    case X86_64 ->
                                        ARCH_X86_64;
                                };
                            }).orElseGet(() -> {
                                context().warningHandler().accept(
                                        TarScriptPackager.MESSAGES.getString("message.unknownarch"));
                                return ARCH_NOARCH;
                            });
                        } else {
                            return ARCH_NOARCH;
                        }
                    });
        }
        return packageArch;
    }

    private String sanitize(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\+\\-\\.]", "-");
    }

    private Path findLauncher(Path binDir) throws IOException {
        try ( var files = Files.list(binDir)) {
            return files.filter(f -> !f.getFileName().toString().endsWith(".exe"))
                    .findFirst().orElseThrow(IOException::new);
        }
    }

    private String setupDesktopFile(String exec, String pkgName) throws IOException {
        String template = TarScriptPackager.DESKTOP_TEMPLATE.load(context());
        return context().replaceTokens(template);
    }

}
