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
import java.nio.file.StandardOpenOption;
import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.netbeans.nbpackage.appimage.AppImagePackager;
import org.apache.netbeans.nbpackage.deb.DebPackager;
import org.apache.netbeans.nbpackage.innosetup.InnoSetupPackager;
import org.apache.netbeans.nbpackage.zip.ZipPackager;

/**
 * Entry point for executing NBPackage tasks.
 */
public final class NBPackage {

    static final ResourceBundle MESSAGES
            = ResourceBundle.getBundle(NBPackage.class.getPackageName() + ".Messages");

    /**
     * Option definition for package name.
     */
    public static final Option<String> PACKAGE_NAME = Option.ofString(
            "package.name", "", MESSAGES.getString("option.name.help"));

    /**
     * Option definition for package version.
     */
    public static final Option<String> PACKAGE_VERSION = Option.ofString(
            "package.version", "1.0", MESSAGES.getString("option.version.help"));

    /**
     * Option definition for package type.
     */
    public static final Option<String> PACKAGE_TYPE = Option.ofString(
            "package.type", "", MESSAGES.getString("option.type.help"));

    /**
     * Option definition for path to the optional Java runtime to include in the
     * package.
     */
    public static final Option<Path> PACKAGE_RUNTIME = Option.ofPath(
            "package.runtime", "", MESSAGES.getString("option.runtime.help"));

// @TODO generate list from service loader if modularizing
    private static final List<Packager> PACKAGERS = List.of(
            new AppImagePackager(),
            new DebPackager(),
            new InnoSetupPackager(),
            new ZipPackager()
    );

    private static final List<Option<?>> GLOBAL_OPTIONS
            = List.of(PACKAGE_NAME, PACKAGE_VERSION, PACKAGE_TYPE, PACKAGE_RUNTIME);

    private NBPackage() {
        // no op
    }

    /**
     * Create a package from the provided input and configuration. The input
     * should be an archive or directory containing the Apache NetBeans IDE or
     * platform application to package. The destination is the directory to
     * create the package in, and must already exist. The returned path is the
     * created package.
     *
     * @param input archive or directory with application
     * @param configuration packaging configuration
     * @param destination directory to create package in
     * @return path to created package
     * @throws Exception on errors in creating package
     */
    public static Path createPackage(Path input,
            Configuration configuration,
            Path destination)
            throws Exception {
        var packager = findPackager(configuration.getValue(PACKAGE_TYPE));
        var exec = new ExecutionContext(packager,
                input.toAbsolutePath(),
                configuration,
                destination.toAbsolutePath(),
                false);
        return exec.execute();
    }

    /**
     * Create a package image from the provided input and configuration. The
     * nature of a package image will differ between packaging types, but will
     * usually be a directory in the required layout for the underlying
     * packaging execution. A package image may have additional packager
     * specific build files alongside.#
     * <p>
     * The input should be an archive or directory containing the Apache
     * NetBeans IDE or platform application to package. The destination is the
     * directory to create the image in, and must already exist. The returned
     * path is the created image.
     *
     * @param input archive or directory with application
     * @param configuration packaging configuration
     * @param destination directory to create image in
     * @return path to created image
     * @throws Exception on errors in creating package
     */
    public static Path createImage(Path input,
            Configuration configuration,
            Path destination)
            throws Exception {
        var packager = findPackager(configuration.getValue(PACKAGE_TYPE));
        var exec = new ExecutionContext(packager,
                input.toAbsolutePath(),
                configuration,
                destination.toAbsolutePath(),
                true);
        return exec.execute();
    }

