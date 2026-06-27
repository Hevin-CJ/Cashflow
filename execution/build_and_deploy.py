#!/usr/bin/env python3
import os
import sys
import json
import subprocess

def run_command(command, cwd=None):
    try:
        result = subprocess.run(
            command,
            cwd=cwd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            shell=True
        )
        return {
            "success": result.returncode == 0,
            "stdout": result.stdout,
            "stderr": result.stderr,
            "exit_code": result.returncode
        }
    except Exception as e:
        return {
            "success": False,
            "stdout": "",
            "stderr": str(e),
            "exit_code": -1
        }

def build_and_deploy(project_dir):
    # 1. Check if emulator/device is connected via adb
    adb_check = run_command("adb devices", cwd=project_dir)
    if not adb_check["success"]:
        return {
            "status": "failed",
            "step": "check_devices",
            "message": "ADB command failed. Please check if ADB is installed and in your PATH.",
            "details": adb_check["stderr"]
        }
    
    # Parse adb devices output
    lines = [line.strip() for line in adb_check["stdout"].split("\n") if line.strip()]
    devices = [line for line in lines[1:] if "device" in line and not "offline" in line]
    
    if not devices:
        return {
            "status": "failed",
            "step": "check_devices",
            "message": "No active Android emulators or devices connected. Please start your emulator first."
        }
    
    # 2. Run unit tests
    test_run = run_command("./gradlew testDebugUnitTest", cwd=project_dir)
    if not test_run["success"]:
        return {
            "status": "failed",
            "step": "run_tests",
            "message": "Unit tests failed to compile or run.",
            "details": test_run["stderr"] + "\n" + test_run["stdout"]
        }
    
    # 3. Install on emulator
    install_run = run_command("./gradlew installDebug", cwd=project_dir)
    if not install_run["success"]:
        return {
            "status": "failed",
            "step": "install_apk",
            "message": "Failed to install the debug APK on the device.",
            "details": install_run["stderr"] + "\n" + install_run["stdout"]
        }
        
    # 4. Start main activity
    launch_run = run_command("adb shell am start -n com.hevincj.cashflow/com.hevincj.cashflow.MainActivity", cwd=project_dir)
    if not launch_run["success"]:
        return {
            "status": "failed",
            "step": "launch_activity",
            "message": "Failed to launch MainActivity via ADB.",
            "details": launch_run["stderr"]
        }
        
    return {
        "status": "success",
        "message": "Code verification passed, APK successfully installed and launched on the active emulator.",
        "tests": "PASSED",
        "install": "SUCCESS",
        "launch": "SUCCESS"
    }

def main():
    project_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    result = build_and_deploy(project_dir)
    print(json.dumps(result, indent=2))
    if result["status"] != "success":
        sys.exit(1)

if __name__ == "__main__":
    main()
