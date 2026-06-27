# Build and Deploy CashFlow App to Emulator

This Standard Operating Procedure (SOP) outlines the deterministic procedure for verifying the Kotlin codebase (building and running unit tests) and deploying the debug package to the running Android emulator.

## Goal
Verify compilation, run the unit test suite, install the debug APK, and launch the application on the active emulator.

## Inputs
- None required (uses standard local paths and Gradle wrapper).

## Tools/Scripts
- `execution/build_and_deploy.py`

## Expected Outputs
- A JSON output summarizing:
  - Unit test execution status (pass/fail).
  - Gradle debug package build and installation success status.
  - ADB launcher activity startup status.

## Edge Cases & Error Handling
- If no active emulator is connected (via ADB), the script should report a failure requesting the user to launch the emulator first.
- If compile or tests fail, the script should abort early with the Gradle error trace and exit code.
