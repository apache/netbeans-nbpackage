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
package org.apache.netbeans.nbpackage.macos;

import java.util.List;
import java.util.stream.Stream;
import org.apache.netbeans.nbpackage.ExecutionContext;
import org.apache.netbeans.nbpackage.Option;
import org.apache.netbeans.nbpackage.Packager;
import org.apache.netbeans.nbpackage.Template;

/**
 * Packager for macOS PKG installer.
 */
public class PkgPackager implements Packager {

    private static final List<Option<?>> PKG_OPTIONS = List.of(
            MacOS.BUNDLE_ID,
            MacOS.ICON_PATH,
            MacOS.INFO_TEMPLATE_PATH,
            MacOS.LAUNCHER_TEMPLATE_PATH,
            MacOS.ENTITLEMENTS_TEMPLATE_PATH);

    private static final List<Template> PKG_TEMPLATE = List.of(
            MacOS.INFO_TEMPLATE,
            MacOS.LAUNCHER_TEMPLATE,
            MacOS.ENTITLEMENTS_TEMPLATE
    );

    @Override
    public Task createTask(ExecutionContext context) {
        return new PkgTask(context);
    }

    @Override
    public String name() {
        return "macos-pkg";
    }

    @Override
    public Stream<Option<?>> options() {
        return PKG_OPTIONS.stream();
    }

    @Override
    public Stream<Template> templates() {
        return PKG_TEMPLATE.stream();
    }
    
}
