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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * Definition for a text template supported by NBPackage or one of the
 * underlying packagers. Text will be loaded from the provided path option if
 * set in the configuration, or from the default stream provided.
 */
public final class Template {

    private final Option<Path> option;
    private final String name;
    private final Supplier<Reader> readerProvider;

    private Template(Option<Path> option, String name, Supplier<Reader> readerSupplier) {
        this.name = name;
        this.option = option;
        this.readerProvider = readerSupplier;
    }

    /**
     * Name of the template. May be used for a file name for exporting
     * templates.
     *
     * @return template name
     */
    public String name() {
        return name;
    }

    /**
     * The path option for overriding the default template.
     *
     * @return path option
     */
    public Option<Path> option() {
        return option;
    }

    /**
     * Load the text template. The path option from the provided
     * {@link ExecutionContext} will be used if set, or the template will be
     * loaded from the default source.
     *
     * @param context execution context for path option
     * @return loaded template
     * @throws IOException
     */
    public String load(ExecutionContext context) throws IOException {
        Path file = context.getValue(option).orElse(null);
        if (file != null) {
            return Files.readString(file);
        } else {
            try ( Reader in = readerProvider.get();  StringWriter out = new StringWriter()) {
                in.transferTo(out);
                return out.toString();
            }
        }
    }

    /**
     * Create a template definition from the provided path option and default
     * template source. The input stream for the default source should be
     * readable as UTF-8 text. A packager will usually use
     * {@link Class#getResourceAsStream(java.lang.String)}. The template name
     * may be used as a file name for exporting templates.
     *
     * @param option user configurable path option to override template
     * @param name template name / export file name
     * @param defaultSourceSupplier supplier of input stream for default template
     * @return template
     */
    public static Template of(Option<Path> option, String name,
            Supplier<InputStream> defaultSourceSupplier) {
        return new Template(option, name, ()
                -> new InputStreamReader(defaultSourceSupplier.get(), StandardCharsets.UTF_8));
    }

}
