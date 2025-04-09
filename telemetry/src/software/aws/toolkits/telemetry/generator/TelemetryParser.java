// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.telemetry.generator;

import com.fasterxml.jackson.databind.ObjectMapper;

import software.aws.toolkits.telemetry.generator.model.MetadataSchema;
import software.aws.toolkits.telemetry.generator.model.MetricSchema;
import software.aws.toolkits.telemetry.generator.model.TelemetryDefinition;
import software.aws.toolkits.telemetry.generator.model.TelemetryMetricType;
import software.aws.toolkits.telemetry.generator.model.TelemetrySchema;

import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class TelemetryParser {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // prevent instantiation
    private TelemetryParser() { }

    public static TelemetrySchema parseFiles(final File definitionsDirectory) {
        ResourceLoader resourceLoader = new ResourceLoader(definitionsDirectory);

        List<String> files = new ArrayList<>();
        files.addAll(resourceLoader.getDefinitionsFiles());

        JSONObject rawSchema = new JSONObject(new JSONTokener(resourceLoader.getSchemaFile()));
        Schema schema = SchemaLoader.load(rawSchema);
        files.forEach(f -> validate(f, schema));

        TelemetryDefinition telemetryDefinition = files.stream()
                .map(TelemetryParser::parse)
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        defs -> new TelemetryDefinition(
                                defs.stream().flatMap(d -> d.types().stream()).distinct().collect(Collectors.toList()),
                                defs.stream().flatMap(d -> d.metrics().stream()).distinct().collect(Collectors.toList())
                        )
                ));

        Map<String, TelemetryMetricType> metadataTypes = telemetryDefinition.types().stream()
                .collect(Collectors.toMap(TelemetryMetricType::name, t -> t));

        List<MetricSchema> resolvedMetricTypes = telemetryDefinition.metrics().stream()
                .map(m -> new MetricSchema(
                        m.name(),
                        m.description(),
                        m.unit(),
                        m.metadata().stream()
                                .map(md -> new MetadataSchema(
                                        metadataTypes.get(md.type()),
                                        md.required()
                                ))
                                .collect(Collectors.toList()),
                        m.passive()
                ))
                .collect(Collectors.toList());

        return new TelemetrySchema(
                telemetryDefinition.types(),
                resolvedMetricTypes
        );
    }

    private static void validate(final String fileContents, final Schema schema) {
        try {
            schema.validate(new JSONObject(fileContents));
        } catch (Exception e) {
            System.err.println("Schema validation failed: " + e);
            throw e;
        }
    }

    private static TelemetryDefinition parse(final String input) {
        try {
            return MAPPER.readValue(input, TelemetryDefinition.class);
        } catch (IOException e) {
            System.err.println("Error while parsing: " + e);
            throw new RuntimeException(e);
        }
    }

}
