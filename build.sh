#!/bin/sh

set -ex

# Uncomment to remove all packages
rm -rf prepare-artifacts/debstore && mkdir -p prepare-artifacts/debstore

# regenerate artifacts.db
(cd prepare-artifacts && mvn package && java -jar prepare-artifacts/target/prepare-artifacts-1.0-SNAPSHOT-jar-with-dependencies.jar)

cp prepare-artifacts/artifacts.db check-depends/src/main/resources/artifacts.db

# build the snap
snapcraft
