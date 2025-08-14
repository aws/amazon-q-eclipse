#!/bin/bash

if [ -z "$1" ]; then
    echo "Usage: ./update-version.sh [new-version]"
    echo "Example: ./update-version.sh 2.4.0"
    exit 1
fi

NEW_VERSION=$1

# Validate version format (x.x.x)
if ! [[ $NEW_VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "Error: Version must be in format x.x.x (e.g., 2.4.0)"
    echo "Provided: $NEW_VERSION"
    exit 1
fi

echo "Updating to version $NEW_VERSION"

# Update root pom.xml version
sed -i "s/<version>.*-SNAPSHOT<\/version>/<version>$NEW_VERSION-SNAPSHOT<\/version>/" pom.xml

# Update parent version in all child pom.xml files
find . -name "pom.xml" -not -path "./pom.xml" -exec sed -i "s/<version>.*-SNAPSHOT<\/version>/<version>$NEW_VERSION-SNAPSHOT<\/version>/" {} \;

# Update MANIFEST.MF
sed -i "s/Bundle-Version: .*/Bundle-Version: $NEW_VERSION.qualifier/" plugin/META-INF/MANIFEST.MF

# Update feature.xml
sed -i "s/version=\".*\.qualifier\"/version=\"$NEW_VERSION.qualifier\"/g" feature/feature.xml

# Update category.xml
sed -i "s/version=\".*\.qualifier\"/version=\"$NEW_VERSION.qualifier\"/g" updatesite/category.xml
sed -i "s/_.*\.qualifier\.jar/_$NEW_VERSION.qualifier.jar/g" updatesite/category.xml

echo "Version updated to $NEW_VERSION"
echo "Run 'mvn clean install' and 'mvn clean package' to build with new version"