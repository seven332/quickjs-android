/*
 * Copyright 2019 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.quickjs.android;

import com.cloudbees.diff.ContextualPatch;
import com.cloudbees.diff.PatchException;
import org.apache.commons.io.FileUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class PatchTask extends DefaultTask {

  private List<File> from;
  private File to;
  private File patch;

  @InputFiles
  public List<File> getFrom() {
    return from;
  }

  public void setFrom(List<File> from) {
    this.from = from;
  }

  @OutputDirectory
  public File getTo() {
    return to;
  }

  public void setTo(File to) {
    this.to = to;
  }

  @InputFile
  public File getPatch() {
    return patch;
  }

  public void setPatch(File patch) {
    this.patch = patch;
  }

  @TaskAction
  public void run() throws IOException, PatchException {
    FileUtils.forceMkdir(to);
    for (File f : from) {
      FileUtils.copyFileToDirectory(f, to);
    }

    ContextualPatch.create(patch, to).patch(false);
  }
}
