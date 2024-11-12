// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.telemetry.generator.util;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import software.aws.toolkits.telemetry.generator.model.MetadataSchema;
import software.aws.toolkits.telemetry.generator.model.MetricMetadataTypes;
import software.aws.toolkits.telemetry.generator.model.MetricSchema;
import software.aws.toolkits.telemetry.generator.model.TelemetryMetricType;

import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Modifier;

public final class MetricDatumBuilder {
    private static final ClassName TELEMETRY_DEFINITIONS = ClassName.get("software.aws.toolkits.telemetry", "TelemetryDefinitions");
    private static final ClassName METRIC_DATUM = ClassName.get("software.amazon.awssdk.services.toolkittelemetry.model", "MetricDatum");
    private static final ClassName METADATA_ENTRY = ClassName.get("software.amazon.awssdk.services.toolkittelemetry.model", "MetadataEntry");
    private static final ClassName UNIT = ClassName.get("software.amazon.awssdk.services.toolkittelemetry.model", "Unit");

    private MetricDatumBuilder() {
        // Prevent instantiation
    }

    public static TypeSpec.Builder generateBuilder(final MetricSchema metric, final String builderClassName) {
        TypeSpec.Builder builderClassBuilder = TypeSpec.classBuilder(builderClassName)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

        List<MetadataSchema> metricMetadata = metric.metadata();
        metricMetadata.add(new MetadataSchema(new TelemetryMetricType("passive", "Indicates that the metric was not caused by an explicit user action.\n",
                MetricMetadataTypes.BOOLEAN, null), true));
        metricMetadata.add(new MetadataSchema(new TelemetryMetricType("createTime", "The time that the event took place.",
                MetricMetadataTypes.INSTANT, null), true));
        metricMetadata.add(new MetadataSchema(new TelemetryMetricType("value", "Value based on unit and call type.", MetricMetadataTypes.DOUBLE, null), true));

        for (MetadataSchema metadata : metricMetadata) {
            TypeName parameterType = getParameterType(metadata);
            String parameterName = ParsingUtils.toArgumentFormat(metadata.type().name());
            builderClassBuilder.addField(parameterType, parameterName, Modifier.PRIVATE);

            MethodSpec.Builder builderMethodBuilder = MethodSpec.methodBuilder(parameterName)
                    .addModifiers(Modifier.PUBLIC)
                    .addJavadoc(metadata.type().description())
                    .returns(ClassName.get("", builderClassName))
                    .addParameter(parameterType, parameterName)
                    .addStatement("this.$1N = $1N", parameterName)
                    .addStatement("return this");

            builderClassBuilder.addMethod(builderMethodBuilder.build());
        }

        MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE);
        builderClassBuilder.addMethod(constructorBuilder.build());

        MethodSpec.Builder buildMethodBuilder = MethodSpec.methodBuilder("build")
                .addModifiers(Modifier.PUBLIC)
                .returns(METRIC_DATUM)
                .addCode(generateBuildFunctionBody(metric));

        builderClassBuilder.addMethod(buildMethodBuilder.build());

        return builderClassBuilder;
    }

    private static CodeBlock generateBuildFunctionBody(final MetricSchema metric) {
        CodeBlock.Builder codeBlockBuilder = CodeBlock.builder();
        codeBlockBuilder.add("$T<MetadataEntry> metadata = new $T<$T>();\n", ClassName.get(List.class), ClassName.get(ArrayList.class), METADATA_ENTRY);
        for (MetadataSchema metadata : metric.metadata()) {
            String metadataName = ParsingUtils.toArgumentFormat(metadata.type().name());
            codeBlockBuilder.add("metadata.add(MetadataEntry.builder().key($S).value(String.valueOf($L)).build());\n", metadataName, metadataName);
        }

        codeBlockBuilder.add("\nreturn $T.builder()\n", METRIC_DATUM);
        codeBlockBuilder.add("    .metricName($S)\n", metric.name());
        codeBlockBuilder.add("    .epochTimestamp((createTime != null ? createTime : Instant.now()).toEpochMilli())\n");
        codeBlockBuilder.add("    .unit($T.$L)\n", UNIT, metric.unit() != null ? metric.unit().getType().toUpperCase() : "NONE");
        codeBlockBuilder.add("    .value(value != null ? value : 1.0)\n");
        codeBlockBuilder.add("    .passive(passive)\n");
        codeBlockBuilder.add("    .metadata(metadata)\n");
        codeBlockBuilder.add("    .build();\n");
        return codeBlockBuilder.build();
    }

    private static TypeName getParameterType(final MetadataSchema metadata) {
        return metadata.type().allowedValues() != null
                ? ClassName.get(TELEMETRY_DEFINITIONS.canonicalName(), ParsingUtils.toTypeFormat(metadata.type().name()))
                : metadata.type().type().javaType();
    }
}
