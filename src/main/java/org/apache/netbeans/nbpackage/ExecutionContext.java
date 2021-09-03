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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provide access to configuration, environment and utilities for packager
 * tasks. An execution context is valid only for execution of a single task.
 */
public final class ExecutionContext {

    private static final String TOKEN_IMAGE_DIR = "IMAGE_DIR";
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\$\\{(.+?)\\}");

    private final Packager packager;
    private final Path input;
    private final Path destination;
    private final Configuration configuration;
    private final boolean imageOnly;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    private Path imagePath;
    private List<Path> buildFiles;

    /**
     * Constructor used from NBPackage createPackage and createImage.
     */
    ExecutionContext(Packager packager,
            Path input,
            Configuration config,
            Path destination,
            boolean imageOnly) {
        this.packager = packager;
        this.configuration = config;
        this.input = input;
        this.imagePath = null;
        this.buildFiles = null;
        this.destination = destination;
        this.imageOnly = imageOnly;
    }

    /**
     * Constructor used from NBPackage packageImage.
     */
    ExecutionContext(Packager packager,
            Path inputImage,
            List<Path> buildFiles,
            Configuration configuration,
            Path destination) {
        this.packager = packager;
        this.input = null;
        this.imagePath = inputImage;
        this.buildFiles = buildFiles;
        this.configuration = configuration;
        this.destination = destination;
        this.imageOnly = false;
    }

    /**
     * Get the input file. May be null if packaging an image.
     *
     * @return input file or null
     */
    public Path input() {
        return input;
    }

    /**
     * Get the path to the image. May be null.
     *
     * @return image path
     */
    public Path image() {
        return imagePath;
    }

    /**
     * Get any additional build files. May be null.
     *
     * @return additional build files
     */
    public List<Path> buildFiles() {
        return buildFiles == null ? null : List.copyOf(buildFiles);
    }

    /**
     * Get the destination directory. Never null.
     *
     * @return destination path
     */
    public Path destination() {
        return destination;
    }

    /**
     * Execute the given external process. The process will be executed using
     * the current working directory. If control over the working directory or
     * environment is required, use {@link #exec(java.lang.ProcessBuilder)}.
     * <p>
     * If {@link #isVerbose()} then process output streams will be routed to the
     * info handler, else they will be discarded.
     *
     * @param command command line
     * @return exit code of process
     * @throws IOException
     * @throws InterruptedException
     */
    public int exec(List<String> command) throws IOException, InterruptedException {
        return exec(new ProcessBuilder(command));
    }

