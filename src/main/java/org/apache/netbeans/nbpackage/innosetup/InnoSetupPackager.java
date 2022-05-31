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
package org.apache.netbeans.nbpackage.innosetup;

import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Stream;
import org.apache.netbeans.nbpackage.ExecutionContext;
import org.apache.netbeans.nbpackage.Option;
import org.apache.netbeans.nbpackage.Packager;

/**
 * Packager for Windows .exe installer using Inno Setup.
 */
public class InnoSetupPackager implements Packager {

    static final ResourceBundle MESSAGES
            = ResourceBundle.getBundle(InnoSetupPackager.class.getPackageName() + ".Messages");

    /**
     * Path to InnoSetup compiler executable. Or to Linux script to invoke via
     * Wine.
     */
    static final Option<Path> TOOL_PATH
            = Option.ofPath("package.innosetup.tool", "",
                    MESSAGES.getString("option.innosetuptool.description"));

    /**
     * InnoSetup App ID.
     */
    static final Option<String> APPID
            = Option.ofString("package.innosetup.appid", "",
                    MESSAGES.getString("option.innosetupappid.description"));

    /**
     * Path to icon file (*.ico).
     */
    static final Option<Path> ICON_PATH
            = Option.ofPath("package.innosetup.icon", "",
                    MESSAGES.getString("option.innosetupicon.description"));

    /**
     * Path to optional license file (*.txt or *.rtf) to display during
     * installation.
     */
    static final Option<Path> LICENSE_PATH
            = Option.ofPath("package.innosetup.license", "",
                    MESSAGES.getString("option.innosetuplicense.description"));

    /**
     * Path to alternative InnoSetup template.
     */
    static final Option<Path> ISS_TEMPLATE_PATH
            = Option.ofPath("package.innosetup.template", "",
                    MESSAGES.getString("option.innosetuptemplate.description"));

    private static final List<Option<?>> INNOSETUP_OPTIONS
            = List.of(TOOL_PATH, APPID, ICON_PATH,
                    LICENSE_PATH, ISS_TEMPLATE_PATH);

    @Override
    public Task createTask(ExecutionContext context) {
        return new InnoSetupTask(context);
    }

    @Override
    public String name() {
        return "windows-innosetup";
    }

    @Override
    public Stream<Option<?>> options() {
        return INNOSETUP_OPTIONS.stream();
    }

}
