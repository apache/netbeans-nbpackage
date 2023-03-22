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
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Abstract base class for Packager.Task implementations. Contains support for
 * extracting the application and (optional) runtime into an image directory.
 * Subclasses can enhance the image and create any necessary build files before
 * running the final packaging. The image directory name, and internal paths to
 * application and runtime can be customized as required.
 */
public abstract class AbstractPackagerTask implements Packager.Task {

    private final ExecutionContext context;

    /**
     * Create task with provided context.
     *
     * @param context execution context
     */
    protected AbstractPackagerTask(ExecutionContext context) {
        this.context = Objects.requireNonNull(context);
    }

    /**
     * Implementation of {@link Packager.Task#validateCreateImage()}. Calls
     * through to the optional {@link #checkImageRequirements()} hook for task
     * implementations to validate any requirements for building the image.
     *
     * @throws Exception if image creation requirements are not met
     */
    @Override
    public final void validateCreateImage() throws Exception {
        checkImageRequirements();
    }

    /**
     * Implementation of {@link Packager.Task#validateCreatePackage() ()}. Calls
     * through to the optional {@link #checkPackageRequirements()} hook for task
     * implementations to validate any requirements for building the final
     * package.
     *
     * @throws Exception if package creation requirements are not met
     */
    @Override
    public final void validateCreatePackage() throws Exception {
        checkPackageRequirements();
    }

    /**
     * Implementation of {@link Packager.Task#createImage(java.nio.file.Path)}.
     * Creates an image directory, and extracts the application and (optional)
     * runtime into it. Name and paths can be customized if required by
     * overriding the implementations of
     * {@link #calculateImageName(java.nio.file.Path)}, {@link #calculateAppPath(java.nio.file.Path)}
     * and
     * {@link #calculateRuntimePath(java.nio.file.Path, java.nio.file.Path)}. If
     * the runtime is extracted inside the application path, the *.conf file
     * will be updated with the relative path to the runtime (currently only for
     * RCP applications).
     * <p>
     * After the base image is created,
     * {@link #customizeImage(java.nio.file.Path)} will be called, allowing the
     * task implementation to alter and extend the base image (eg. adding
     * scripts, icons, build files etc.).
     * <p>
     * Following customization, any filtering tasks such as merging or deleting
     * files will be run. Following filtering, the optional
     * {@link #finalizeImage(java.nio.file.Path)} hook is called, allowing the
     * task implementation to perform any additional steps that require the
     * final image state (eg. file lists).
     *
     * @param input file or directory
     * @return path to image
     * @throws Exception if unable to create image
     */
    @Override
    public final Path createImage(Path input) throws Exception {
        String imageName = calculateImageName(input);
        Path image = context.destination().resolve(imageName);
        Files.createDirectory(image);

        Path appDir = calculateAppPath(image);
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
            Path runtimeDir = calculateRuntimePath(image, appDir);
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

        customizeImage(image);

        String filterPattern = context.getValue(NBPackage.PACKAGE_REMOVE).orElse(null);
        if (filterPattern != null) {
            removeFromImage(image, filterPattern);
        }

        Path mergeSource = context.getValue(NBPackage.PACKAGE_MERGE)
                .map(Path::toAbsolutePath)
                .orElse(null);
        if (mergeSource != null) {
            Path rootDir = calculateRootPath(image, appDir);
            if (Files.isDirectory(mergeSource)) {
                processMergeFromDirectory(mergeSource, image, rootDir, appDir);
            } else if (Files.isRegularFile(mergeSource)) {
                processMergeFromArchive(mergeSource, image, rootDir, appDir);
            } else {
                throw new IllegalArgumentException(mergeSource.toString());
            }
        }

        finalizeImage(image);

        return image;
    }

    /**
     * Implementation of
     * {@link Packager.Task#createPackage(java.nio.file.Path)}. Calls through to
     * {@link #buildPackage(java.nio.file.Path)}.
     *
     * @param image path to image directory
     * @return path to created package
     * @throws Exception if package creation fails
     */
    @Override
    public final Path createPackage(Path image) throws Exception {
        return buildPackage(image);
    }

    /**
     * Optional hook called during {@link #validateCreateImage()} for tasks to
     * check image building requirements. Tasks that require specific
     * configurations or tools in order to build an image should check them here
     * and throw an exception if requirements are not met.
     *
     * @throws Exception if image requirements are not met
     */
    protected void checkImageRequirements() throws Exception {
        // no op hook
    }

    /**
     * Optional hook called during {@link #validateCreatePackage() ()} for tasks
     * to check package building requirements. Tasks that require specific
     * configurations or tools in order to build a package should check them
     * here and throw an exception if requirements are not met.
     *
     * @throws Exception if image requirements are not met
     */
    protected void checkPackageRequirements() throws Exception {
        // no op hook
    }

    /**
     * Customize the base image. Called during
     * {@link #createImage(java.nio.file.Path)} after the application has been
     * extracted, along with the optional runtime, into the image directory. The
     * task implementation should add additional required files, such as
     * scripts, icons and builds files at this stage.
     * <p>
     * This method is called prior to filtering of the image. Any step that
     * requires the final image layout or list of files should be done in the
     * optional {@link #finalizeImage(java.nio.file.Path)} hook.
     *
     * @param image base image directory
     * @throws Exception if image customization fails
     */
    protected abstract void customizeImage(Path image) throws Exception;

