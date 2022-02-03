#! /bin/bash
version=$(./gradlew -q printVersion)
echo "Creating release tag $version"
git tag $version
git push --tags
