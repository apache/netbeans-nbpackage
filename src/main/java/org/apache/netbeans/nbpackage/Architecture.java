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
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * An enum reflecting known architectures and their synonyms. Also contains
 * utility methods for architecture detection. All synonyms are stored and
 * reported in lower case. Detection is case insensitive.
 */
public enum Architecture {

    /**
     * Value for x86_64 architecture. Synonyms are {@code x86_64},
     * {@code x86-64}, {@code amd64} and {@code x64}.
     */
    X86_64("x86_64", "x86-64", "amd64", "x64"),
    /**
     * Value for AArch64 architecture. Synonyms are {@code aarch64} and
     * {@code arm64}.
     */
    AARCH64("aarch64", "arm64");

    private final List<String> synonyms;

    private Architecture(String... synonyms) {
        this.synonyms = List.of(synonyms);
    }

    /**
     * List of synonyms. The returned list is unmodifiable.
     *
     * @return list of synonyms
     */
    public List<String> synonyms() {
        return synonyms;
    }

    /**
     * Query whether the provided value is a synonym of this architecture. This
     * method checks whether the synonyms list contains the lowercase version of
     * the value.
     *
     * @param arch value to query
     * @return true if value is a synonym of this architecture
     */
    public boolean isSynonym(String arch) {
        return synonyms.contains(arch.toLowerCase(Locale.ROOT));
    }

    /**
     * Try to detect an architecture from the provided path. The detection is
     * based solely on the provided path, and this method will not attempt to
     * access any files.
     * <p>
     * Implementation note : this method looks for synonyms in the file name of
     * the path. It may in future look for other clues in the path hierarchy.
     *
     * @param path path to detect architecture
     * @return architecture if detected
     */
    public static Optional<Architecture> detectFromPath(Path path) {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        for (Architecture arch : values()) {
            for (String synonym : arch.synonyms()) {
                if (fileName.contains(synonym)) {
                    return Optional.of(arch);
                }
            }
        }
        return Optional.empty();
    }
}
