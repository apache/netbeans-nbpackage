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

import java.util.Optional;

/**
 * Provide access to configuration, environment and utilities for packager
 * tasks. An execution context is valid only for execution of a single task.
 */
public class ExecutionContext {

    private final Configuration configuration;

    ExecutionContext(Configuration config) {
        this.configuration = config;
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
            try {
                return Optional.of(option.parse(raw));
            } catch (Exception ex) {
                throw new IllegalArgumentException(ex);
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
     * Replace tokens of the form <code>${KEY}</code> in the provided input
     * text.
     *
     * @param input
     * @return
     */
    public String replaceTokens(String input) {
        return input;
    }

}
