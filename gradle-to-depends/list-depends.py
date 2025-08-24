
from tempfile import TemporaryDirectory
import os
import sys
import sqlite3
import csv
import shutil
from pathlib import Path

def convert_gradle_to_maven(gradle_cache_dir, maven_repo_dir):
    """
    Converts a Gradle cache directory to a Maven repository directory.
    """
    gradle_cache_dir = Path(gradle_cache_dir)
    maven_repo_dir = Path(maven_repo_dir)

    if not gradle_cache_dir.is_dir():
        print(f"Source directory not found: {gradle_cache_dir}")
        return

    # Create the destination directory if it doesn't exist
    maven_repo_dir.mkdir(parents=True, exist_ok=True)
    print(f"Created destination Maven repository at: {maven_repo_dir}")

    # Walk through the Gradle cache directory
    # The structure is typically: group/artifact/version/...
    for root, dirs, files in os.walk(gradle_cache_dir):
        root_path = Path(root)

        # We need to find directories that contain the artifact files
        # A simple heuristic is to check if it's a directory with JAR/POM files
        if any(f.endswith(('.jar', '.pom')) for f in files):
            try:
                # The path from the root of the cache is what we need to parse
                relative_path = root_path.relative_to(gradle_cache_dir)

                # Split the path into parts to get group, artifact, and version
                parts = relative_path.parts
                if len(parts) >= 3:
                    group_id = parts[0]
                    artifact_id = parts[1]
                    version = parts[2]

                    # Create the corresponding Maven directory structure
                    # Example: com.google.guava -> com/google/guava
                    maven_group_dir = maven_repo_dir / Path(group_id.replace('.', os.sep))
                    maven_artifact_dir = maven_group_dir / artifact_id
                    maven_version_dir = maven_artifact_dir / version

                    maven_version_dir.mkdir(parents=True, exist_ok=True)

                    print(f"Processing: {group_id}:{artifact_id}:{version}")

                    # Copy the files, excluding hash files
                    for file in files:
                        if not file.endswith(('.sha1', '.md5', '.asc')):
                            src_file = root_path / file
                            dest_file = maven_version_dir / file
                            shutil.copy2(src_file, dest_file)
                            print(f"  Copied {file}")

            except Exception as e:
                print(f"Error processing {root}: {e}")


def list_local_artifacts(repo_path: Path):
    """
    Scans the local Maven repository to find all unique group and artifact IDs.

    It works by finding all Project Object Model (.pom) files, which are the
    definitive metadata file for any Maven artifact. The path to the .pom file
    contains the group, artifact, and version information.
    """
    if not repo_path.is_dir():
        print(f"❌ Error: Maven repository not found at the expected path: '{repo_path}'")
        return None

    print(f"🔍 Scanning for artifacts in '{repo_path}'...")
    # Using a set to store (group_id, artifact_id) tuples automatically handles duplicates
    unique_artifacts = set()

    # Recursively find all .pom files in the repository directory
    for pom_file in repo_path.rglob("*.pom"):
        try:
            # The directory structure is predictable:
            # .../repository/[group/id/path]/[artifact-id]/[version]/[artifact-id]-[version].pom

            # Get the path components by moving up from the pom_file
            version_dir = pom_file.parent
            artifact_dir = version_dir.parent

            artifact_id = artifact_dir.name

            # The group ID path is the part between the repo root and the artifact directory
            group_path_parts = artifact_dir.parent.relative_to(repo_path).parts
            group_id = ".".join(group_path_parts)

            # Add the unique combination to our set
            if group_id and artifact_id:
                unique_artifacts.add((group_id, artifact_id))
        except (ValueError, IndexError):
            # This handles any malformed paths that don't match the expected structure
            print(f"⚠️ Could not parse artifact from path: {pom_file}")
            continue

    # Return a sorted list for consistent output
    return sorted(list(unique_artifacts))


def setup_database(db_name, items):
    # Connect to the SQLite database (this will create the file)
    conn = sqlite3.connect(db_name)
    cursor = conn.cursor()

    cursor.execute('''DROP TABLE IF EXISTS dependencies''')

    cursor.execute('''
    CREATE TABLE dependencies (
        group_id TEXT,
        artifact_id TEXT,
        PRIMARY KEY (group_id, artifact_id)
    )
    ''')

    for row in items:
        cursor.execute('INSERT INTO dependencies (group_id, artifact_id) VALUES (?, ?)', row)

    # Commit changes and return the connection
    conn.commit()
    return conn

def find_and_print_packages(conn):
    """
    Joins the tables and prints the resulting package names.
    """
    cursor = conn.cursor()

    # SQL query to join the two tables
    sql_query = """
    SELECT distinct T2.package_name
    FROM dependencies AS T1
    INNER JOIN imported_artifacts AS T2
      ON T1.group_id = T2.group_id AND T1.artifact_id = T2.artifact_id;
    """

    cursor.execute(sql_query)

    # Fetch all matching rows
    results = cursor.fetchall()

    print("--- Matched Packages ---")
    if not results:
        print("No matching packages found.")
    else:
        # Print each package_name on a new line, ending with a comma
        for row in results:
            package_name = row[0]
            print(f"{package_name},")
    print("------------------------")


if __name__ == "__main__":
    with TemporaryDirectory() as temp_dir_path:
        gradle_cache = Path(sys.argv[1]) / "caches/modules-2/files-2.1"
        convert_gradle_to_maven(gradle_cache, temp_dir_path)
        artifacts = list_local_artifacts(Path(temp_dir_path))
        conn = setup_database("artifacts.db", artifacts)
        find_and_print_packages(conn)
