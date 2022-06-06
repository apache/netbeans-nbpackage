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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import picocli.CommandLine;

/**
 * Entry point for command line usage.
 */
public class Main {

    /**
     * Main entry point for command line usage.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        var cmd = new CommandLine(new Launcher());
        cmd.setResourceBundle(NBPackage.MESSAGES);
        int ret = cmd.execute(args);
        System.exit(ret);
    }

    @CommandLine.Command(mixinStandardHelpOptions = true)
    private static class Launcher implements Callable<Integer> {

        @CommandLine.Option(names = {"-t", "--type"},
                descriptionKey = "option.type.description",
                completionCandidates = TypesCandidates.class
        )
        private String packageType;

        @CommandLine.Option(names = {"-i", "--input"},
                descriptionKey = "option.input.description")
        private Path input;

        @CommandLine.Option(names = {"--input-image"},
                descriptionKey = "option.inputimage.description")
        private Path inputImage;

        @CommandLine.Option(names = {"-o", "--output"},
                descriptionKey = "option.output.description")
        private Path output;

        @CommandLine.Option(names = {"-c", "--config"},
                descriptionKey = "option.config.description")
        private Path config;

        @CommandLine.Option(names = {"--save-config"},
                descriptionKey = "option.saveconfig.description")
        private Path configOut;
        
        @CommandLine.Option(names = {"--save-templates"},
                descriptionKey = "option.savetemplates.description")
        private Path templatesOut;

        @CommandLine.Option(names = {"--image-only"},
                descriptionKey = "option.imageonly.description")
        private boolean imageOnly;

        @CommandLine.Option(names = {"-v", "--verbose"},
                descriptionKey = "option.verbose.description")
        private boolean verbose;

        @CommandLine.Option(names = {"-P"},
                descriptionKey = "option.property.description")
        private Map<String, String> options;

        @Override
        public Integer call() throws Exception {
            try {
                if (input == null && inputImage == null && !hasAuxTasks()) {
                    warning(NBPackage.MESSAGES.getString("message.notasks"));
                    return 1;
                }
                if (input != null && inputImage != null) {
                    warning(NBPackage.MESSAGES.getString("message.inputandimage"));
                    return 2;
                }
                var cb = Configuration.builder();
                if (config != null) {
                    cb.load(config.toAbsolutePath());
                }
                if (packageType != null && !packageType.isBlank()) {
                    cb.set(NBPackage.PACKAGE_TYPE, packageType);
                }

                if (options != null && !options.isEmpty()) {
                    options.forEach((key, value) -> {
                        var opt = NBPackage.options()
                                .filter(o -> o.key().equals(key))
                                .findFirst()
                                .orElseThrow(() -> new IllegalArgumentException(key));
                        cb.set(opt, value);
                    });
                }
                
                if (verbose) {
                    cb.verbose();
                }

                var conf = cb.build();

                if (configOut != null) {
                    NBPackage.writeFullConfiguration(conf, configOut);
                }

                if (templatesOut != null) {
                    NBPackage.copyTemplates(conf, templatesOut);
                }
                
                Path dest = output == null ? Path.of("") : output;

                Path created = null;
                if (input != null) {
                    if (imageOnly) {
                        created = NBPackage.createImage(input, conf, dest);
                    } else {
                        created = NBPackage.createPackage(input, conf, dest);
                    }
                } else if (inputImage != null) {
                    created = NBPackage.packageImage(inputImage, conf, dest);
                }
                return 0;
            } catch (Exception ex) {
                warning(ex.getClass().getSimpleName());
                warning(ex.getLocalizedMessage());
                if (verbose) {
                    var sw = new StringWriter();
                    var pw = new PrintWriter(sw);
                    ex.printStackTrace(pw);
                    warning(sw.toString());
                }
                return 3;
            }
        }

        private void info(String msg) {
            System.out.println(msg);
        }

        private void warning(String msg) {
            var ansiMsg = CommandLine.Help.Ansi.AUTO.string(
                    "@|bold,red " + msg + "|@"
            );
            System.out.println(ansiMsg);
        }
        
        private boolean hasAuxTasks() {
            return configOut != null || templatesOut != null;
        } 

    }

    private static class TypesCandidates implements Iterable<String> {

        @Override
        public Iterator<String> iterator() {
            return NBPackage.packagers().map(Packager::name).sorted().iterator();
        }

    }

}
