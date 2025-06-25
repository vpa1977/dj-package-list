export DEBIAN_MAVEN_REPO=http://localhost:8080/
export DEBIAN_SKIP_MAVEN_RULES=1
gradle --gradle-user-home .gradle-cache \
    --init-script init-all-deps.gradle.kts \
    -x test \
    :distributions-full:binDistributionZip



# select r.groupId, r.artifactId, r.version, i.package_name, i.package_version from remote_artifacts r LEFT JOIN imported_artifacts i ON r.groupId = i.group_id and r.artifactId = i.artifact_id