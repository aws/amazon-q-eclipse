// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.telemetry.generator;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;

@Mojo(name = "generate-telemetry", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class TelemetryGeneratorMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.basedir}/codegen-resources", property = "definitionsDirectory", required = true)
    private File definitionsDirectory;

    @Parameter(defaultValue = "${project.build.directory}/generated-sources", property = "outputDirectory", required = true)
    private File outputDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            TelemetryGenerator.generateTelemetry(definitionsDirectory, outputDirectory);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to generate telemetry classes", e);
        }
    }
}