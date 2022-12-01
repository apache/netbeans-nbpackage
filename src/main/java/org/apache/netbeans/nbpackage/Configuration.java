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
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * Configuration for running NBPackage tasks. Configurations are immutable - use
 * the builder to create.
 */
public final class Configuration {

    private final Properties properties;
    private final boolean verbose;
    private final Consumer<String> infoHandler;
    private final Consumer<String> warningHandler;

    private Configuration(Builder builder) {
        this.properties = new Properties();
        this.properties.putAll(builder.properties);
        this.verbose = builder.verbose;
        this.infoHandler = builder.infoHandler;
        this.warningHandler = builder.warningHandler;
    }

//    /**
//     * Get a value as String from the underlying configuration properties.
//     *
//     * @param key configuration key
//     * @param defaultValue default value
//     * @return value or default
//     */
//    public String getValue(String key, String defaultValue) {
//        return properties.getProperty(key, defaultValue);
//    }
    /**
     * Get an option value as String from the underlying configuration
     * properties, or the option default if not set.
     *
     * @param option option definition
     * @return value or option default
     */
    public String getValue(Option<?> option) {
        return properties.getProperty(option.key(), option.defaultValue());
    }

    /**
     * If tasks should be verbose.
     *
     * @return verbose
     */
    public boolean isVerbose() {
        return verbose;
    }

    /**
     * The consumer of info messages for tasks to output to the user.
     *
     * @return info message handler
     */
    public Consumer<String> infoHandler() {
        return infoHandler;
    }

    /**
     * The consumer of warning messages for tasks to output to the user.
     *
     * @return warning message handler
     */
    public Consumer<String> warningHandler() {
        return warningHandler;
    }

    /**
     * Configuration builder.
     *
     * @return builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for configurations.
     */
    public static class Builder {

        private final Properties properties;

        private boolean verbose;
        private Consumer<String> infoHandler;
        private Consumer<String> warningHandler;

        private Builder() {
            properties = new Properties();
            warningHandler = s -> System.out.println(s);
            infoHandler = warningHandler;
        }

        /**
         * A path to a properties file to load into the configuration. The token
         * <code>${CONFIG}</code> in any values will be replaced with the parent
         * path of the properties file, allowing for relative paths to be
         * specified.
         *
         * @param path configuration properties file
         * @return this
         * @throws IOException on problems loading
         */
        public Builder load(Path path) throws IOException {
            var extraProps = new Properties();
            var configReplace = Map.of("CONFIG",
                    path.getParent().toString());
            try ( var reader = Files.newBufferedReader(path)) {
                extraProps.load(reader);
            }
            extraProps.entrySet().forEach(e -> {
                e.setValue(StringUtils.replaceTokens(e.getValue().toString(),
                        configReplace));
            });
            properties.putAll(extraProps);
            return this;
        }

//        /**
//         * Set a property in the underlying configuration.
//         *
//         * @param key property key
//         * @param value property value
//         * @return this
//         */
//        public Builder set(String key, String value) {
//            properties.setProperty(key, value);
//            return this;
//        }
        /**
         * Set an option in the underlying configuration.
         *
         * @param option option definition
         * @param value option value as text
         * @return this
         */
        public Builder set(Option<?> option, String value) {
            properties.setProperty(option.key(), value);
            return this;
        }

        /**
         * Set verbose. Increase output from packaging tasks for diagnostic
         * purposes.
         *
         * @return this
         */
        public Builder verbose() {
            this.verbose = true;
            return this;
        }

        /**
         * Set the message handlers for information and warning messages to be
         * output to the user. By default these go to System.out. There is no
         * error message handler - errors will result in an exception that
         * terminates execution.
         *
         * @param warning handler for warning messages
         * @param info handler for information messages
         * @return this
         */
        public Builder messageHandlers(Consumer<String> warning, Consumer<String> info) {
            Objects.requireNonNull(warning);
            Objects.requireNonNull(info);
            this.warningHandler = warning;
            this.infoHandler = info;
            return this;
        }

        /**
         * Build the configuration.
         *
         * @return configuration
         */
        public Configuration build() {
            return new Configuration(this);
        }

    }

}
