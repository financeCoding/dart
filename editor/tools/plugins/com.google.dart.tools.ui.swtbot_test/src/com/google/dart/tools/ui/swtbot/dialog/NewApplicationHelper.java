/*
 * Copyright 2012 Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.swtbot.dialog;

import com.google.dart.tools.ui.swtbot.DartLib;
import com.google.dart.tools.ui.swtbot.matchers.EditorWithTitle;
import com.google.dart.tools.ui.swtbot.performance.Performance;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.keyboard.Keystrokes;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;

import static org.eclipse.swtbot.eclipse.finder.waits.Conditions.waitForEditor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

public class NewApplicationHelper {

  private final SWTWorkbenchBot bot;

  public NewApplicationHelper(SWTWorkbenchBot bot) {
    this.bot = bot;
  }

  /**
   * Drive the "New Application..." dialog to create a new application with the specified name
   * 
   * @param appName the application name
   * @return the new application
   */
  public DartLib create(String appName) {

    // Open wizard
    bot.menu("File").menu("New Application...").click();
    SWTBotShell shell = bot.shell("New Dart Application");
    shell.activate();

    // Assert content
    SWTBotText appNameField = bot.text();
    SWTBotText appDirField = bot.textWithLabel("Directory: ");
    assertEquals("", appNameField.getText());
    assertTrue(appDirField.getText().length() > 0);

    // Ensure that the directory to be created does not exist
    DartLib lib = new DartLib(new File(appDirField.getText(), appName), appName);
    lib.deleteDir();

    // Enter name of new app
    appNameField.typeText(appName);

    // Press Enter and wait for the operation to complete
    shell.pressShortcut(Keystrokes.LF);
    lib.logFullAnalysisTime();
    EditorWithTitle matcher = new EditorWithTitle(lib.dartFile.getName());
    Performance.NEW_APP.log(bot, waitForEditor(matcher), appName);
    lib.editor = bot.editor(matcher).toTextEditor();
    return lib;
  }

}
