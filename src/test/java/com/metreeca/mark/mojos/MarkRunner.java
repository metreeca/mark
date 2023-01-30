/*
 * Copyright © 2019-2023 Metreeca srl
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

package com.metreeca.mark.mojos;

import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.console.ConsoleLogger;

final class MarkRunner {

    static void execute(final MarkMojo mojo) {
        try {

            final MavenProject project=new MavenProject();

            project.setGroupId("com.metreeca");
            project.setArtifactId("metreeca-sample");
            project.setVersion("1.2.3");

            project.setName("Metreeca/Sample");
            project.setDescription("A sample project for Metreeca/Mark");

            mojo.setProject(project);

            mojo.setSource("docs");
            mojo.setTarget("target/docs");
            mojo.setLayout("");

            mojo.setLog(new DefaultLog(new ConsoleLogger()));

            mojo.execute();

        } catch ( final MojoExecutionException|MojoFailureException e ) {

            mojo.getLog().error(e);

        }
    }

    private MarkRunner() { }

}