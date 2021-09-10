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
import java.util.List;
import java.util.Objects;

/**
 * Abstract base class for Packager.Task implementations. Contains support for
 * extracting the application and (optional) runtime into an image directory.
 * Subclasses can enhance the image and create any necessary build files before
 * running the final packaging. The image directory name, and internal paths to
 * application and runtime can be customized as required.
 */
public abstract class AbstractPackagerTask implements Packager.Task {

    private final ExecutionContext context;

    protected AbstractPackagerTask(ExecutionContext context) {
        this.context = Objects.requireNonNull(context);
    }

    @Override
    public void validateCreateImage() throws Exception {
        // no op
    }

    /**
     * Default implementation of
     * {@link Packager.Task#createImage(java.nio.file.Path)}. Creates an image
     * directory, and extracts the application and (optional) runtime into it.
     * Name and paths can be customized if required by overriding the
     * implementations of
     * {@link #imageName(java.nio.file.Path)}, {@link #applicationDirectory(java.nio.file.Path)}
     * and {@link #runtimeDirectory(java.nio.file.Path, java.nio.file.Path)}. If
     * the runtime is extracted inside the application path, the *.conf file
     * will be updated with the relative path to the runtime (currently only for
     * RCP applications).
     *
     * @param input file or directory
     * @return path to image
     * @throws Exception
     */
    @Override
    public Path createImage(Path input) throws Exception {
        String imageName = imageName(input);
        Path image = context.destination().resolve(imageName);
        Files.createDirectory(image);

        Path appDir = applicationDirectory(image);
        Files.createDirectories(appDir);
        if (Files.isDirectory(input)) {
            copyAppFromDirectory(input, appDir);
        } else if (Files.isRegularFile(input)) {
            extractAppFromArchive(input, appDir);
        } else {
            throw new IllegalArgumentException(input.toString());
        }

        Path runtime = context.getValue(NBPackage.PACKAGE_RUNTIME)
                .map(Path::toAbsolutePath)
                .orElse(null);
        if (runtime != null) {
            Path runtimeDir = runtimeDirectory(image, appDir);
            Files.createDirectories(runtimeDir);
            if (Files.isDirectory(runtime)) {
                copyRuntimeFromDirectory(runtime, runtimeDir);
            } else if (Files.isRegularFile(runtime)) {
                extractRuntimeFromArchive(runtime, runtimeDir);
            } else {
                throw new IllegalArgumentException(runtime.toString());
            }
            if (runtimeDir.startsWith(appDir)) {
                String jdkhome = appDir.relativize(runtimeDir).toString();
                try (var confs = Files.newDirectoryStream(appDir.resolve("etc"), "*.conf")) {
                    for (Path conf : confs) {
                        addRuntimeToConf(conf, jdkhome);
                    }
                }
            }
        }
        return image;
    }

    /**
     * Access the ExecutionContext.
     *
     * @return execution context
     */
    protected final ExecutionContext context() {
        return context;
    }

    /**
     * The name for the image directory. The default implementation returns a
     * sanitized version of the package name and version. Subclasses may
     * override if they need to change the name.
     *
     * @param input the application input file (if required)
     * @return image directory name
     * @throws Exception on configuration errors
     */
    protected String imageName(Path input) throws Exception {
        String appName = context.getValue(NBPackage.PACKAGE_NAME).orElseThrow();
        String appVersion = context.getValue(NBPackage.PACKAGE_VERSION).orElseThrow();
        return sanitize(appName) + "-" + sanitize(appVersion);
    }

    /**
     * The fully resolved path inside the image in which the application will be
     * extracted. By default this is the image directory itself. Subclasses may
     * override to place the application in an alternative path inside the
     * image.
     *
     * @param image image directory
     * @return resolved application path inside image
     * @throws Exception
     */
    protected Path applicationDirectory(Path image) throws Exception {
        return image;
    }

    /**
     * The fully resolved path inside the image in which the runtime will be
     * extracted. The default implementation returns
     * <code>application.resolve("jdk")</code>. Subclasses may override to
     * extract the runtime into an alternative path inside the image.
     *
     * @param image image path
     * @param application application path
     * @return resolved runtime path inside image
     * @throws Exception
     */
    protected Path runtimeDirectory(Path image, Path application) throws Exception {
        return application.resolve("jdk");
    }

    private void extractAppFromArchive(Path input, Path destDir) throws IOException {
        var tmp = Files.createTempDirectory("nbpackageImageExtract");
        FileUtils.extractArchive(input, tmp);
        var images = FileUtils.findDirs(tmp, 2, "bin/*", "etc/*.conf");
        if (images.size() != 1) {
            throw new IOException(input.toString());
        }
        var image = images.get(0);
        FileUtils.moveFiles(image, destDir);
    }

    private void copyAppFromDirectory(Path input, Path destDir) throws IOException {
        var images = FileUtils.findDirs(input, 2, "bin/*", "etc/*.conf");
        if (images.size() != 1) {
            throw new IOException(input.toString());
        }
        var image = images.get(0);
        FileUtils.copyFiles(image, destDir);
    }

    private String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
    }

    private void extractRuntimeFromArchive(Path runtime, Path destDir) throws IOException {
        var tmp = Files.createTempDirectory("nbpackageRuntimeExtract");
        FileUtils.extractArchive(runtime, tmp);
        var runtimes = FileUtils.findDirs(tmp, 2, "bin/java*");
        if (runtimes.size() != 1) {
            throw new IOException(runtime.toString());
        }
        var java = runtimes.get(0);
        FileUtils.moveFiles(java, destDir);
    }

    private void copyRuntimeFromDirectory(Path runtime, Path destDir) throws IOException {
        var runtimes = FileUtils.findDirs(runtime, 2, "bin/java*");
        if (runtimes.size() != 1) {
            throw new IOException(runtime.toString());
        }
        var java = runtimes.get(0);
        FileUtils.copyFiles(java, destDir);
    }

    private void addRuntimeToConf(Path conf, String jdkhome) throws IOException {
        var contents = Files.readString(conf);
        contents = contents.replace("#jdkhome=\"/path/to/jdk\"", "jdkhome=\"" + jdkhome + "\"");
        // @TODO - fix this when relative links work with IDE launcher
        // contents = contents.replace("#netbeans_jdkhome=\"/path/to/jdk\"", "netbeans_jdkhome=\"" + jdkhome + "\"");
        Files.writeString(conf, contents);
    }

}
