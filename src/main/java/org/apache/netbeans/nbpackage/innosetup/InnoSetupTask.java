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
package org.apache.netbeans.nbpackage.innosetup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.netbeans.nbpackage.AbstractPackagerTask;
import org.apache.netbeans.nbpackage.Architecture;
import org.apache.netbeans.nbpackage.ExecutionContext;
import org.apache.netbeans.nbpackage.FileUtils;
import org.apache.netbeans.nbpackage.NBPackage;
import org.apache.netbeans.nbpackage.StringUtils;

import static org.apache.netbeans.nbpackage.innosetup.InnoSetupPackager.*;

class InnoSetupTask extends AbstractPackagerTask {

    InnoSetupTask(ExecutionContext context) {
        super(context);
    }

    @Override
    protected void checkImageRequirements() throws Exception {
        context().getValue(NBPackage.PACKAGE_ARCH).ifPresent(arch -> {
            if (!Architecture.X86_64.isSynonym(arch)) {
                context().warningHandler().accept(MESSAGES.getString("message.invalidarch"));
            }
        });
    }

    @Override
    protected void checkPackageRequirements() throws Exception {
        context().getValue(TOOL_PATH)
                .orElseThrow(() -> new IllegalStateException(
                MESSAGES.getString("message.noinnosetuptool")));
    }

    @Override
    protected void customizeImage(Path image) throws Exception {
        String execName = findExecName(image.resolve("APPDIR").resolve("bin"));

        Path appDir = image.resolve(execName);
        Files.move(image.resolve("APPDIR"), appDir);

        setupIcons(image, execName);
        setupLicenseFile(image);
    }

    @Override
    protected void finalizeImage(Path image) throws Exception {
        String execName = findExecName(FileUtils.find(image, "*/bin").get(0));
        createInnoSetupScript(image, execName);
    }

    @Override
    protected Path buildPackage(Path image) throws Exception {
        Path tool = context().getValue(TOOL_PATH)
                .orElseThrow(() -> new IllegalStateException(
                MESSAGES.getString("message.noinnosetuptool")))
                .toAbsolutePath();
        Path issFile;
        try (var stream = Files.newDirectoryStream(image, "*.iss")) {
            var itr = stream.iterator();
            if (!itr.hasNext()) {
                throw new IllegalArgumentException(image.toString());
            }
            issFile = itr.next();
        }

        var cmd = List.of(tool.toString(), issFile.getFileName().toString());
        var pb = new ProcessBuilder(cmd);
        pb.directory(image.toFile());
        context().exec(pb);

        Path exeFile;
        try (var stream = Files.newDirectoryStream(image.resolve("Output"), "*.exe")) {
            var itr = stream.iterator();
            if (!itr.hasNext()) {
                throw new IllegalArgumentException(image.toString());
            }
            exeFile = itr.next();
        }
        Path output = context().destination().resolve(exeFile.getFileName());
        Files.move(exeFile, output);
        Files.delete(image.resolve("Output"));
        return output;
    }

    @Override
    protected String calculateImageName(Path input) throws Exception {
        return super.calculateImageName(input) + "-InnoSetup";
    }

    @Override
    protected Path calculateAppPath(Path image) throws Exception {
        return image.resolve("APPDIR");
    }

    @Override
    protected Path calculateRuntimePath(Path image, Path application) throws Exception {
        return application.resolve("jdk");
    }

    @Override
    protected Path calculateRootPath(Path image) throws Exception {
        return FileUtils.find(image, "*/bin").get(0).getParent();
    }

    private Path findLauncher(Path binDir) throws IOException {
        try (var files = Files.list(binDir)) {
            return files.filter(f -> f.getFileName().toString().endsWith("64.exe"))
                    .findFirst().orElseThrow(IOException::new);
        }
    }

    private String findExecName(Path binDir) throws IOException {
        var bin = findLauncher(binDir);
        var name = bin.getFileName().toString();
        return name.substring(0, name.length() - "64.exe".length());
    }

    private void setupIcons(Path image, String execName) throws IOException {
        Path icoFile = context().getValue(ICON_PATH).orElse(null);
        Path dstFile = image.resolve(execName)
                .resolve("etc")
                .resolve(execName + ".ico");
        if (icoFile != null) {
            Files.copy(icoFile, dstFile);
        } else {
            Files.copy(getClass().getResourceAsStream(
                    "/org/apache/netbeans/nbpackage/apache-netbeans.ico"),
                    dstFile
            );
        }
    }

