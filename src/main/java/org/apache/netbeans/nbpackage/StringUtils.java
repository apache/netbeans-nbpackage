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

import java.text.MessageFormat;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A range of useful file utility functions for packagers.
 */
public class StringUtils {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\$\\{(.+?)\\}");

    private StringUtils() {
        // static utilities
    }

    /**
     * Replace tokens of the form <code>${KEY}</code> in the provided input
     * text. The provided token value map is used to convert tokens to their
     * replacement text. If the token value map has no entry for a given token,
     * the original token text will be left in place - to fail in this situation
     * use {@link #replaceTokensOrFail(java.lang.String, java.util.Map)}.
     *
     * @param input text possibly containing tokens
     * @param tokenValues map of token values
     * @return text with tokens replaced
     */
    public static String replaceTokens(String input,
            Map<String, String> tokenValues) {
        return replaceTokensImpl(input, tokenValues::get, true);
    }

    /**
     * Replace tokens of the form <code>${KEY}</code> in the provided input
     * text. The provided token value map is used to convert tokens to their
     * replacement text. If the token value map has no entry for a given token,
     * an exception is thrown - to leave the token text in place use
     * {@link #replaceTokens(java.lang.String, java.util.Map)}.
     *
     * @param input text possibly containing tokens
     * @param tokenValues map of token values
     * @return text with tokens replaced
     * @throws IllegalArgumentException if no token value is available
     */
    public static String replaceTokensOrFail(String input,
            Map<String, String> tokenValues) {
        return replaceTokensImpl(input, tokenValues::get, false);
    }

    /**
     * Replace tokens of the form <code>${KEY}</code> in the provided input
     * text. The provided token value source is used to convert tokens to their
     * replacement text. If the token value source returns <code>null</code> for
     * a given token, the original token text will be left in place - to fail in
     * this situation use
     * {@link #replaceTokensOrFail(java.lang.String, java.util.function.Function)}.
     *
     * @param input text possibly containing tokens
     * @param tokenValueSource convert tokens to replacement text
     * @return text with tokens replaced
     */
    public static String replaceTokens(String input,
            Function<String, String> tokenValueSource) {
        return replaceTokensImpl(input, tokenValueSource, true);
    }

    /**
     * Replace tokens of the form <code>${KEY}</code> in the provided input
     * text. The provided token value source is used to convert tokens to their
     * replacement text. If the token value source returns <code>null</code> for
     * a given token, an exception is thrown - to leave the token text in place
     * use
     * {@link #replaceTokens(java.lang.String, java.util.function.Function)}.
     *
     * @param input text possibly containing tokens
     * @param tokenValueSource convert tokens to replacement text
     * @return text with tokens replaced
     * @throws IllegalArgumentException if no token value is available
     */
    public static String replaceTokensOrFail(String input,
            Function<String, String> tokenValueSource) {
        return replaceTokensImpl(input, tokenValueSource, false);
    }

    private static String replaceTokensImpl(String input,
            Function<String, String> tokenSource,
            boolean lenient) {
        var matcher = TOKEN_PATTERN.matcher(input);
        var sb = new StringBuilder();
        while (matcher.find()) {
            var token = matcher.group(1);
            var replacement = tokenSource.apply(token);
            if (replacement == null) {
                if (lenient) {
                    replacement = "${" + token + "}";
                } else {
                    var msg = MessageFormat.format(
                            NBPackage.MESSAGES.getString("message.invalidtoken"),
                            token);
                    throw new IllegalArgumentException(msg);
                }
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

}
