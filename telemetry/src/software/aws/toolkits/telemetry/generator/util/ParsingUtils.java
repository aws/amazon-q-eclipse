// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.telemetry.generator.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * Utility class for string manipulations.
 */
public final class ParsingUtils {

    // prevent instantiation
    private ParsingUtils() { }

    /**
     * Filters invalid characters (periods and spaces) from the input string.
     *
     * @param str the input string
     * @return the filtered string
     */
    public static String filterInvalidCharacters(final String str) {
        return str.replace(".", "_").replace(" ", "_");
    }

    /**
     * Converts the input string to an enum constant format (e.g., "dotnetcore2.1" -> "DOTNETCORE_2_1").
     *
     * @param str the input string
     * @return the enum constant format string
     */
    public static String toEnumConstantFormat(final String str) {
        StringBuilder sb = new StringBuilder();
        String[] words = splitWords(filterInvalidCharacters(str));
        for (int i = 0; i < words.length; i++) {
            if (i > 0 && (isCapitalized(words[i]) || words[i].contains("-"))) {
                sb.append("_");
            }
            sb.append(words[i].toUpperCase().replace("-", "_"));
        }
        return StringUtils.replace(sb.toString(), "__", "_");
    }

    /**
     * Converts the input string to a type format (e.g., "dotnetcore2.1" -> "DotnetCore21").
     *
     * @param str the input string
     * @return the type format string
     */
    public static String toTypeFormat(final String str) {
        StringBuilder sb = new StringBuilder();
        String[] words = splitWords(filterInvalidCharacters(str));
        for (String word : words) {
            sb.append(capitalize(word));
        }
        return sb.toString();
    }

    /**
     * Converts the input string to an argument format (e.g., "DOTNETCORE_2_1" -> "dotnetcore2.1").
     *
     * @param str the input string
     * @return the argument format string
     */
    public static String toArgumentFormat(final String str) {
        StringBuilder sb = new StringBuilder();
        String[] words = splitWords(filterInvalidCharacters(str));
        for (int i = 0; i < words.length; i++) {
            sb.append(i == 0 ? decapitalize(words[i]) : capitalize(words[i]));
        }
        return sb.toString();
    }

    private static String[] splitWords(final String str) {
        String[] words = StringUtils.splitByCharacterTypeCamelCase(str);
        List<String> result = new ArrayList<>();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.add(word);
            }
        }
        return result.toArray(new String[0]);
    }

    private static boolean isCapitalized(final String word) {
        return !word.isEmpty() && Character.isUpperCase(word.charAt(0));
    }

    private static String capitalize(final String word) {
        if (word.isEmpty()) {
            return word;
        }
        return Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }

    private static String decapitalize(final String str) {
        if (str.isEmpty()) {
            return str;
        }
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }
}
