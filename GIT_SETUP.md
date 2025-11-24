# Git Setup Guide

Quick guide for setting up version control for Apache Ignite training materials.

## Quick Start

### Option 1: Automated Setup (Recommended)

```bash
cd /Users/jwkidd3/classes_in_development/apache_ignite_v1
./init-git.sh
```

This script will:
- Initialize git repository
- Configure git user (if needed)
- Check for common mistakes
- Create initial commit
- Rename to 'main' branch

### Option 2: Manual Setup

```bash
# Initialize repository
git init

# Configure user (if not already set globally)
git config user.name "Your Name"
git config user.email "your.email@example.com"

# Add all files
git add .

# Create initial commit
git commit -m "Initial commit: Apache Ignite training materials"

# Rename to main branch
git branch -M main
```

## What Gets Committed

### ✅ Included in Version Control

**Course Content:**
- `README.md` - Main documentation
- `outline.md` - Course outline
- `course_flow.md` - 3-day schedule
- `COURSE_MATERIALS_SUMMARY.md` - Complete overview

**Labs:**
- All `labs/*.md` files
- Lab instructions and exercises
- Code examples and solutions

**Presentations:**
- All `presentations/*.md` files
- Presentation content and guides
- Instructor notes

**Configuration:**
- `.gitignore` - Git ignore rules
- `init-git.sh` - Setup script
- Example configuration files

### ❌ Excluded from Version Control

**Build Artifacts:**
- `target/` - Maven builds
- `build/` - Gradle builds
- `*.class` - Compiled Java files

**Ignite Runtime:**
- `work/` - Ignite work directory
- `ignite-data/` - Persistence data
- `ignite-wal/` - Write-ahead logs
- `*.log` - Log files

**IDE Files:**
- `.idea/` - IntelliJ IDEA
- `*.iml` - IntelliJ modules
- `.vscode/` - VS Code settings

**macOS Files:**
- `.DS_Store` - Finder metadata
- `._*` - AppleDouble files
- `.Spotlight-V100` - Spotlight indexes

**Credentials:**
- `*.jks` - Java KeyStore
- `*.key` - Private keys
- `.env` - Environment variables

## Repository Structure

```
apache_ignite_v1/
├── .git/                    # Git repository (created by init)
├── .gitignore              # Ignore rules ✅
├── README.md               # Main documentation ✅
├── outline.md              # Course outline ✅
├── course_flow.md          # Schedule ✅
├── init-git.sh             # Git setup script ✅
├── GIT_SETUP.md            # This file ✅
├── GITIGNORE_GUIDE.md      # Ignore guide ✅
├── labs/                   # Lab materials ✅
│   ├── README.md
│   ├── lab01_*.md
│   ├── ...
│   └── lab12_*.md
└── presentations/          # Presentations ✅
    ├── README.md
    ├── module01_*.md
    ├── ...
    └── module12_*.md
```

## Connecting to Remote Repository

### GitHub

```bash
# Create repository on GitHub first, then:
git remote add origin https://github.com/yourusername/apache-ignite-training.git
git push -u origin main
```

### GitLab

```bash
git remote add origin https://gitlab.com/yourusername/apache-ignite-training.git
git push -u origin main
```

### Bitbucket

```bash
git remote add origin https://bitbucket.org/yourusername/apache-ignite-training.git
git push -u origin main
```

## Common Git Commands

### Daily Workflow

```bash
# Check status
git status

# Add changes
git add .

# Commit changes
git commit -m "Updated lab 5 exercises"

# Push to remote
git push

# Pull latest changes
git pull
```

### Branching

```bash
# Create new branch
git checkout -b feature/new-lab

# Switch branches
git checkout main

# Merge branch
git merge feature/new-lab

# Delete branch
git branch -d feature/new-lab
```

### Viewing Changes

```bash
# See what changed
git diff

# See staged changes
git diff --cached

# View history
git log --oneline --graph

# Show specific commit
git show <commit-hash>
```

## Branch Strategy

### For Course Development

**Main Branch:**
- Production-ready course materials
- Tested and reviewed content
- Stable version for delivery

**Develop Branch:**
```bash
git checkout -b develop
```
- Active development
- New labs and modules
- Testing new approaches

**Feature Branches:**
```bash
git checkout -b feature/lab13-kubernetes
git checkout -b feature/module-streaming
git checkout -b fix/lab5-typos
```
- Specific features or fixes
- Merge into develop when complete

### Workflow

```bash
# Start new feature
git checkout develop
git checkout -b feature/new-content

# Make changes
# ... edit files ...
git add .
git commit -m "Add new content"

# Merge to develop
git checkout develop
git merge feature/new-content

# When stable, merge to main
git checkout main
git merge develop
git push origin main
```

## Tags for Releases

```bash
# Create version tag
git tag -a v1.0 -m "Version 1.0 - Initial release"
git push origin v1.0

# List tags
git tag

# Checkout specific version
git checkout v1.0
```

