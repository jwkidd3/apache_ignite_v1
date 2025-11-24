#!/bin/bash

# Git Repository Initialization Script for Apache Ignite Training
# This script properly initializes the git repository with all course materials

set -e  # Exit on error

echo "=================================================="
echo "Apache Ignite Training - Git Repository Setup"
echo "=================================================="
echo ""

# Check if git is installed
if ! command -v git &> /dev/null; then
    echo "❌ Error: git is not installed"
    echo "Install git: brew install git"
    exit 1
fi

echo "✓ Git is installed"

# Check if already a git repository
if [ -d .git ]; then
    echo "⚠️  Warning: .git directory already exists"
    read -p "Reinitialize repository? This will reset git history (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        rm -rf .git
        echo "✓ Removed existing .git directory"
    else
        echo "Exiting without changes"
        exit 0
    fi
fi

# Initialize git repository
echo ""
echo "Initializing git repository..."
git init
echo "✓ Git repository initialized"

# Configure git (if not already configured globally)
if [ -z "$(git config --global user.name)" ]; then
    echo ""
    echo "Git user not configured globally."
    read -p "Enter your name: " git_name
    read -p "Enter your email: " git_email
    git config user.name "$git_name"
    git config user.email "$git_email"
    echo "✓ Git user configured for this repository"
else
    echo "✓ Using global git configuration"
    echo "  Name: $(git config user.name)"
    echo "  Email: $(git config user.email)"
fi

# Ensure .gitignore exists
if [ ! -f .gitignore ]; then
    echo ""
    echo "❌ Error: .gitignore file not found"
    echo "Please ensure .gitignore exists in the repository root"
    exit 1
fi

echo "✓ .gitignore file found"

# Check for common mistakes before initial commit
echo ""
echo "Checking for files that should not be committed..."

# Check for credential files
if find . -name "*.jks" -o -name "*.key" -o -name "*.pem" | grep -v examples | grep -q .; then
    echo "⚠️  Warning: Found credential files (*.jks, *.key, *.pem)"
    echo "Make sure these are in .gitignore or are example files"
fi

# Check for .DS_Store files
if find . -name ".DS_Store" | grep -q .; then
    echo "⚠️  Cleaning up .DS_Store files..."
    find . -name ".DS_Store" -delete
    echo "✓ .DS_Store files removed"
fi

# Check for IDE files
if [ -d .idea ] || find . -name "*.iml" | grep -q .; then
    echo "⚠️  Warning: Found IDE files (.idea, *.iml)"
    echo "These should be ignored by .gitignore"
fi

# Create initial commit
echo ""
echo "Creating initial commit..."

# Stage all files
git add .

# Show what will be committed
echo ""
echo "Files to be committed:"
git status --short

echo ""
read -p "Proceed with initial commit? (Y/n): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Nn]$ ]]; then
    git commit -m "Initial commit: Apache Ignite training course materials

- Complete 3-day course structure
- 12 hands-on labs with detailed instructions
- Presentation content for all modules
- Course flow and outline documentation
- Comprehensive .gitignore for Ignite/macOS"

    echo ""
    echo "✅ Initial commit created successfully!"
else
    echo "❌ Commit cancelled. Files are staged but not committed."
    echo "Run 'git commit' when ready."
    exit 0
fi

# Offer to create main branch
echo ""
read -p "Rename master to main branch? (Y/n): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Nn]$ ]]; then
    git branch -M main
    echo "✓ Branch renamed to 'main'"
fi

# Repository statistics
echo ""
echo "=================================================="
echo "Repository Statistics"
echo "=================================================="
echo "Files tracked: $(git ls-files | wc -l)"
echo "Course materials:"
echo "  - Presentations: $(find presentations -name "*.md" 2>/dev/null | wc -l) modules"
echo "  - Labs: $(find labs -name "lab*.md" 2>/dev/null | wc -l) labs"
echo "  - Documentation: $(find . -maxdepth 1 -name "*.md" 2>/dev/null | wc -l) files"
echo ""

# Show git log
echo "Git history:"
git log --oneline --graph --all
echo ""

# Next steps
echo "=================================================="
echo "Next Steps"
echo "=================================================="
echo ""
echo "Repository is initialized and ready!"
echo ""
echo "To push to a remote repository:"
echo "  1. Create a repository on GitHub/GitLab/Bitbucket"
echo "  2. Add remote: git remote add origin <URL>"
echo "  3. Push: git push -u origin main"
echo ""
echo "To create a development branch:"
echo "  git checkout -b develop"
echo ""
echo "To check repository status:"
echo "  git status"
echo ""
echo "Useful commands:"
echo "  git log --oneline          # View commit history"
echo "  git status --ignored       # See ignored files"
echo "  git check-ignore -v <file> # Check why file is ignored"
echo ""
echo "=================================================="
echo "Setup Complete! ✨"
echo "=================================================="
