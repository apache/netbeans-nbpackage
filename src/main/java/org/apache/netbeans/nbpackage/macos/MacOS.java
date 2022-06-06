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

import java.nio.file.Path;
import java.util.ResourceBundle;
import org.apache.netbeans.nbpackage.Option;
import org.apache.netbeans.nbpackage.Template;

/**
 * Package private options and utilities across packagers.
 */
class MacOS {

    static final ResourceBundle MESSAGES
            = ResourceBundle.getBundle(PkgPackager.class.getPackageName() + ".Messages");

    /**
     * Value for CFBundleIdentifier.
     */
    static final Option<String> BUNDLE_ID
            = Option.ofString("package.macos.bundleid",
                    MESSAGES.getString("option.bundle_id.description"));

    /**
     * Path to icon (*.icns) file.
     */
    static final Option<Path> ICON_PATH
            = Option.ofPath("package.macos.icon",
                    MESSAGES.getString("option.icon.description"));

    /**
     * Optional Info.plist template path.
     */
    static final Option<Path> INFO_TEMPLATE_PATH
            = Option.ofPath("package.macos.info-template",
                    MESSAGES.getString("option.info_template.description"));

    /**
     * Info.plist template.
     */
    static final Template INFO_TEMPLATE
            = Template.of(INFO_TEMPLATE_PATH, "Info.plist.template",
                    () -> MacOS.class.getResourceAsStream("Info.plist.template"));

    /**
     * Optional launcher (main.swift) template path.
     */
    static final Option<Path> LAUNCHER_TEMPLATE_PATH
            = Option.ofPath("package.macos.launcher-template",
                    MESSAGES.getString("option.launcher_template.description"));

    /**
     * Launcher (main.swift) template.
     */
    static final Template LAUNCHER_TEMPLATE
            = Template.of(LAUNCHER_TEMPLATE_PATH, "main.swift.template",
                    () -> MacOS.class.getResourceAsStream("main.swift.template"));

    /**
     * Unlisted Swift package template.
     */
    static final Option<Path> LAUNCHER_PACKAGE_TEMPLATE_PATH
            = Option.ofPath("package.macos.launcher-package-template", "");

    /**
     * Unlisted launcher package (Package.swift) template.
     */
    static final Template LAUNCHER_PACKAGE_TEMPLATE
            = Template.of(LAUNCHER_PACKAGE_TEMPLATE_PATH, "Package.swift.template",
                    () -> MacOS.class.getResourceAsStream("Package.swift.template"));

    /**
     * Optional codesign entitlements template path.
     */
    static final Option<Path> ENTITLEMENTS_TEMPLATE_PATH
            = Option.ofPath("package.macos.entitlements-template",
                    MESSAGES.getString("option.entitlements_template.description"));

    /**
     * Codesign entitlements template.
     */
    static final Template ENTITLEMENTS_TEMPLATE
            = Template.of(ENTITLEMENTS_TEMPLATE_PATH, "sandbox.plist.template",
                    () -> MacOS.class.getResourceAsStream("sandbox.plist.template"));

    private MacOS() {
    }

}
