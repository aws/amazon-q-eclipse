// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.telemetry.generator;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import software.aws.toolkits.telemetry.generator.model.MetricSchema;
import software.aws.toolkits.telemetry.generator.model.TelemetryMetricType;
import software.aws.toolkits.telemetry.generator.model.TelemetrySchema;
import software.aws.toolkits.telemetry.generator.util.FileWriter;
import software.aws.toolkits.telemetry.generator.util.MetricDatumBuilder;
import software.aws.toolkits.telemetry.generator.util.ParsingUtils;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class TelemetryGenerator {

    private static final String PACKAGE_NAME = "software.aws.toolkits.telemetry";

    private TelemetryGenerator() {
        // Prevent instantiation
    }

    public static void generateTelemetry(final File inputDirectory, final File outputDirectory) throws IOException {
        TelemetrySchema telemetry = TelemetryParser.parseFiles(inputDirectory);
        outputDirectory.mkdirs();

        TypeSpec.Builder telemetryDefinitions = TypeSpec.classBuilder("TelemetryDefinitions")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
                        .addMember("value", "{$S, $S}", "unused", "MemberVisibilityCanBePrivate")
                        .build());

        generateTelemetryDefinitions(telemetry.types(), telemetryDefinitions, outputDirectory);
        generateTelemetryMetrics(telemetry.metrics(), outputDirectory);

    }

    private static void generateTelemetryDefinitions(final List<TelemetryMetricType> types, final TypeSpec.Builder telemetryDefinitions, final File outputDirectory) {
        for (TelemetryMetricType type : types) {
            if (type.allowedValues() != null && !type.allowedValues().isEmpty()) {
                generateTelemetryDefinition(type, telemetryDefinitions);
            }
        }
        FileWriter.writeClassToFile(telemetryDefinitions.build(), outputDirectory, PACKAGE_NAME);
    }

    private static void generateTelemetryDefinition(final TelemetryMetricType type, final TypeSpec.Builder telemetryDefinitions) {
        String enumTypeName = ParsingUtils.toTypeFormat(type.name());
        TypeSpec.Builder enumBuilder = TypeSpec.enumBuilder(enumTypeName)
                .addModifiers(Modifier.PUBLIC)
                .addField(String.class, "value", Modifier.PRIVATE, Modifier.FINAL)
                .addMethod(MethodSpec.constructorBuilder()
                        .addParameter(String.class, "value")
                        .addStatement("this.value = value")
                        .build())
                .addMethod(MethodSpec.methodBuilder("toString")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(String.class)
                        .addStatement("return value")
                        .build())
                .addJavadoc(type.description());

        for (Object enumValue : type.allowedValues()) {
            String enumName = ParsingUtils.toEnumConstantFormat(enumValue.toString());
            enumBuilder.addEnumConstant(enumName, TypeSpec.anonymousClassBuilder("$S", enumValue.toString()).build());
        }

        enumBuilder.addEnumConstant("UNKNOWN", TypeSpec.anonymousClassBuilder("$S", "unknown").build());

        enumBuilder.addMethod(MethodSpec.methodBuilder("from")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(ClassName.get("", enumTypeName))
                        .addParameter(String.class, "type")
                        .addStatement("for ($L value : values()) { if (value.value.equals(type)) return value; }", enumTypeName)
                        .addStatement("return UNKNOWN")
                        .build())
                .build();

        telemetryDefinitions.addType(enumBuilder.build());
    }

    private static void generateTelemetryMetrics(final List<MetricSchema> metrics, final File outputDirectory) {
        metrics.stream()
                .collect(Collectors.groupingBy(MetricSchema::getNamespace))
                .forEach((namespace, namespaceMetrics) -> generateNamespace(namespace, namespaceMetrics, outputDirectory));
    }

    private static void generateNamespace(final String namespace, final List<MetricSchema> metrics, final File outputDirectory) {
        String namespaceClassName = ParsingUtils.toTypeFormat(namespace) + "Telemetry";
        TypeSpec.Builder namespaceBuilder = TypeSpec.classBuilder(namespaceClassName)
                .addModifiers(Modifier.PUBLIC);

        metrics.stream()
                .sorted(Comparator.comparing(MetricSchema::name))
                .forEach(metric -> generateMetricDatumBuilder(metric, namespaceBuilder));

        FileWriter.writeClassToFile(namespaceBuilder.build(), outputDirectory, PACKAGE_NAME);
    }

    private static void generateMetricDatumBuilder(final MetricSchema metric, final TypeSpec.Builder namespaceBuilder) {
        String builderClassName = ParsingUtils.toTypeFormat(metric.name().split("_")[1]) + "EventBuilder";
        TypeSpec.Builder builderClassBuilder = MetricDatumBuilder.generateBuilder(metric, builderClassName);
        namespaceBuilder.addType(builderClassBuilder.build());

        MethodSpec.Builder factoryMethodBuilder = MethodSpec.methodBuilder(builderClassName.substring(0, builderClassName.length() - "Builder".length()))
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassName.get(namespaceBuilder.build().name, builderClassName));

        factoryMethodBuilder.addCode("return new $T();\n", ClassName.get(namespaceBuilder.build().name, builderClassName));
        namespaceBuilder.addMethod(factoryMethodBuilder.build());
    }

    public static void main(String[] args) {
        try {
            TelemetryGenerator.generateTelemetry(new File("/Users/breedloj/workspace/amazon-q-eclipse/plugin/codegen-resources"), new File("/Users/breedloj/Desktop/telem"));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
