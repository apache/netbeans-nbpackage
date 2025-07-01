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
package org.apache.netbeans.nbpackage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility methods for tests.
 */
public class TestUtils {

    private TestUtils() {
    }

    /**
     * Build a fake application structure, providing the files required by any
     * packager. The directory must not already exist.
     *
     * @param parent parent directory
     * @param dirname name of the created directory
     * @param branding branding token of application
     * @return created directory
     * @throws java.io.IOException
     */
    public static Path buildFakeApp(Path parent, String dirname, String branding)
            throws IOException {
        Path dir = Files.createDirectory(parent.resolve(dirname));
        Path appBin = Files.createDirectory(dir.resolve("bin"));
        Path appEtc = Files.createDirectory(dir.resolve("etc"));
        Path platform = Files.createDirectory(dir.resolve("platform"));
        Files.createFile(appBin.resolve(branding));
        Files.createFile(appBin.resolve(branding + ".exe"));
        Files.createFile(appBin.resolve(branding + "64.exe"));
        Files.createFile(appEtc.resolve(branding + ".conf"));
        Files.createFile(platform.resolve("module"));
        return dir;
    }

    /**
     * Build a fake JDK structure, providing the files required by any packager.
     * The directory must not already exist.
     *
     * @param parent parent directory
     * @param dirname name of the created directory
     * @param windows whether files should have .exe suffix
     * @return created directory
     * @throws java.io.IOException
     */
    public static Path buildFakeJDK(Path parent, String dirname, boolean windows)
            throws IOException {
        Path dir = Files.createDirectory(parent.resolve(dirname));
        Path jdkBin = Files.createDirectory(dir.resolve("bin"));
        if (windows) {
            Files.createFile(jdkBin.resolve("java.exe"));
        } else {
            Files.createFile(jdkBin.resolve("java"));
        }
        return dir;
    }

    /**
     * Build an image using the provided packager and configuration.
     *
     * @param packager packager to use
     * @param input input application
     * @param config configuration
     * @param destination output directory (image parent)
     * @return path to created image
     * @throws Exception
     */
    public static Path buildImage(Packager packager, Path input,
            Configuration config, Path destination) throws Exception {
        ExecutionContext exec = new ExecutionContext(
                packager, input, config, destination, true);
        return exec.execute();
    }

    /**
     * Utility to resolve paths.
     *
     * @param path root path
     * @param fragments path fragments
     * @return resolved path
     */
    public static Path resolve(Path path, String... fragments) {
        Path result = path;
        for (String fragment : fragments) {
            result = result.resolve(fragment);
        }
        return result;
    }

}
