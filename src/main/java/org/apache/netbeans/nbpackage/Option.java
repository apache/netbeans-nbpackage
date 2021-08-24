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

import java.nio.file.Path;
import java.util.Objects;

/**
 * Definition for an option supported by NBPackage or one of the underlying
 * packagers. Option values should be obtained from the {@link ExecutionContext}
 * when required using the relevant Option definition - values may be stored as
 * text, and will be parsed by the Option's parser when required.
 *
 * @param <T> type of the option
 */
public final class Option<T> {

    private final String key;
    private final Class<T> type;
    private final String defaultValue;
    private final Parser<? extends T> parser;
    private final String comment;

    private Option(String key, Class<T> type, String defaultValue, Parser<? extends T> parser, String comment) {
        this.key = Objects.requireNonNull(key);
        this.type = Objects.requireNonNull(type);
        this.defaultValue = Objects.requireNonNull(defaultValue);
        this.parser = Objects.requireNonNull(parser);
        this.comment = Objects.requireNonNull(comment);
    }

    /**
     * Key used for defining option values in Configuration, properties files,
     * etc.
     *
     * @return option key
     */
    public String key() {
        return key;
    }

    /**
     * The underlying type of the option.
     *
     * @return type
     */
    public Class<T> type() {
        return type;
    }

    /**
     * Default value of the Option. This is defined as the text that parses to
     * the default value rather than the value itself. One reason for this is
     * that the default value can be defined to include tokens to be replaced
     * before parsing.
     *
     * @return default value as text
     */
    public String defaultValue() {
        return defaultValue;
    }

    /**
     * Parse the option into the required value type.
     *
     * @param text value to parse
     * @return value as type
     * @throws Exception on parsing errors
     */
    public T parse(String text) throws Exception {
        return parser.parse(text);
    }

    /**
     * A text comment to act as user help in understanding the use and possible
     * values of this option.
     *
     * @return help text
     */
    public String comment() {
        return comment;
    }

    /**
     * Create an Option of a String type.
     *
     * @param key key used to store option in configuration
     * @param defaultValue default value as text (may include tokens)
     * @param comment help text for user
     * @return option
     */
    public static Option<String> ofString(String key,
            String defaultValue,
            String comment) {
        return new Option<>(key, String.class, defaultValue, s -> s, comment);
    }

    /**
     * Create an option of a Path type.
     *
     * @param key key used to store option in configuration
     * @param defaultValue default value as text (may include tokens)
     * @param comment help text for user
     * @return option
     */
    public static Option<Path> ofPath(String key,
            String defaultValue,
            String comment) {
        return new Option<>(key, Path.class, defaultValue, s -> Path.of(s), comment);
    }

    /**
     * Create an option of arbitrary type.
     *
     * @param <T> type of option
     * @param key key used to store option in configuration
     * @param type option type
     * @param defaultValue default value as text (may include tokens)
     * @param parser parser to convert text to option type
     * @param comment help text for user
     * @return option
     */
    public static <T> Option<T> of(String key,
            Class<T> type,
            String defaultValue,
            Parser<? extends T> parser,
            String comment) {
        return new Option<>(key, type, defaultValue, parser, comment);
    }

    /**
     * Parser to convert text value into option type.
     * 
     * @param <T> option type
     */
    @FunctionalInterface
    public static interface Parser<T> {

        /**
         * Parse text to type.
         * 
         * @param text to parse
         * @return value as type
         * @throws Exception on parsing error
         */
        public T parse(String text) throws Exception;

    }

}