    /**
     * Execute the given external process. If {@link #isVerbose()} then process
     * output streams will be routed to the info handler, else they will be
     * discarded.
     *
     * @param processBuilder command to execute
     * @return exit code of process
     * @throws IOException
     * @throws InterruptedException
     */
    public int exec(ProcessBuilder processBuilder) throws IOException, InterruptedException {
        boolean showOutput = isVerbose();
        if (showOutput) {
            processBuilder.redirectErrorStream(true);
            processBuilder.redirectOutput(ProcessBuilder.Redirect.PIPE);
        } else {
            processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);
        }
        Process p = processBuilder.start();
        if (showOutput) {
            var info = infoHandler();
            var warning = warningHandler();
            executor.submit(() -> {
                try ( var in = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    in.lines().forEachOrdered(info);
                } catch (IOException ex) {
                    warning.accept(ex.getClass().getSimpleName());
                }

            });
        }
        return p.waitFor();
    }

    /**
     * Execute and get the output of the given external process. The process
     * will be executed using the current working directory. If control over the
     * working directory or environment is required, use
     * {@link #execAndGetOutput(java.lang.ProcessBuilder)}.
     * <p>
     * If {@link #isVerbose()} then the error stream of the process will be
     * routed to the info handler, else it will be discarded.
     * <p>
     * Implementation note : execution will be routed to a temporary file and
     * read on process exit.
     *
     * @param command command line
     * @return output of command
     * @throws IOException
     * @throws InterruptedException
     */
    public String execAndGetOutput(List<String> command) throws IOException, InterruptedException {
        return execAndGetOutput(new ProcessBuilder(command));
    }

    /**
     * Execute and get the output of the given external process. If
     * {@link #isVerbose()} then the error stream of the process will be routed
     * to the info handler, else it will be discarded.
     * <p>
     * Implementation note : execution will be routed to a temporary file and
     * read on process exit.
     *
     * @param processBuilder command to execute
     * @return output of command
     * @throws IOException
     * @throws InterruptedException
     */
    public String execAndGetOutput(ProcessBuilder processBuilder) throws IOException, InterruptedException {
        boolean showOutput = isVerbose();
        Path tmp = Files.createTempFile("nbpackage", ".tmp");
        processBuilder.redirectOutput(tmp.toFile());
        processBuilder.redirectError(showOutput ? ProcessBuilder.Redirect.PIPE
                : ProcessBuilder.Redirect.DISCARD);
        Process p = processBuilder.start();
        if (showOutput) {
            var info = configuration.infoHandler();
            var warning = configuration.warningHandler();
            executor.submit(() -> {
                try ( var in = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
                    in.lines().forEachOrdered(info);
                } catch (IOException ex) {
                    warning.accept(ex.getClass().getSimpleName());
                }

            });
        }
        p.waitFor();
        String out = Files.readString(tmp);
        Files.delete(tmp);
        return out;
    }

    /**
     * Split the given command line string into individual command line tokens.
     *
     * @param command commend line string
     * @return tokenized command line
     */
    public List<String> splitCommandLine(String command) {
        return parseParameters(command);
    }

    /**
     * Get the value of the provided {@link Option} if set. Tokens in the text
     * will be replaced before the option is parsed.
     * <p>
     * Will return {@link Optional#EMPTY} if the value has not been set and has
     * no default. Will throw an exception a value exists and cannot be parsed
     * by the option.
     *
     * @param <T> option type
     * @param option option definition
     * @return value or empty
     * @throws IllegalArgumentException if the option cannot be parsed
     */
    public <T> Optional<T> getValue(Option<T> option) {
        var raw = configuration.getValue(option);
        if (!raw.isBlank()) {
            raw = replaceTokens(raw);
            try {
                return Optional.of(option.parse(raw));
            } catch (Exception ex) {
                var msg = MessageFormat.format(
                        NBPackage.MESSAGES.getString("message.invalidoptionvalue"),
                        option.key(), raw);
                throw new IllegalArgumentException(msg, ex);
            }
        } else {
            return Optional.empty();
        }
    }

    /**
     * Get the raw text value of an option, without replacement of any tokens.
     *
     * @param option option definition
     * @return text value
     */
    public String getRawValue(Option<?> option) {
        return configuration.getValue(option);
    }

    /**
     * Check if the provided option is at its default value - unset or
     * explicitly.
     *
     * @param option option definition
     * @return true if default value
     */
    public boolean isDefaultValue(Option<?> option) {
        return option.defaultValue().equals(getRawValue(option));
    }

    /**
     * If tasks should be verbose.
     *
     * @return verbose
     */
    public boolean isVerbose() {
        return configuration.isVerbose();
    }

    /**
     * Replace tokens of the form <code>${KEY}</code> in the provided input
     * text. This uses the default token value source that supports tokens from
     * option keys, as well as dynamic tokens during execution (currently only
     * <code>${IMAGE_DIR}</code> for the path to the application image).
     *
     * @param input text possibly containing tokens
     * @return text with tokens replaced
     */
    public String replaceTokens(String input) {
        return replaceTokens(input, this::tokenReplacementFor);
    }

    /**
     * Replace tokens of the form <code>${KEY}</code> in the provided input
     * text. The provided token value source is used to convert tokens to their
     * replacement text.
     *
     * @param input text possibly containing tokens
     * @param tokenValueSource convert tokens to replacement text
     * @return text with tokens replaced
     */
    public String replaceTokens(String input, UnaryOperator<String> tokenValueSource) {
        var matcher = TOKEN_PATTERN.matcher(input);
        var sb = new StringBuilder();
        while (matcher.find()) {
            var token = matcher.group(1);
            var replacement = tokenValueSource.apply(token);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Default token lookup. Useful if a task wants to provide its own token
     * value source in
     * {@link #replaceTokens(java.lang.String, java.util.function.UnaryOperator)}
     * but fallback to the default token replacement.
     *
     * @param token key to replace
     * @return replaced value
     * @throws IllegalArgumentException if token is invalid
     */
    public String tokenReplacementFor(String token) {
        if (TOKEN_IMAGE_DIR.equals(token)) {
            if (imagePath != null) {
                return imagePath.toString();
            }
        } else {
            Option<?> opt = NBPackage.options()
                    .filter(o -> o.key().equals(token))
                    .findFirst().orElse(null);
            if (opt != null) {
                return getValue(opt)
                        .map(Object::toString)
                        .orElse(opt.defaultValue());
            }
        }
        var msg = MessageFormat.format(NBPackage.MESSAGES.getString("message.invalidtoken"), token);
        throw new IllegalArgumentException(msg);
    }

    /**
     * The consumer of info messages for tasks to output to the user.
     *
     * @return info message handler
     */
    public Consumer<String> infoHandler() {
        return configuration.infoHandler();
    }

    /**
     * The consumer of warning messages for tasks to output to the user.
     *
     * @return warning message handler
     */
    public Consumer<String> warningHandler() {
        return configuration.warningHandler();
    }

    /**
     * Create and execute the packager task - called from NBPackage.
     *
     * @return path to image or package created
     * @throws Exception on error
     */
    Path execute() throws Exception {
        try {
            var task = packager.createTask(this);
            if (input != null) {
                task.validateCreateImage();
                task.validateCreateBuildFiles();
            }
            if (!imageOnly) {
                task.validateCreatePackage();
            }
            if (input != null) {
                imagePath = task.createImage(input);
                buildFiles = task.createBuildFiles(imagePath);
            }
            if (imageOnly) {
                return imagePath;
            }
            return task.createPackage(imagePath, buildFiles);
        } finally {
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    // copied from NetBeans' org.openide.util.BaseUtilities
    private static List<String> parseParameters(String s) {
        final int NULL = 0x0;
        final int IN_PARAM = 0x1;
        final int IN_DOUBLE_QUOTE = 0x2;
        final int IN_SINGLE_QUOTE = 0x3;
        ArrayList<String> params = new ArrayList<>(5);
        char c;

        int state = NULL;
        StringBuilder buff = new StringBuilder(20);
        int slength = s.length();

        for (int i = 0; i < slength; i++) {
            c = s.charAt(i);
            switch (state) {
                case NULL:
                    switch (c) {
                        case '\'':
                            state = IN_SINGLE_QUOTE;
                            break;
                        case '"':
                            state = IN_DOUBLE_QUOTE;
                            break;
                        default:
                            if (!Character.isWhitespace(c)) {
                                buff.append(c);
                                state = IN_PARAM;
                            }
                    }
                    break;
                case IN_SINGLE_QUOTE:
                    if (c != '\'') {
                        buff.append(c);
                    } else {
                        state = IN_PARAM;
                    }
                    break;
                case IN_DOUBLE_QUOTE:
                    switch (c) {
                        case '\\':
                            char peek = (i < slength - 1) ? s.charAt(i + 1) : Character.MIN_VALUE;
                            if (peek == '"' || peek == '\\') {
                                buff.append(peek);
                                i++;
                            } else {
                                buff.append(c);
                            }
                            break;
                        case '"':
                            state = IN_PARAM;
                            break;
                        default:
                            buff.append(c);
                    }
                    break;
                case IN_PARAM:
                    switch (c) {
                        case '\'':
                            state = IN_SINGLE_QUOTE;
                            break;
                        case '"':
                            state = IN_DOUBLE_QUOTE;
                            break;
                        default:
                            if (Character.isWhitespace(c)) {
                                params.add(buff.toString());
                                buff.setLength(0);
                                state = NULL;
                            } else {
                                buff.append(c);
                            }
                    }
                    break;
            }
        }
        if (buff.length() > 0) {
            params.add(buff.toString());
        }

        return List.copyOf(params);
    }
}