    private void setupLicenseFile(Path image) throws IOException {
        var license = context().getValue(LICENSE_PATH).orElse(null);
        if (license == null) {
            return;
        }
        var name = license.getFileName().toString().toLowerCase(Locale.ROOT);
        var isTXT = name.endsWith(".txt");
        var isRTF = name.endsWith(".rtf");
        if (!isTXT && !isRTF) {
            throw new IllegalArgumentException(license.toString());
        }
        var target = image.resolve(isTXT ? "license.txt" : "license.rtf");
        Files.copy(license, target);
    }

    private void createInnoSetupScript(Path image, String execName) throws IOException {
        // make sure loaded template has correct line endings
        String template = ISS_TEMPLATE.load(context()).lines()
                .collect(Collectors.joining("\r\n", "", "\r\n"));

        List<Path> files;
        try (var l = Files.list(image.resolve(execName))) {
            files = l.sorted().collect(Collectors.toList());
        }

        String installDeleteSection = buildInstallDeleteSection(files);
        String filesSection = buildFilesSection(execName, files);

        String appName = context().getValue(NBPackage.PACKAGE_NAME).orElse(execName);
        String appNameSafe = sanitize(appName);
        String appID = context().getValue(APPID).orElse(appName);
        String appVersion = context().getValue(NBPackage.PACKAGE_VERSION).orElse("1.0");
        String appPublisher = context().getValue(NBPackage.PACKAGE_PUBLISHER)
                .map(pub -> "AppPublisher=" + pub)
                .orElse("");
        String appURL = context().getValue(NBPackage.PACKAGE_URL)
                .map(url -> "AppPublisherURL=" + url)
                .orElse("");

        String appLicense;
        if (Files.exists(image.resolve("license.txt"))) {
            appLicense = "LicenseFile=license.txt";
        } else if (Files.exists(image.resolve("license.rtf"))) {
            appLicense = "LicenseFile=license.rtf";
        } else {
            appLicense = "";
        }

        String execParam = context().getValue(NBPackage.PACKAGE_RUNTIME)
                .map(p -> "Parameters: \"--jdkhome \"\"{app}\\jdk\"\"\";")
                .orElse("");

        String outputFilename = appNameSafe.replaceAll("\\s", "-") + "-"
                + sanitize(appVersion).replaceAll("\\s", "-")
                + context().getValue(NBPackage.PACKAGE_ARCH)
                        .map(arch -> "-" + sanitize(arch).replaceAll("\\s", ""))
                        .orElse("");

        Map<String, String> map = Map.ofEntries(
                Map.entry("APP_ID", appID),
                Map.entry("APP_NAME", appName),
                Map.entry("APP_NAME_SAFE", appNameSafe),
                Map.entry("APP_VERSION", appVersion),
                Map.entry("APP_PUBLISHER", appPublisher),
                Map.entry("APP_PUBLISHER_URL", appURL),
                Map.entry("APP_LICENSE", appLicense),
                Map.entry("OUTPUT_FILENAME", outputFilename),
                Map.entry("INSTALL_DELETE", installDeleteSection),
                Map.entry("FILES", filesSection),
                Map.entry("EXEC_NAME", execName),
                Map.entry("PARAMETERS", execParam)
        );

        String script = StringUtils.replaceTokens(template, map);

        Files.writeString(image.resolve(execName + ".iss"), script,
                StandardOpenOption.CREATE_NEW);
    }

    private String sanitize(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String buildInstallDeleteSection(List<Path> files) {
        return files.stream()
                .map(Path::getFileName)
                .map(Path::toString)
                .map(name -> "Type: filesandordirs; Name: \"{app}\\" + name + "\"")
                .collect(Collectors.joining("\r\n", "", "\r\n"));
    }

    private String buildFilesSection(String execName, List<Path> files) {
        return files.stream()
                .map(file -> buildFilesSectionLine(execName, file))
                .collect(Collectors.joining("\r\n", "", "\r\n"));
    }

    private String buildFilesSectionLine(String execName, Path file) {
        boolean isDir = Files.isDirectory(file);
        String fileName = file.getFileName().toString();
        if (isDir) {
            return "Source: \"" + execName + "\\" + fileName + "\\*\"; DestDir: \"{app}\\"
                    + fileName + "\"; Flags: ignoreversion recursesubdirs createallsubdirs";
        } else {
            return "Source: \"" + execName + "\\" + fileName + "\"; DestDir: \"{app}\"; Flags: ignoreversion";
        }
    }

}