### Suggested Versioning

- `v1.0` - Initial complete course
- `v1.1` - Minor updates and fixes
- `v2.0` - Major course revision

## Handling Large Files

### If You Need to Track Large Files

Use Git LFS (Large File Storage):

```bash
# Install Git LFS
brew install git-lfs

# Initialize in repo
git lfs install

# Track large files
git lfs track "*.pdf"
git lfs track "*.pptx"
git lfs track "*.mp4"

# Add .gitattributes
git add .gitattributes

# Commit and push
git commit -m "Add Git LFS tracking"
git push
```

## Troubleshooting

### Problem: Accidentally Committed Large Files

```bash
# Remove from last commit
git rm --cached large-file.zip
git commit --amend

# Remove from history (careful!)
git filter-branch --tree-filter 'rm -f large-file.zip' HEAD
```

### Problem: Committed Credentials

**⚠️ URGENT - SECURITY ISSUE**

```bash
# Remove sensitive file from history
git filter-branch --force --index-filter \
  "git rm --cached --ignore-unmatch path/to/credentials.jks" \
  --prune-empty --tag-name-filter cat -- --all

# Force push
git push origin --force --all
```

**THEN:**
- Rotate all credentials immediately
- Change all passwords
- Revoke and regenerate certificates

### Problem: Merge Conflicts

```bash
# See conflicts
git status

# Edit conflicted files
# Look for <<<<<<< HEAD markers

# After resolving
git add resolved-file.md
git commit -m "Resolved merge conflict"
```

## Collaboration

### For Multiple Instructors

**Setup:**
```bash
# Each instructor clones the repo
git clone https://github.com/org/apache-ignite-training.git

# Create personal branch
git checkout -b instructor/john-labs
```

**Working Together:**
```bash
# Pull latest changes regularly
git checkout main
git pull

# Update your branch
git checkout instructor/john-labs
git merge main

# Push your changes
git push origin instructor/john-labs

# Create pull request for review
```

### Code Review Process

1. Create feature branch
2. Make changes and commit
3. Push to remote
4. Create pull request
5. Review and discuss
6. Merge when approved

## Best Practices

### Commit Messages

**Good:**
```
Add Lab 13: Advanced Streaming

- Created new lab for Kafka Streams integration
- Added exercises for data transformation
- Included troubleshooting guide
```

**Bad:**
```
fixed stuff
update
changes
```

### Commit Frequency

- Commit logical units of work
- Don't commit broken code
- Commit before major changes
- Push at end of day

### What to Review Before Commit

```bash
# Always check what you're committing
git status
git diff

# Check for sensitive data
git diff | grep -i password
git diff | grep -i secret

# Review each file
git diff filename
```

## Backup Strategy

### Local Backups

```bash
# Create backup branch
git branch backup-$(date +%Y%m%d)

# Or clone to backup location
git clone . ../apache-ignite-backup
```

### Remote Backups

- Push to GitHub/GitLab/Bitbucket
- Use multiple remotes
- Regular backups to external drive

```bash
# Add backup remote
git remote add backup git@backup-server:repo.git
git push backup main
```

## Resources

### Git Documentation
- [Pro Git Book](https://git-scm.com/book/en/v2)
- [GitHub Guides](https://guides.github.com/)
- [Atlassian Git Tutorials](https://www.atlassian.com/git/tutorials)

### Git Tools
- [GitHub Desktop](https://desktop.github.com/) - GUI client
- [SourceTree](https://www.sourcetreeapp.com/) - Free Git GUI
- [GitKraken](https://www.gitkraken.com/) - Git client

### macOS Git Tools
```bash
# Install via Homebrew
brew install git
brew install git-lfs
brew install tig  # Terminal git browser
```

## Quick Reference Card

```bash
# Setup
git init                          # Initialize repository
git clone <url>                   # Clone repository

# Basic Commands
git status                        # Check status
git add <file>                    # Stage file
git add .                         # Stage all
git commit -m "message"           # Commit
git push                          # Push to remote
git pull                          # Pull from remote

# Branching
git branch                        # List branches
git branch <name>                 # Create branch
git checkout <name>               # Switch branch
git checkout -b <name>            # Create and switch
git merge <branch>                # Merge branch

# History
git log                           # View history
git log --oneline --graph         # Compact history
git diff                          # Show changes
git show <commit>                 # Show commit

# Undo
git checkout -- <file>            # Discard changes
git reset HEAD <file>             # Unstage file
git revert <commit>               # Revert commit

# Remote
git remote -v                     # List remotes
git remote add origin <url>       # Add remote
git fetch                         # Fetch from remote
git push -u origin main           # Push with upstream
```

---

**Ready to version control your training materials!** 🚀

For questions or issues, refer to:
- `.gitignore` file for ignore rules
- `GITIGNORE_GUIDE.md` for detailed explanations
- `init-git.sh` script for automated setup
