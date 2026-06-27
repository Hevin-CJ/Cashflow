#!/usr/bin/env python3
import os
import sys
import json
import argparse

def count_kotlin_lines(directory_path):
    if not os.path.exists(directory_path):
        return {"error": f"Directory not found: {directory_path}"}

    total_files = 0
    total_lines = 0
    scanned_extensions = {'.kt', '.kts'}

    for root, dirs, files in os.walk(directory_path):
        # Exclude hidden and build directories
        dirs[:] = [d for d in dirs if not d.startswith('.') and d not in {'build', 'gradle', '.gradle', 'tmp', '.tmp'}]
        
        for file in files:
            ext = os.path.splitext(file)[1]
            if ext in scanned_extensions:
                file_path = os.path.join(root, file)
                try:
                    with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
                        lines = f.readlines()
                        total_files += 1
                        total_lines += len(lines)
                except Exception as e:
                    # Safe fallback to log/ignore reading errors
                    pass

    return {
        "status": "success",
        "directory": directory_path,
        "total_files": total_files,
        "total_lines": total_lines
    }

def main():
    parser = argparse.ArgumentParser(description="Count Kotlin files and lines of code.")
    parser.add_argument("--dir", required=True, help="Directory path to scan")
    args = parser.parse_args()

    result = count_kotlin_lines(args.dir)
    print(json.dumps(result, indent=2))

if __name__ == "__main__":
    main()
