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
            = Option.ofString("package.macos.bundleid", "",
                    MESSAGES.getString("option.bundle_id.description"));
    
    /**
     * Path to icon (*.icns) file.
     */
    static final Option<Path> ICON_PATH
            = Option.ofPath("package.macos.icon", "",
                    MESSAGES.getString("option.icon.description"));
    
    /**
     * Optional Info.plist template.
     */
    static final Option<Path> INFO_TEMPLATE_PATH
            = Option.ofPath("package.macos.info-template", "",
                    MESSAGES.getString("option.info_template.description"));
    
    private MacOS() {
    }
    
}
