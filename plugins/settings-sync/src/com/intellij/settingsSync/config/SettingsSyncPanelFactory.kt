package com.intellij.settingsSync.config

import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.components.SettingsCategory
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.settingsSync.SettingsSyncBundle.message
import com.intellij.settingsSync.SettingsSyncSettings
import com.intellij.ui.CheckBoxList
import com.intellij.ui.CheckBoxListListener
import com.intellij.ui.SeparatorComponent
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.ThreeStateCheckBox
import com.intellij.util.ui.ThreeStateCheckBox.State
import org.jetbrains.annotations.Nls
import java.awt.BorderLayout
import javax.swing.BoxLayout
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel

internal object SettingsSyncPanelFactory {
  fun createPanel(syncLabel: @Nls String, loggedInAndEnabled: ComponentPredicate): DialogPanel {
    return panel {
      row {
        label(syncLabel)
      }

      SettingsCategoryDescriptor.listAll().forEach { descriptor ->
        indent {
          if (descriptor.secondaryGroup == null) {
            row {
              checkBox(
                descriptor.name
              )
                .bindSelected({ descriptor.isSynchronized },
                              { descriptor.isSynchronized = it })
                .onReset { descriptor.reset() }
                .onApply { descriptor.apply() }
                .onIsModified { descriptor.isModified() }
              comment(descriptor.description)
            }
          }
          else {
            lateinit var topCheckBox: ThreeStateCheckBox
            row {
              topCheckBox = ThreeStateCheckBox(descriptor.name)
              topCheckBox.isThirdStateEnabled = false
              cell(topCheckBox)
                .onReset {
                  descriptor.reset()
                  topCheckBox.state = getGroupState(descriptor)
                }
                .onApply {
                  descriptor.isSynchronized = topCheckBox.state != State.NOT_SELECTED
                  descriptor.apply()
                }
                .onIsModified { descriptor.isModified() }
              val c = comment(descriptor.description).visible(!descriptor.description.isEmpty())
              val subcategoryLink = configureLink(descriptor.secondaryGroup, c.component.font.size2D) {
                topCheckBox.state = getGroupState(descriptor)
                descriptor.isSynchronized = topCheckBox.state != State.NOT_SELECTED
              }
              cell(subcategoryLink)
                .visible(descriptor.secondaryGroup.getDescriptors().size > 1 || !descriptor.secondaryGroup.isComplete())
              topCheckBox.addActionListener {
                descriptor.isSynchronized = topCheckBox.state != State.NOT_SELECTED
                descriptor.secondaryGroup.getDescriptors().forEach {
                  it.isSelected = descriptor.isSynchronized
                }
                subcategoryLink.isEnabled = descriptor.secondaryGroup.isComplete() || descriptor.isSynchronized
              }
            }
            if (descriptor.category == SettingsCategory.PLUGINS) {
              indent {
                row {
                  checkBox(message("settings.cross.ide.sync.plugins.checkbox"))
                    .visibleIf(loggedInAndEnabled)
                    .enabledIf(object : ComponentPredicate() {
                      private fun isSelected() = topCheckBox.state != State.NOT_SELECTED

                      override fun invoke(): Boolean = isSelected()

                      override fun addListener(listener: (Boolean) -> Unit) {
                        topCheckBox.addPropertyChangeListener { listener(isSelected()) }
                      }
                    })
                    .bindSelected(SettingsSyncSettings.getInstance()::syncPluginsAcrossIdes)
                    .gap(RightGap.SMALL)
                  contextHelp(
                    message("settings.cross.ide.sync.plugins.checkbox.description", ApplicationNamesInfo.getInstance().fullProductName))
                }
              }
            }
          }
        }
      }
    }
  }

  private fun getGroupState(descriptor: SettingsCategoryDescriptor): State {
    val group = descriptor.secondaryGroup
    if (group == null) {
      return if (descriptor.isSynchronized) State.SELECTED else State.NOT_SELECTED
    }
    if (!group.isComplete() && !descriptor.isSynchronized) return State.NOT_SELECTED
    val descriptors = group.getDescriptors()
    if (descriptors.isEmpty()) return State.NOT_SELECTED
    val isFirstSelected = descriptors.first().isSelected
    descriptors.forEach {
      if (it.isSelected != isFirstSelected) return State.DONT_CARE
    }
    return when {
      isFirstSelected -> State.SELECTED
      group.isComplete() -> State.NOT_SELECTED
      else -> State.DONT_CARE
    }
  }

  private fun configureLink(group: SettingsSyncSubcategoryGroup,
                            fontSize: Float,
                            onCheckBoxChange: () -> Unit): JComponent {
    val actionLink = ActionLink(message("subcategory.config.link")) {}
    actionLink.withFont(actionLink.font.deriveFont(fontSize))
    actionLink.addActionListener {
      showSubcategories(actionLink, group.getDescriptors(), onCheckBoxChange)
    }
    actionLink.setDropDownLinkIcon()
    return actionLink
  }

  private fun showSubcategories(owner: JComponent,
                                descriptors: List<SettingsSyncSubcategoryDescriptor>,
                                onCheckBoxChange: () -> Unit) {
    val panel = JPanel(BorderLayout())
    panel.border = JBUI.Borders.empty()
    val checkboxList = PluginsCheckboxList(descriptors) { i: Int, isSelected: Boolean ->
      descriptors[i].isSelected = isSelected
      onCheckBoxChange()
    }
    descriptors.forEach { checkboxList.addItem(it, it.name, it.isSelected) }
    val scrollPane = JBScrollPane(checkboxList)
    panel.add(scrollPane, BorderLayout.CENTER)
    scrollPane.border = JBUI.Borders.empty(5)
    val chooserBuilder = JBPopupFactory.getInstance().createComponentPopupBuilder(panel, checkboxList)
    chooserBuilder.createPopup().showUnderneathOf(owner)
  }

  private class PluginsCheckboxList(
    val descriptors: List<SettingsSyncSubcategoryDescriptor>,
    listener: CheckBoxListListener) : CheckBoxList<SettingsSyncSubcategoryDescriptor>(listener) {

    override fun adjustRendering(rootComponent: JComponent,
                                 checkBox: JCheckBox?,
                                 index: Int,
                                 selected: Boolean,
                                 hasFocus: Boolean): JComponent {
      if (descriptors[index].isSubGroupEnd) {
        val itemWrapper = JPanel()
        itemWrapper.layout = BoxLayout(itemWrapper, BoxLayout.Y_AXIS)
        itemWrapper.add(rootComponent)
        itemWrapper.add(SeparatorComponent(5, JBUI.CurrentTheme.Popup.separatorColor(), null))
        return itemWrapper
      }
      return rootComponent
    }
  }
}