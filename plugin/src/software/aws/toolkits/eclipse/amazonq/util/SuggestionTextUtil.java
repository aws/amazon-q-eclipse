// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

public final class SuggestionTextUtil {

    private SuggestionTextUtil() {
    }

    public static String replaceSpacesWithTabs(final String input, final int tabSize) {
        StringBuilder result = new StringBuilder();
        String[] lines = input.split("\\r?\\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int numSpaces = 0;
            StringBuilder newLine = new StringBuilder();

            for (int j = 0; j < line.length(); j++) {
                char c = line.charAt(j);
                if (c == ' ') {
                    numSpaces++;
                } else {
                    newLine.append(getTabsForSpaces(numSpaces, tabSize));
                    newLine.append(c);
                    numSpaces = 0;
                }
            }

            if (i < lines.length - 1) {
                newLine.append("\n");
            }

            result.append(newLine);
        }

        return result.toString();
    }

    private static String getTabsForSpaces(final int numSpaces, final int tabSize) {
        int numTabs = numSpaces / tabSize;
        StringBuilder tabs = new StringBuilder();

        for (int i = 0; i < numTabs; i++) {
            tabs.append("\t");
        }

        int remainingSpaces = numSpaces % tabSize;
        for (int i = 0; i < remainingSpaces; i++) {
            tabs.append(" ");
        }

        return tabs.toString();
    }
}
