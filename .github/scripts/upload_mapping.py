import os
import sys
import re
import subprocess
import zipfile
import glob

import requests

def get_version_info(gradle_file_path):
    version_name = None
    version_code = None
    try:
        with open(gradle_file_path, 'r') as f:
            content = f.read()
            # versionName = "2.0.0.0_preview"
            vn_match = re.search(r'versionName\s*=\s*"([^"]+)"', content)
            if vn_match:
                version_name = vn_match.group(1)
            
            # versionCode = 20000
            vc_match = re.search(r'versionCode\s*=\s*(\d+)', content)
            if vc_match:
                version_code = vc_match.group(1)
                
    except Exception as e:
        print(f"Failed reading gradle file: {e}")
    return version_name, version_code

def download_bugly_tool(dest_dir):
    url = "https://bugly.qq.com/v2/sdk?id=d796e9d7-0423-422f-9eb9-63b6e16ef4f9"
    print(f"Downloading Bugly tool from {url}...")
    
    zip_path = os.path.join(dest_dir, "bugly_tool.zip")
    
    try:
        response = requests.get(url, stream=True)
        response.raise_for_status()
        with open(zip_path, 'wb') as f:
            for chunk in response.iter_content(chunk_size=8192):
                f.write(chunk)
        print("Download complete.")
        
        print("Extracting Bugly tool...")
        with zipfile.ZipFile(zip_path, 'r') as zip_ref:
            zip_ref.extractall(dest_dir)
            
        # Find the jar file
        # The zip usually contains a folder like 'buglyqq-upload-symbol-v3.x.x'
        # We search for any jar that looks like the tool
        jar_files = glob.glob(os.path.join(dest_dir, "**", "buglyqq-upload-symbol*.jar"), recursive=True)
        
        if not jar_files:
             # Try another common name pattern just in case
             jar_files = glob.glob(os.path.join(dest_dir, "**", "bugly-qq-upload-symbol*.jar"), recursive=True)

        if jar_files:
            return jar_files[0]
        else:
            print("Could not find Bugly JAR file in extracted content.")
            return None
            
    except Exception as e:
        print(f"Failed to download or extract Bugly tool: {e}")
        return None

def main():
    print("Starting Bugly Mapping Upload...")
    
    bugly_app_id = os.environ.get('BUGLY_APP_ID')
    bugly_app_key = os.environ.get('BUGLY_APP_KEY')

    if not bugly_app_id or not bugly_app_key:
        print("Missing required environment variables: BUGLY_APP_ID, BUGLY_APP_KEY")
        sys.exit(1)

    # 1. Extract Version Info
    gradle_path = 'app/build.gradle.kts'
    if not os.path.exists(gradle_path):
         print(f"File not found: {gradle_path}")
         sys.exit(1)
    
    version_name, version_code = get_version_info(gradle_path)
    if not version_name:
        print("Error: Could not extract versionName from app/build.gradle.kts")
        sys.exit(1)
    
    # Default version code if not found (though it should be there)
    if not version_code:
        version_code = "1"
        
    print(f"App Version: {version_name} (Code: {version_code})")

    # 2. Locate Mapping File
    mapping_file = "app/build/outputs/mapping/normalRelease/mapping.txt"
    if not os.path.exists(mapping_file):
        print(f"Mapping file not found at default path: {mapping_file}")
        # Fallback search
        found = False
        for root, dirs, files in os.walk("app/build/outputs/mapping"):
            if "mapping.txt" in files:
                mapping_file = os.path.join(root, "mapping.txt")
                found = True
                break
        if not found:
            print("Error: Could not find any mapping.txt")
            sys.exit(1)
    
    print(f"Found mapping file at: {mapping_file}")

    # 3. Download and Setup Bugly Tool
    tool_dir = os.path.join(".github", "tools", "bugly")
    if not os.path.exists(tool_dir):
        os.makedirs(tool_dir)
        
    jar_path = download_bugly_tool(tool_dir)
    if not jar_path:
        print("Failed to setup Bugly tool.")
        sys.exit(1)
        
    print(f"Using Bugly JAR: {jar_path}")

    # 4. Run Upload Command
    # Command format: java -jar bugly.jar -appid <id> -appkey <key> -bundleid <pkg> -version <ver> -platform Android -inputMapping <file>
    # Note: We need the package name (bundle ID). 
    # Usually in app/build.gradle.kts: applicationId = "remix.myplayer"
    bundle_id = "remix.myplayer" # Default fallback
    
    # Try to extract applicationId from gradle file
    try:
        with open(gradle_path, 'r') as f:
            content = f.read()
            match = re.search(r'applicationId\s*=\s*"([^"]+)"', content)
            if match:
                bundle_id = match.group(1)
    except:
        pass
        
    print(f"Bundle ID: {bundle_id}")

    cmd = [
        "java", "-jar", jar_path,
        "-appid", bugly_app_id,
        "-appkey", bugly_app_key,
        "-bundleid", bundle_id,
        "-version", version_name,
        "-buildNo", version_code,
        "-platform", "Android",
        "-inputMapping", mapping_file
    ]
    
    print("Executing upload command...")
    try:
        # Capture output to show in logs
        result = subprocess.run(cmd, capture_output=True, text=True)
        print("STDOUT:", result.stdout)
        print("STDERR:", result.stderr)
        
        if result.returncode != 0:
            print(f"Bugly upload failed with return code {result.returncode}")
            sys.exit(1)
        else:
            print("Bugly upload completed successfully.")
            
    except Exception as e:
        print(f"Error executing Bugly tool: {e}")
        sys.exit(1)

if __name__ == '__main__':
    main()