#! /bin/bash
version=$(./gradlew -q printVersion)
echo "Checking that release tag $version does not exist"
git tag -l | grep $version
[ $? == 1 ] || exit 1
