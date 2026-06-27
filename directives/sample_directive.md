# Sample Directive: Fetch Project Statistics

This Standard Operating Procedure (SOP) outlines how the agent can count code lines in the Kotlin codebase using a deterministic execution script.

## Goal
Count the number of files and lines in the Android project source code (`app/src`).

## Inputs
- `directory_path`: Absolute path to search for Kotlin source files.

## Tools/Scripts
- `execution/sample_tool.py`

## Expected Outputs
- A JSON output showing total files count, total lines of code count, and a list of scanned directories.

## Edge Cases & Error Handling
- If the directory does not exist, the script should return a clean error code and message.
- Exclude `build/` and `.gradle/` files.
