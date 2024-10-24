#!/bin/bash

# Path to the target/dependency directory
dependency_dir="target/dependency"

# Path to the existing MANIFEST.MF file
manifest_file="META-INF/MANIFEST.MF"

# Check if the dependency directory exists
if [ ! -d "$dependency_dir" ]; then
    echo "Error: $dependency_dir directory not found."
    exit 1
fi

# Check if the MANIFEST.MF file exists
if [ ! -f "$manifest_file" ]; then
    echo "Error: $manifest_file not found."
    exit 1
fi

# Initialize the Bundle-Classpath entry
bundle_classpath="Bundle-Classpath: target/classes/,\n"

# Loop through the JAR files in the dependency directory
for jar in "$dependency_dir"/*.jar; do
    if [ -f "$jar" ]; then
        bundle_classpath+=" $jar,\n"
    fi
done

# Remove the trailing comma and newline from the last line
bundle_classpath="${bundle_classpath%,\\n}"

# Replace the existing Bundle-Classpath line(s) with the new computed value
awk -v new_bundle_classpath="$bundle_classpath" '
    /^Bundle-Classpath:/ {
        print new_bundle_classpath
        classpath_printed = 1
        next
    }
    !classpath_printed {
        print
    }
' "$manifest_file" > temp.MF && mv temp.MF "$manifest_file"

echo "Updated $manifest_file with the new Bundle-Classpath entry."
