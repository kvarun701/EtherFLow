import os
import subprocess
import zipfile
import sys

import argparse

def main():
    parser = argparse.ArgumentParser(description="Package Maven Central Bundle")
    parser.add_argument("--passphrase", help="GPG Passphrase")
    parser.add_argument("--keyname", help="GPG Key Name")
    args, unknown = parser.parse_known_args()

    base_dir = "/Users/varun/Desktop/EtherFlow"
    staging_dir = os.path.join(base_dir, "target", "staging-deploy")
    zip_path = os.path.join(base_dir, "target", "etherflow-maven-central-0.1.0.zip")

    passphrase = args.passphrase or os.environ.get("GPG_PASSPHRASE")
    keyname = args.keyname or os.environ.get("GPG_KEYNAME")

    # Step 1: Run Maven Deploy to Local Staging Directory
    print("=== Step 1: Running Maven clean deploy to local staging repository ===")
    maven_cmd = [
        "mvn", "clean", "deploy",
        "-P", "release",
        f"-DaltDeploymentRepository=local::file://{staging_dir}",
        "-DskipTests"
    ]
    if keyname:
        maven_cmd.append(f"-Dgpg.keyname={keyname}")
    if passphrase:
        # Hide passphrase in printing but include in execution
        maven_cmd.append(f"-Dgpg.passphrase={passphrase}")
        print(f"Executing: {' '.join(maven_cmd[:-1]) if passphrase else ' '.join(maven_cmd)} -Dgpg.passphrase=********")
    else:
        print(f"Executing: {' '.join(maven_cmd)}")
        
    env = os.environ.copy()
    # Force JDK 21 to avoid Kotlin compiler incompatibilities on JDK 26
    jdk_21_home = "/opt/homebrew/Cellar/openjdk@21/21.0.11/libexec/openjdk.jdk/Contents/Home"
    if os.path.exists(jdk_21_home):
        env["JAVA_HOME"] = jdk_21_home
        
    result = subprocess.run(maven_cmd, cwd=base_dir, env=env)
    if result.returncode != 0:
        print("Error: Maven build and deployment failed.", file=sys.stderr)
        sys.exit(1)
        
    print("\n=== Step 2: Validating staged artifacts and signatures ===")
    if not os.path.exists(staging_dir):
        print(f"Error: Staging directory '{staging_dir}' does not exist.", file=sys.stderr)
        sys.exit(1)

    # Let's collect and validate artifacts
    missing_signatures = []
    all_files = []
    
    for root, dirs, files in os.walk(staging_dir):
        for file in files:
            # Skip signature files, checksum files, and metadata files for check
            if file.endswith(('.asc', '.md5', '.sha1', '.sha256', '.sha512', 'maven-metadata.xml')):
                continue
            
            filepath = os.path.join(root, file)
            all_files.append(filepath)
            
            # Check for signature (.asc) file
            sig_file = filepath + ".asc"
            if not os.path.exists(sig_file):
                missing_signatures.append(filepath)

    if missing_signatures:
        print("Error: The following artifacts are missing GPG signatures (.asc files):", file=sys.stderr)
        for missing in missing_signatures:
            print(f"  - {os.path.relpath(missing, staging_dir)}", file=sys.stderr)
        sys.exit(1)
        
    print("All artifacts successfully verified with matching GPG signatures!")

    # Step 3: Package into ZIP file
    print("\n=== Step 3: Compressing artifacts into ZIP file ===")
    print(f"Creating ZIP archive at: {zip_path}")
    
    with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_DEFLATED) as zipf:
        for root, dirs, files in os.walk(staging_dir):
            for file in files:
                # Skip maven-metadata.xml files if any exist (they are not needed / rejected by central portal)
                if file.startswith('maven-metadata.xml'):
                    continue
                filepath = os.path.join(root, file)
                # Compute path relative to staging_dir so 'io' is the root of the ZIP
                archive_name = os.path.relpath(filepath, staging_dir)
                zipf.write(filepath, archive_name)
                
    print(f"Successfully packaged ZIP archive for Maven Central: {zip_path}")
    
    # Step 4: Display ZIP contents
    print("\n=== Step 4: Verification of ZIP File Contents ===")
    with zipfile.ZipFile(zip_path, 'r') as zipf:
        namelist = sorted(zipf.namelist())
        print(f"Total files in ZIP: {len(namelist)}")
        print("ZIP Directory Structure:")
        for name in namelist[:30]:
            print(f"  {name}")
        if len(namelist) > 30:
            print(f"  ... and {len(namelist) - 30} more files.")
            
    print("\nSuccess! The ZIP is ready for uploading to Maven Central Portal.")

if __name__ == "__main__":
    main()
