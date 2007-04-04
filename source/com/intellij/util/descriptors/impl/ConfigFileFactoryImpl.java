/*
 * Copyright (c) 2000-2007 JetBrains s.r.o. All Rights Reserved.
 */

package com.intellij.util.descriptors.impl;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.deployment.DeploymentItemUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.descriptors.*;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

/**
 * @author nik
 */
public class ConfigFileFactoryImpl extends ConfigFileFactory {

  public ConfigFileMetaDataProvider createMetaDataProvider(final ConfigFileMetaData... metaDatas) {
    return new ConfigFileMetaDataRegistryImpl(metaDatas);
  }

  public ConfigFileMetaDataRegistry createMetaDataRegistry() {
    return new ConfigFileMetaDataRegistryImpl();
  }

  public ConfigFileInfoSet createConfigFileInfoSet(final ConfigFileMetaDataProvider metaDataProvider) {
    return new ConfigFileInfoSetImpl(metaDataProvider);
  }

  public ConfigFileContainer createConfigFileContainer(final Project project, final ConfigFileMetaDataProvider metaDataProvider,
                                                              final ConfigFileInfoSet configuration) {
    return new ConfigFileContainerImpl(project, metaDataProvider, (ConfigFileInfoSetImpl)configuration);
  }

  public CustomConfigFileSet createCustomConfigFileSet() {
    return new CustomConfigFileSetImpl();
  }

  private static String getText(final String templateName) throws IOException {
    final FileTemplateManager templateManager = FileTemplateManager.getInstance();
    final FileTemplate template = templateManager.getJ2eeTemplate(templateName);
    if (template == null) {
      return "";
    }
    return template.getText(templateManager.getDefaultProperties());
  }

  @Nullable
  public VirtualFile createFile(Project project, String url, ConfigFileVersion version) {
    return createFileFromTemplate(project, url, version.getTemplateName());
  }

  @Nullable
  private VirtualFile createFileFromTemplate(final Project project, String url, final String templateName) {
    final LocalFileSystem fileSystem = LocalFileSystem.getInstance();
    final File file = new File(VfsUtil.urlToPath(url));
    final VirtualFile existingFile = fileSystem.refreshAndFindFileByIoFile(file);
    if (existingFile != null) {
      return existingFile;
    }
    try {
      String text = getText(templateName);
      final VirtualFile virtualFile;
      if (!FileUtil.createParentDirs(file) ||
          (virtualFile = fileSystem.refreshAndFindFileByIoFile(file.getParentFile())) == null) {
        throw new IOException(IdeBundle.message("error.message.unable.to.create.file", file.getPath()));
      }
      final VirtualFile childData = virtualFile.createChildData(this, file.getName());
      DeploymentItemUtil.setFileText(project, childData, text);
      return childData;
    }
    catch (final IOException e) {
      ApplicationManager.getApplication().invokeLater(new Runnable() {
        public void run() {
          Messages.showErrorDialog(IdeBundle.message("message.text.error.creating.deployment.descriptor", e.getLocalizedMessage()),
                                   IdeBundle.message("message.text.creating.deployment.descriptor"));
        }
      });
    }
    return null;
  }

  public ConfigFileContainer createSingleFileContainer(Project project, ConfigFileMetaData metaData) {
    final ConfigFileMetaDataProvider metaDataProvider = createMetaDataProvider(metaData);
    return createConfigFileContainer(project, metaDataProvider, createConfigFileInfoSet(metaDataProvider));
  }
}