    /**
     * Optional hook called during {@link #createImage(java.nio.file.Path)}
     * after the image has been filtered. Any step that requires the final image
     * layout, such as creating a build file with a complete file list, should
     * be done at this stage.
     *
     * @param image filtered image directory
     * @throws Exception if image finalization fails
     */
    protected void finalizeImage(Path image) throws Exception {
        // no op hook
    }

    /**
     * Build the package from the provided image. Called during
     * {@link #createPackage(java.nio.file.Path)}.
     *
     * @param image path to image directory
     * @return path to created package
     * @throws Exception if package build fails
     */
    protected abstract Path buildPackage(Path image) throws Exception;

    /**
     * Access the ExecutionContext.
     *
     * @return execution context
     */
    protected final ExecutionContext context() {
        return context;
    }

    /**
     * Hook to calculate the name for the image directory. The default
     * implementation returns a sanitized version of the package name and
     * version. Subclasses may override if they need to change the name.
     *
     * @param input the application input file (if required)
     * @return image directory name
     * @throws Exception on configuration errors
     */
    protected String calculateImageName(Path input) throws Exception {
        String appName = context.getValue(NBPackage.PACKAGE_NAME).orElseThrow();
        String appVersion = context.getValue(NBPackage.PACKAGE_VERSION).orElseThrow();
        return sanitize(appName) + "-" + sanitize(appVersion);
    }

    /**
     * Hook to calculate the path inside the image in which the application will
     * be extracted. By default this is the image directory itself. Subclasses
     * may override to place the application in an alternative path inside the
     * image.
     *
     * @param image image directory
     * @return resolved application path inside image
     * @throws Exception if unable to compute path
     */
    protected Path calculateAppPath(Path image) throws Exception {
        return image;
    }

    /**
     * Hook to calculate the path inside the image in which the runtime will be
     * extracted. The default implementation returns
     * <code>application.resolve("jdk")</code>. Subclasses may override to
     * extract the runtime into an alternative path inside the image.
     *
     * @param image image path
     * @param application application path
     * @return resolved runtime path inside image
     * @throws Exception if unable to compute path
     */
    protected Path calculateRuntimePath(Path image, Path application) throws Exception {
        return application.resolve("jdk");
    }

    /**
     * Hook to calculate the root path, mainly used in merging files. The
     * default implementation returns the image directory itself. Subclasses may
     * override to provide a more suitable path in the image.
     * <p>
     * The notion of suitable root path will differ from packager to packager.
     * It should be the root of files within the image that are installed on the
     * end user's system. This may be the path relating to the filesystem root
     * in a Linux package, the root of a macOS app bundle, or the application
     * directory itself.
     *
     * @param image image path
     * @param application application path
     * @return resolved root path
     * @throws Exception if unable to compute path
     */
    protected Path calculateRootPath(Path image, Path application) throws Exception {
        return image;
    }

    private void extractAppFromArchive(Path input, Path destDir) throws IOException {
        var tmp = Files.createTempDirectory("nbpackageImageExtract");
        FileUtils.extractArchive(input, tmp);
        var images = FileUtils.findDirs(tmp, 5, "bin/*", "etc/*.conf");
        if (images.size() != 1) {
            throw new IOException(input.toString());
        }
        var image = images.get(0);
        FileUtils.moveFiles(image, destDir);
        FileUtils.deleteFiles(tmp);
    }

    private void copyAppFromDirectory(Path input, Path destDir) throws IOException {
        var images = FileUtils.findDirs(input, 5, "bin/*", "etc/*.conf");
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
        var runtimes = FileUtils.findDirs(tmp, 5, "bin/java*");
        if (runtimes.size() != 1) {
            throw new IOException(runtime.toString());
        }
        var java = runtimes.get(0);
        FileUtils.moveFiles(java, destDir);
        FileUtils.deleteFiles(tmp);
    }

    private void copyRuntimeFromDirectory(Path runtime, Path destDir) throws IOException {
        var runtimes = FileUtils.findDirs(runtime, 5, "bin/java*");
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

    private void removeFromImage(Path image, String pattern) throws IOException {
        var filesToRemove = FileUtils.find(image, pattern);
        for (Path file : filesToRemove) {
            FileUtils.deleteFiles(file);
        }
    }

    private void processMergeFromArchive(Path archive, Path image,
            Path rootDir, Path appDir) throws IOException {
        var tmp = Files.createTempDirectory("nbpackageMergeExtract");
        FileUtils.extractArchive(archive, tmp);
        processMergeFromDirectory(tmp, image, rootDir, appDir);
        FileUtils.deleteFiles(tmp);
    }

    private void processMergeFromDirectory(Path sourceDir, Path imageDir,
            Path rootDir, Path appDir) throws IOException {
        try (var stream = Files.list(sourceDir)) {
            var files = stream.sorted().collect(Collectors.toList());
            for (Path file : files) {
                if (Files.isDirectory(file)) {
                    String name = file.getFileName().toString();
                    Path dest;
                    if ("__ROOT".equals(name)) {
                        dest = rootDir;
                    } else if ("__APP".equals(name)) {
                        dest = appDir;
                    } else {
                        dest = imageDir.resolve(name);
                        Files.createDirectories(dest);
                    }
                    FileUtils.copyFiles(file, dest);
                } else {
                    Path dest = imageDir.resolve(file.getFileName());
                    Files.copy(file, dest, StandardCopyOption.COPY_ATTRIBUTES);
                    FileUtils.ensureWritable(dest);
                }
            }
        }
    }

}
