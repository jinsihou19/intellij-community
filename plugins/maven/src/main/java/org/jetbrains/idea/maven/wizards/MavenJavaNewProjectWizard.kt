// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.idea.maven.wizards

import com.intellij.ide.projectWizard.NewProjectWizardCollector.BuildSystem.logAddSampleCodeChanged
import com.intellij.ide.projectWizard.NewProjectWizardCollector.BuildSystem.logAddSampleOnboardingTipsChangedEvent
import com.intellij.ide.projectWizard.NewProjectWizardConstants.BuildSystem.MAVEN
import com.intellij.ide.projectWizard.generators.AssetsNewProjectWizardStep
import com.intellij.ide.projectWizard.generators.BuildSystemJavaNewProjectWizard
import com.intellij.ide.projectWizard.generators.BuildSystemJavaNewProjectWizardData
import com.intellij.ide.projectWizard.generators.JavaNewProjectWizard
import com.intellij.ide.starters.local.StandardAssetsProvider
import com.intellij.ide.wizard.NewProjectWizardBaseData.Companion.name
import com.intellij.ide.wizard.NewProjectWizardBaseData.Companion.path
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep.Companion.ADD_SAMPLE_CODE_PROPERTY_NAME
import com.intellij.ide.wizard.chain
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl
import com.intellij.openapi.observable.util.bindBooleanStorage
import com.intellij.openapi.project.Project
import com.intellij.ui.UIBundle
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.*
import org.jetbrains.idea.maven.model.MavenId
import org.jetbrains.idea.maven.project.MavenProjectsManager

class MavenJavaNewProjectWizard : BuildSystemJavaNewProjectWizard {

  override val name = MAVEN

  override val ordinal = 100

  override fun createStep(parent: JavaNewProjectWizard.Step) = Step(parent).chain(::AssetsStep)

  class Step(parent: JavaNewProjectWizard.Step) :
    MavenNewProjectWizardStep<JavaNewProjectWizard.Step>(parent),
    BuildSystemJavaNewProjectWizardData by parent {

    private val addSampleCodeProperty = propertyGraph.property(true)
      .bindBooleanStorage(ADD_SAMPLE_CODE_PROPERTY_NAME)
    private val generateOnboardingTipsProperty = propertyGraph.property(AssetsNewProjectWizardStep.proposeToGenerateOnboardingTipsByDefault())
      .bindBooleanStorage(NewProjectWizardStep.GENERATE_ONBOARDING_TIPS_NAME)

    var addSampleCode by addSampleCodeProperty
    val generateOnboardingTips by generateOnboardingTipsProperty

    override fun setupSettingsUI(builder: Panel) {
      super.setupSettingsUI(builder)
      lateinit var addSampleCode: Cell<JBCheckBox>
      builder.row {
        addSampleCode = checkBox(UIBundle.message("label.project.wizard.new.project.add.sample.code"))
          .bindSelected(addSampleCodeProperty)
          .whenStateChangedFromUi { logAddSampleCodeChanged(it) }
      }.topGap(TopGap.SMALL)
      builder.indent {
        row {
          checkBox(UIBundle.message("label.project.wizard.new.project.generate.onboarding.tips"))
            .bindSelected(generateOnboardingTipsProperty)
            .whenStateChangedFromUi { logAddSampleOnboardingTipsChangedEvent(it) }
        }
      }.enabledIf(addSampleCode.selected)
    }

    override fun setupProject(project: Project) {
      super.setupProject(project)

      val builder = InternalMavenModuleBuilder().apply {
        moduleJdk = sdk
        name = parentStep.name
        contentEntryPath = "${parentStep.path}/${parentStep.name}"

        parentProject = parentData
        aggregatorProject = parentData
        projectId = MavenId(groupId, artifactId, version)
        isInheritGroupId = parentData?.mavenId?.groupId == groupId
        isInheritVersion = parentData?.mavenId?.version == version
      }

      ExternalProjectsManagerImpl.setupCreatedProject(project)
      MavenProjectsManager.setupCreatedMavenProject(project)
      project.putUserData(ExternalSystemDataKeys.NEWLY_CREATED_PROJECT, true)

      builder.commit(project)
    }
  }

  private class AssetsStep(private val parent: Step) : AssetsNewProjectWizardStep(parent) {
    override fun setupAssets(project: Project) {
      outputDirectory = "$path/$name"
      addAssets(StandardAssetsProvider().getMavenIgnoreAssets())
      if (parent.addSampleCode) {
        withJavaSampleCodeAsset("src/main/java", parent.groupId, parent.generateOnboardingTips)
      }
    }

    override fun setupProject(project: Project) {
      super.setupProject(project)
      if (parent.generateOnboardingTips) {
        prepareTipsInEditor(project)
      }
    }
  }
}