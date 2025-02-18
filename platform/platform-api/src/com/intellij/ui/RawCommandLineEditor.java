/*
 * Copyright 2000-2019 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.ui;

import com.intellij.execution.ui.FragmentWrapper;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.components.fields.ExpandableTextField;
import com.intellij.ui.dsl.builder.DslComponentProperty;
import com.intellij.ui.dsl.builder.VerticalComponentGap;
import com.intellij.util.Function;
import com.intellij.util.execution.ParametersListUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.Document;
import java.awt.*;
import java.util.List;

import static com.intellij.ui.dsl.gridLayout.GapsKt.toGaps;

public class RawCommandLineEditor extends JPanel implements TextAccessor, FragmentWrapper {
  private final ExpandableTextField myEditor;
  private String myDialogCaption = "";

  public RawCommandLineEditor() {
    this(ParametersListUtil.DEFAULT_LINE_PARSER, ParametersListUtil.DEFAULT_LINE_JOINER);
  }

  public RawCommandLineEditor(final Function<? super String, ? extends List<String>> lineParser, final Function<? super List<String>, String> lineJoiner) {
    super(new BorderLayout());
    myEditor = new ExpandableTextField(lineParser, lineJoiner);
    add(myEditor, BorderLayout.CENTER);
    setDescriptor(null);
    putClientProperty(DslComponentProperty.VERTICAL_COMPONENT_GAP, new VerticalComponentGap(true, true));
    putClientProperty(DslComponentProperty.INTERACTIVE_COMPONENT, myEditor);
    putClientProperty(DslComponentProperty.VISUAL_PADDINGS, toGaps(myEditor.getInsets()));
  }

  public void setDescriptor(FileChooserDescriptor descriptor) {
    setDescriptor(descriptor, true);
  }
  
  public void setDescriptor(FileChooserDescriptor descriptor, boolean insertSystemDependentPaths) {
    InsertPathAction.addTo(myEditor, descriptor, insertSystemDependentPaths);
  }

  /**
   * @deprecated Won't be used anymore as dialog is replaced with lightweight popup
   */
  @Deprecated(forRemoval = true)
  public String getDialogCaption() {
    return myDialogCaption;
  }

  /**
   * @deprecated Won't be used anymore as dialog is replaced with lightweight popup
   */
  @Deprecated
  public void setDialogCaption(String dialogCaption) {
    myDialogCaption = dialogCaption != null ? dialogCaption : "";
  }

  @Override
  public void setText(@Nullable String text) {
    myEditor.setText(text);
  }

  @Override
  public String getText() {
    return StringUtil.notNullize(myEditor.getText());
  }

  public JTextField getTextField() {
    return myEditor;
  }

  public ExpandableTextField getEditorField() {
    return myEditor;
  }

  public Document getDocument() {
    return myEditor.getDocument();
  }

  public void attachLabel(JLabel label) {
    label.setLabelFor(myEditor);
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    myEditor.setEnabled(enabled);
  }

  public @NotNull RawCommandLineEditor withMonospaced(boolean monospaced) {
    myEditor.setMonospaced(monospaced);
    return this;
  }

  @Override
  public JComponent getComponentToRegister() {
    return getEditorField();
  }
}
