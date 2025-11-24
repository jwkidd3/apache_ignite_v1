# .gitignore Guide for Apache Ignite Projects

This document explains the .gitignore file created for Apache Ignite development on macOS.

## Overview

The .gitignore file is organized into sections to exclude:
- Build artifacts and dependencies
- IDE and editor files
- Ignite-specific runtime files
- macOS system files
- Sensitive configuration and credentials

## Important Sections

### 🔥 Apache Ignite Specific

**Excluded:**
```
work/                    # Ignite work directory
ignite-data/            # Native persistence data
ignite-wal/             # Write-Ahead Log
ignite-wal-archive/     # WAL archives
snapshots/              # Cluster snapshots
*.log                   # Log files
*.hprof                 # Heap dumps
```

**Why:** These are runtime artifacts that should not be committed to version control. They can be large and are specific to each environment.

### 💻 macOS Files

**Excluded:**
```
.DS_Store               # Finder metadata
.AppleDouble            # Resource forks
._*                     # AppleDouble files
Icon\r\r                # Custom folder icons
```

**Why:** These are macOS-specific system files that clutter the repository and are useless on other systems.

### 🛠️ Build Tools

**Excluded:**
```
target/                 # Maven build output
build/                  # Gradle build output
.gradle/                # Gradle cache
*.class                 # Compiled Java classes
*.jar                   # JAR files (except wrapper)
```

**Why:** Build artifacts should be generated, not stored in version control.

### 💡 IDEs

**Excluded:**
```
.idea/                  # IntelliJ IDEA
*.iml                   # IntelliJ module files
.vscode/                # VS Code settings
.settings/              # Eclipse settings
```

**Why:** IDE configuration is personal preference and differs between developers.

### 🔐 Security

**Excluded:**
```
*.jks                   # Java KeyStore files
*.key                   # Private keys
*.pem                   # Certificates
.env                    # Environment variables
```

**Why:** Credentials and certificates should NEVER be committed to version control.

## What IS Committed

✅ **DO Commit:**
- Source code (`.java`, `.xml`, `.properties`)
- Documentation (`.md` files)
- Example configurations
- Build scripts (`pom.xml`, `build.gradle`)
- Lab instructions
- Presentation content

❌ **DON'T Commit:**
- Build artifacts
- IDE settings
- Runtime data
- Log files
- Credentials
- System files

## Common Scenarios

### Starting a New Lab

When creating a new lab:
```bash
# These will be tracked
labs/lab01/src/main/java/Example.java  ✅
labs/lab01/pom.xml                      ✅
labs/lab01/README.md                    ✅

# These will be ignored
labs/lab01/target/                      ❌
labs/lab01/ignite-data/                 ❌
labs/lab01/*.log                        ❌
```

### Working with Configurations

```bash
# Production configs - DO commit
config/ignite-config.xml                ✅
config/example-config.xml               ✅

# Local overrides - DON'T commit
config/ignite-local.xml                 ❌
config/local-config.xml                 ❌
```

### Certificates for Labs

```bash
# Example certs - CAN commit
labs/security/examples/test.jks         ✅

# Actual certs - DON'T commit
config/keystore.jks                     ❌
config/server.key                       ❌
```

## Customization

### To Add Project-Specific Patterns

Edit `.gitignore` and add at the bottom:
```bash
# Project Specific
my-custom-pattern/
*.myext
```

### To Temporarily Track Ignored Files

Use `git add -f`:
```bash
# Force add a specific ignored file
git add -f work/important-file.txt
```

### To Check What's Ignored

```bash
# Check if file is ignored
git check-ignore -v filename

# List all ignored files
git status --ignored
```

## Verifying Your Setup

### Check Status
```bash
cd /path/to/apache_ignite_v1
git status
```

Should show only:
- Source files
- Documentation
- Configuration templates
- NOT: build artifacts, logs, IDE files

### Test Ignite Artifacts Are Ignored

```bash
# Start Ignite (creates work directory)
bin/ignite.sh

# Check status
git status

# Should NOT show work/, ignite-data/, or *.log files
```

## Troubleshooting

### Problem: .DS_Store Still Appearing

**Solution:**
```bash
# Remove from tracking
git rm --cached .DS_Store

# Add to .gitignore (already done)
echo ".DS_Store" >> .gitignore

# Commit the change
git commit -m "Remove .DS_Store from tracking"
```

### Problem: IDE Files Still Tracked

**Solution:**
```bash
# Remove from tracking
git rm --cached -r .idea/
git rm --cached *.iml

# Commit
git commit -m "Remove IDE files from tracking"
```

### Problem: Build Artifacts Committed

**Solution:**
```bash
# Remove from tracking
git rm --cached -r target/
git rm --cached -r build/

# Commit
git commit -m "Remove build artifacts from tracking"
```

### Problem: Accidentally Committed Credentials

**⚠️ CRITICAL - DO IMMEDIATELY:**
```bash
# Remove from history (use BFG or git-filter-repo)
# DO NOT just delete and commit - stays in history!

# Option 1: Using BFG
java -jar bfg.jar --delete-files keystore.jks

# Option 2: Using git-filter-repo
git filter-repo --path keystore.jks --invert-paths

# After cleaning history
git push --force
```

**IMPORTANT:**
- Change all credentials immediately
- Rotate keys and certificates
- Consider the repository compromised

## Global Git Ignore (macOS)

For macOS-specific files across ALL projects:

```bash
# Create global gitignore
cat >> ~/.gitignore_global << EOF
.DS_Store
.AppleDouble
.LSOverride
._*
.Spotlight-V100
.Trashes
EOF

# Configure git to use it
git config --global core.excludesfile ~/.gitignore_global
```

## Pre-commit Checks

Consider adding a pre-commit hook to check for sensitive files:

```bash
# .git/hooks/pre-commit
#!/bin/bash

# Check for potential credential files
if git diff --cached --name-only | grep -E '\.(jks|key|pem)$'; then
    echo "Error: Attempting to commit credential files!"
    echo "Please remove sensitive files from staging."
    exit 1
fi

# Check for .env files
if git diff --cached --name-only | grep '\.env'; then
    echo "Warning: Attempting to commit .env file"
    read -p "Are you sure? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi
```

## Best Practices

### ✅ DO:
1. Review `.gitignore` when starting new project
2. Test that Ignite artifacts are ignored
3. Keep credentials out of version control
4. Use example configurations for templates
5. Document any custom ignore patterns

### ❌ DON'T:
1. Commit build artifacts
2. Commit IDE-specific files
3. Commit credentials or keys
4. Commit large binary files
5. Commit runtime data (work/, logs)

## Files to Always Review Before Commit

Run this before committing:
```bash
# See what you're about to commit
git status

# Review changes
git diff --cached

# Check for sensitive patterns
git diff --cached | grep -i 'password\|secret\|key'
```

## Quick Reference Commands

```bash
# Check ignore status
git check-ignore -v <file>

# Show all ignored files
git status --ignored

# Force add ignored file
git add -f <file>

# Remove tracked file (keep local)
git rm --cached <file>

# Remove tracked directory (keep local)
git rm --cached -r <directory>

# Clean ignored files (removes them)
git clean -fdX

# Clean ALL untracked files (careful!)
git clean -fdx
```

## Support

If you need to modify the `.gitignore`:

1. Edit `.gitignore` file
2. Test with `git status`
3. If file was previously tracked: `git rm --cached <file>`
4. Commit the changes

---

**Remember:** The `.gitignore` file itself SHOULD be committed to version control so all team members benefit from it.
