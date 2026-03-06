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
package org.apache.netbeans.nbpackage.nsis;

import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Stream;
import org.apache.netbeans.nbpackage.ExecutionContext;
import org.apache.netbeans.nbpackage.Option;
import org.apache.netbeans.nbpackage.Packager;
import org.apache.netbeans.nbpackage.Template;

/**
 * Packager for Windows .exe installer using Nsis.
 */
public class NsisPackager implements Packager {

    static final ResourceBundle MESSAGES
            = ResourceBundle.getBundle(NsisPackager.class.getPackageName() + ".Messages");
    
    /**
     * InnoSetup App ID.
     */
    static final Option<String> APPID
            = Option.ofString("package.innosetup.appid", "",
                    MESSAGES.getString("option.appid.help"));

    /**
     * Path to icon file (*.ico).
     */
    static final Option<Path> ICON_PATH
            = Option.ofPath("package.innosetup.icon", "",
                    MESSAGES.getString("option.icon.help"));

    /**
     * Path to optional license file (*.txt or *.rtf) to display during
     * installation.
     */
    static final Option<Path> LICENSE_PATH
            = Option.ofPath("package.innosetup.license", "",
                    MESSAGES.getString("option.license.help"));

    /**
     * Path to alternative InnoSetup template.
     */
    static final Option<Path> NSH_TEMPLATE_PATH
            = Option.ofPath("package.innosetup.template", "",
                    MESSAGES.getString("option.template.help"));
    
    /**
     * ISS file template.
     */
    static final Template NSH_TEMPLATE
            = Template.of(NSH_TEMPLATE_PATH, "windows.nsh.template",
                    () -> NsisPackager.class.getResourceAsStream("windows.nsh.template"));

    private static final List<Option<?>> NSIS_OPTIONS
            = List.of(APPID, ICON_PATH,
                    LICENSE_PATH, NSH_TEMPLATE_PATH);
    
    private static final List<Template> NSIS_TEMPLATES
            = List.of(NSH_TEMPLATE);

    @Override
    public Task createTask(ExecutionContext context) {
        return new NsisTask(context);
    }

    @Override
    public String name() {
        return "windows-nsis";
    }

    @Override
    public Stream<Option<?>> options() {
        return NSIS_OPTIONS.stream();
    }

    @Override
    public Stream<Template> templates() {
        return NSIS_TEMPLATES.stream();
    }
    
}
