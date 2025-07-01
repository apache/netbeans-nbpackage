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
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.stream.*;

public final class PackageTest {

    enum OS {
        Windows, MacOS, Linux
    }

    private final OS os;

    private PackageTest(OS os) {
        this.os = os;
    }

    private void execute() throws Exception {
        System.out.println("Starting " + os + " test");
        System.out.println("========================");
        System.out.println("\nStarting preparation.\n");
        Path root = Path.of("").toAbsolutePath();
        System.out.println("Root : " + root);
        Path nbpackage = findNBPackage(root);
        System.out.println("NBPackage : " + nbpackage);

        Path testDir = root.resolve("target").resolve("package-test");
        Files.createDirectories(testDir);
        System.out.println("Test dir : " + testDir);
        Path input = buildInput(testDir);
        System.out.println("Input zip : " + input);
        listZipContents(input);
        Path runtime = buildRuntime(testDir);
        System.out.println("Runtime : " + runtime);
        Path output = Files.createDirectory(testDir.resolve("output"));
        System.out.println("Output : " + output);
        System.out.println("Finished preparation.\n\n");

        System.out.println("Starting package tests\n");

        switch (os) {
            case Linux -> {
                runNBPackage("linux-deb", nbpackage, input, runtime, output);
                runNBPackage("linux-rpm", nbpackage, input, runtime, output);
            }
            case MacOS -> {
                runNBPackage("macos-pkg", nbpackage, input, runtime, output);
            }
            case Windows -> {
                runNBPackage("windows-innosetup", nbpackage, input, runtime, output);
            }
        }

        System.out.println("\nTests completed OK");
    }

    private Path findNBPackage(Path root) throws IOException {
        try (Stream<Path> files = Files.list(root.resolve("target"))) {
            return files
                    .filter(p -> p.getFileName().toString().startsWith("nbpackage-"))
                    .filter(p -> Files.isDirectory(p))
                    .findFirst()
                    .map(p -> p.resolve("bin"))
                    .map(p -> {
                        if (os == OS.Windows) {
                            return p.resolve("nbpackage.cmd");
                        } else {
                            return p.resolve("nbpackage");
                        }
                    })
                    .filter(p -> Files.exists(p))
                    .orElseThrow(() -> new IOException("Cannot find NBPackage"));
        }
    }

    private Path buildInput(Path testDir) throws IOException {
        Path zip = testDir.resolve("app-12.3-rc4-bin.zip");
        try (FileSystem zipFS = FileSystems.newFileSystem(zip,
                Map.of("create", true, "enablePosixFileAttributes", true))) {
            Path bin = zipFS.getPath("bin");
            Files.createDirectory(bin);
            var perms = PosixFilePermissions.asFileAttribute(
                    PosixFilePermissions.fromString("rwxr-xr-x")
            );
            Files.createFile(bin.resolve("app"), perms);
            Files.createFile(bin.resolve("app.exe"));
            Files.createFile(bin.resolve("app64.exe"));
            Path etc = zipFS.getPath("etc");
            Files.createDirectory(etc);
            Path conf = Files.createFile(etc.resolve("app.conf"));
        }
        return zip;
    }

    private Path buildRuntime(Path testDir) throws IOException {
        String fileName = switch (os) {
            case Windows ->
                "OpenJDK24U-jdk_x64_windows_hotspot_24.0.1_9.zip";
            case MacOS ->
                "OpenJDK24U-jdk_aarch64_mac_hotspot_24.0.1_9.zip";
            case Linux ->
                "OpenJDK24U-jdk_x64_linux_hotspot_24.0.1_9.zip";
        };
        Path zip = testDir.resolve(fileName);
        try (FileSystem zipFS = FileSystems.newFileSystem(zip,
                Map.of("create", true, "enablePosixFileAttributes", true))) {
            Path bin = zipFS.getPath("bin");
            Files.createDirectory(bin);
            if (os == OS.Windows) {
                Files.createFile(bin.resolve("java.exe"));
            } else {
                var perms = PosixFilePermissions.asFileAttribute(
                        PosixFilePermissions.fromString("rwxr-xr-x")
                );
                Files.createFile(bin.resolve("java"), perms);
            }
        }
        return zip;
    }

    private void listZipContents(Path zip) throws IOException {
        try (FileSystem zipFS = FileSystems.newFileSystem(zip)) {
            Files.walk(zipFS.getPath("/")).forEach(p -> {
                System.out.println(" --- " + p);
            });
        }
    }

    private void runNBPackage(String type, Path nbpackage, Path input,
            Path runtime, Path output) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        cmd.add(nbpackage.toString());
        cmd.add("--verbose");
        cmd.add("--type");
        cmd.add(type);
        cmd.add("--input");
        cmd.add(input.toString());
        cmd.add("-Pname=NetBeans RCP");
        cmd.add("-Pversion=12.3-rc4");
        cmd.add("-Pruntime=" + runtime.toString());
        if ("windows-innosetup".equals(type)) {
            cmd.add("-Pinnosetup.tool=" + System.getenv("INNOSETUP_PATH"));
        }
        cmd.add("--output");
        cmd.add(output.toString());

        System.out.println("\nRunning NBPackage\n -- " + cmd);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.inheritIO();
        int result = pb.start().waitFor();
        if (result != 0) {
            throw new IOException("Process failed with exit code : " + result);
        }
        System.out.println("\nNBPackage completed\n");
    }

    public static void main(String[] args) throws Exception {
        String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        PackageTest test;
        if (osName.contains("windows")) {
            test = new PackageTest(OS.Windows);
        } else if (osName.contains("mac")) {
            test = new PackageTest(OS.MacOS);
        } else {
            test = new PackageTest(OS.Linux);
        }
        test.execute();
    }

}