    /**
     * Create a package from the image and any build files previously output by
     * {@link #createImage(java.nio.file.Path, org.apache.netbeans.nbpackage.Configuration, java.nio.file.Path)}.
     * The destination is the directory to create the package in, and must
     * already exist. The returned path is the created package.
     *
     * @param inputImage input image
     * @param configuration packaging configuration
     * @param destination directory to create package in
     * @return path to created package
     * @throws Exception on errors in creating package
     */
    public static Path packageImage(Path inputImage,
            Configuration configuration,
            Path destination)
            throws Exception {
        var packager = findPackager(configuration.getValue(PACKAGE_TYPE));
        var exec = new ExecutionContext(packager,
                inputImage.toAbsolutePath(),
                configuration,
                destination.toAbsolutePath());
        return exec.execute();
    }

    /**
     * Write out the full configuration, combining the provided configuration
     * with the default values of other options. If package type is set in the
     * provided configuration, then only global options and the options for that
     * type will be output. Help text as comments above each value may be
     * optionally included in the output. The output is in properties file
     * format, suitable for use with
     * {@link Configuration.Builder#load(java.nio.file.Path)}.
     *
     * @param configuration additional configuration
     * @param includeComments whether to include option comments
     * @return
     */
    public static String writeFullConfiguration(Configuration configuration,
            boolean includeComments) {
        var type = configuration.getValue(PACKAGE_TYPE);
        var sb = new StringBuilder();
        Stream<Option<?>> options = type.isBlank() ? options() : options(type);
        options.forEachOrdered(o -> writeOption(sb, configuration, o, includeComments));
        return sb.toString();
    }

    /**
     * Write out the full configuration to the given file, combining the
     * provided configuration with the default values of other options. If
     * package type is set in the provided configuration, then only global
     * options and the options for that type will be output. The output is in
     * properties file format, suitable for use with
     * {@link Configuration.Builder#load(java.nio.file.Path)}.
     *
     * @param configuration additional configuration
     * @param destination destination file
     * @throws IOException
     */
    public static void writeFullConfiguration(Configuration configuration,
            Path destination) throws IOException {
        Files.writeString(destination.toAbsolutePath(),
                writeFullConfiguration(configuration, true),
                StandardOpenOption.CREATE_NEW);
    }

    /**
     * Query all available packagers. Not all packagers are guaranteed to run on
     * the current system / OS.
     *
     * @return stream of available packagers
     */
    public static Stream<Packager> packagers() {
        return PACKAGERS.stream();
    }

    /**
     * Find a packager by name.
     *
     * @param name packager name
     * @return packager
     * @throws IllegalArgumentException if name is blank or no packager by this
     * name exists
     */
    public static Packager findPackager(String name) {
        if (name.isBlank()) {
            throw new IllegalArgumentException(MESSAGES.getString("message.notype"));
        }
        return packagers().filter(p -> name.equals(p.name()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException(
                MessageFormat.format(MESSAGES.getString("message.invalidtype"),
                        name)));
    }

    /**
     * Query all available global options.
     *
     * @return stream of global option definitions
     */
    public static Stream<Option<?>> globalOptions() {
        return GLOBAL_OPTIONS.stream();
    }

    /**
     * Query all available options, including global options and options
     * supported by all of the available packagers.
     *
     * @return stream of all available option definitions
     */
    public static Stream<Option<?>> options() {
        return Stream.concat(globalOptions(),
                packagers().flatMap(Packager::options));
    }

    /**
     * Query all options available for the given packager type, including global
     * options.
     *
     * @param packagerName name of packager
     * @return stream of available option definitions
     * @throws IllegalArgumentException if no packager by this name exists
     */
    public static Stream<Option<?>> options(String packagerName) {
        return Stream.concat(globalOptions(),
                findPackager(packagerName).options());
    }

    // @TODO properly escape and support multi-line comments / values
    private static void writeOption(StringBuilder sb, Configuration conf, Option<?> option, boolean comment) {
        String value = conf.getValue(option);
        if (comment) {
            sb.append("# ").append(option.comment()).append(System.lineSeparator());
        }
        sb.append(option.key()).append("=").append(value).append(System.lineSeparator());
    }

}
