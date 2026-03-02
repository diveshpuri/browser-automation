#!/usr/bin/env bash
# =============================================================================
# Browser Automation - Setup Script for macOS and Linux
# =============================================================================
# This script installs all prerequisites and sets up the project for development.
#
# Usage:
#   chmod +x scripts/setup.sh
#   ./scripts/setup.sh
#
# What it does:
#   1. Checks/installs Java 17+ (via SDKMAN or system package manager)
#   2. Checks/installs Maven 3.6+
#   3. Installs Playwright browsers (Chromium)
#   4. Builds the project
#   5. Runs the test suite
# =============================================================================

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

info()    { echo -e "${BLUE}[INFO]${NC} $*"; }
success() { echo -e "${GREEN}[OK]${NC}   $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC} $*"; }
error()   { echo -e "${RED}[ERR]${NC}  $*"; }

header() {
    echo ""
    echo "======================================================"
    echo "  Browser Automation - Setup"
    echo "======================================================"
    echo ""
}

# Detect OS
detect_os() {
    case "$(uname -s)" in
        Darwin*)  OS="macos" ;;
        Linux*)   OS="linux" ;;
        *)        OS="unknown" ;;
    esac

    if [ "$OS" = "linux" ]; then
        if [ -f /etc/os-release ]; then
            . /etc/os-release
            DISTRO="$ID"
        else
            DISTRO="unknown"
        fi
    fi

    info "Detected OS: $OS"
    if [ "$OS" = "linux" ]; then
        info "Detected distribution: ${DISTRO:-unknown}"
    fi
}

# Check if a command exists
cmd_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Check Java version (returns 0 if >= 17)
check_java_version() {
    if ! cmd_exists java; then
        return 1
    fi
    local version
    version=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
    [ "$version" -ge 17 ] 2>/dev/null
}

# Check Maven version (returns 0 if >= 3.6)
check_maven_version() {
    if ! cmd_exists mvn; then
        return 1
    fi
    local version
    version=$(mvn -version 2>&1 | head -1 | grep -oP '\d+\.\d+' | head -1)
    local major minor
    major=$(echo "$version" | cut -d'.' -f1)
    minor=$(echo "$version" | cut -d'.' -f2)
    [ "$major" -ge 3 ] && [ "$minor" -ge 6 ] 2>/dev/null
}

# Install Java 17
install_java() {
    info "Installing Java 17..."

    if [ "$OS" = "macos" ]; then
        if cmd_exists brew; then
            info "Installing via Homebrew..."
            brew install openjdk@17
            # Create symlink for macOS
            sudo ln -sfn "$(brew --prefix)/opt/openjdk@17/libexec/openjdk.jdk" /Library/Java/JavaVirtualMachines/openjdk-17.jdk 2>/dev/null || true
            export JAVA_HOME="$(brew --prefix)/opt/openjdk@17"
            export PATH="$JAVA_HOME/bin:$PATH"
        else
            warn "Homebrew not found. Installing Homebrew first..."
            /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
            brew install openjdk@17
            export JAVA_HOME="$(brew --prefix)/opt/openjdk@17"
            export PATH="$JAVA_HOME/bin:$PATH"
        fi
    elif [ "$OS" = "linux" ]; then
        case "${DISTRO:-}" in
            ubuntu|debian|pop|linuxmint)
                info "Installing via apt..."
                sudo apt-get update -qq
                sudo apt-get install -y -qq openjdk-17-jdk
                ;;
            fedora|rhel|centos|rocky|almalinux)
                info "Installing via dnf/yum..."
                if cmd_exists dnf; then
                    sudo dnf install -y java-17-openjdk-devel
                else
                    sudo yum install -y java-17-openjdk-devel
                fi
                ;;
            arch|manjaro)
                info "Installing via pacman..."
                sudo pacman -Sy --noconfirm jdk17-openjdk
                ;;
            opensuse*|sles)
                info "Installing via zypper..."
                sudo zypper install -y java-17-openjdk-devel
                ;;
            *)
                warn "Unsupported distribution: ${DISTRO:-unknown}"
                warn "Attempting to install via SDKMAN..."
                install_java_sdkman
                return
                ;;
        esac
    fi

    if check_java_version; then
        success "Java 17+ installed successfully"
    else
        warn "Package manager install may not have set Java 17 as default."
        warn "Attempting SDKMAN as fallback..."
        install_java_sdkman
    fi
}

# Install Java via SDKMAN (fallback)
install_java_sdkman() {
    if ! cmd_exists sdk; then
        info "Installing SDKMAN..."
        curl -s "https://get.sdkman.io" | bash
        source "$HOME/.sdkman/bin/sdkman-init.sh"
    fi
    sdk install java 17.0.11-tem
    success "Java 17 installed via SDKMAN"
}

# Install Maven
install_maven() {
    info "Installing Maven..."

    if [ "$OS" = "macos" ]; then
        if cmd_exists brew; then
            brew install maven
        else
            warn "Homebrew not installed. Please install Maven manually."
            return 1
        fi
    elif [ "$OS" = "linux" ]; then
        case "${DISTRO:-}" in
            ubuntu|debian|pop|linuxmint)
                sudo apt-get install -y -qq maven
                ;;
            fedora|rhel|centos|rocky|almalinux)
                if cmd_exists dnf; then
                    sudo dnf install -y maven
                else
                    sudo yum install -y maven
                fi
                ;;
            arch|manjaro)
                sudo pacman -Sy --noconfirm maven
                ;;
            opensuse*|sles)
                sudo zypper install -y maven
                ;;
            *)
                warn "Unsupported distribution. Installing Maven manually..."
                install_maven_manual
                return
                ;;
        esac
    fi

    if check_maven_version; then
        success "Maven 3.6+ installed successfully"
    else
        warn "Package manager Maven may be too old. Installing manually..."
        install_maven_manual
    fi
}

# Install Maven manually (fallback)
install_maven_manual() {
    local MVN_VERSION="3.9.6"
    local MVN_URL="https://dlcdn.apache.org/maven/maven-3/${MVN_VERSION}/binaries/apache-maven-${MVN_VERSION}-bin.tar.gz"
    local MVN_DIR="$HOME/.local/share/maven"

    info "Downloading Maven ${MVN_VERSION}..."
    mkdir -p "$MVN_DIR"
    curl -fsSL "$MVN_URL" | tar xz -C "$MVN_DIR" --strip-components=1
    export PATH="$MVN_DIR/bin:$PATH"

    if check_maven_version; then
        success "Maven ${MVN_VERSION} installed to $MVN_DIR"
        warn "Add the following to your shell profile (~/.bashrc or ~/.zshrc):"
        warn "  export PATH=\"$MVN_DIR/bin:\$PATH\""
    else
        error "Maven installation failed"
        return 1
    fi
}

# Install Playwright system dependencies (Linux only)
install_playwright_deps() {
    if [ "$OS" = "linux" ]; then
        info "Installing Playwright system dependencies..."
        case "${DISTRO:-}" in
            ubuntu|debian|pop|linuxmint)
                sudo apt-get install -y -qq \
                    libnss3 libnspr4 libatk1.0-0 libatk-bridge2.0-0 \
                    libcups2 libdrm2 libxkbcommon0 libxcomposite1 \
                    libxdamage1 libxfixes3 libxrandr2 libgbm1 \
                    libpango-1.0-0 libcairo2 libasound2 libatspi2.0-0 \
                    libwayland-client0 2>/dev/null || true
                ;;
            *)
                info "You may need to install Playwright system deps manually."
                info "See: https://playwright.dev/java/docs/intro#system-requirements"
                ;;
        esac
    fi
}

# Main setup flow
main() {
    header
    detect_os

    if [ "$OS" = "unknown" ]; then
        error "Unsupported operating system. Please use macOS or Linux."
        error "For Windows, run scripts/setup.ps1 instead."
        exit 1
    fi

    # Step 1: Java
    echo ""
    info "Step 1/5: Checking Java 17+..."
    if check_java_version; then
        success "Java 17+ found: $(java -version 2>&1 | head -1)"
    else
        warn "Java 17+ not found"
        install_java
    fi

    # Step 2: Maven
    echo ""
    info "Step 2/5: Checking Maven 3.6+..."
    if check_maven_version; then
        success "Maven found: $(mvn -version 2>&1 | head -1)"
    else
        warn "Maven 3.6+ not found"
        install_maven
    fi

    # Step 3: Playwright dependencies + browsers
    echo ""
    info "Step 3/5: Installing Playwright browsers..."
    install_playwright_deps
    mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install --with-deps chromium" -B 2>/dev/null || \
        mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium" -B
    success "Playwright Chromium browser installed"

    # Step 4: Build
    echo ""
    info "Step 4/5: Building project..."
    mvn clean compile test-compile -B -q
    success "Project built successfully"

    # Step 5: Run tests
    echo ""
    info "Step 5/5: Running test suite..."
    mvn test -B -q
    success "All tests passed"

    # Summary
    echo ""
    echo "======================================================"
    echo "  Setup Complete!"
    echo "======================================================"
    echo ""
    echo "  Java:       $(java -version 2>&1 | head -1)"
    echo "  Maven:      $(mvn -version 2>&1 | head -1)"
    echo "  Playwright: Chromium installed"
    echo "  Build:      SUCCESS"
    echo "  Tests:      ALL PASSED"
    echo ""
    echo "  Next steps:"
    echo "    1. Set your LLM API key as an environment variable:"
    echo "       export GEMINI_API_KEY=your-key"
    echo "       export OPENAI_API_KEY=your-key"
    echo "       export ANTHROPIC_API_KEY=your-key"
    echo ""
    echo "    2. Run the E2E test:"
    echo "       GEMINI_API_KEY=your-key mvn test -Dgroups=e2e"
    echo ""
    echo "    3. Try the example:"
    echo "       See examples/EndToEndExample.java"
    echo ""
    echo "======================================================"
}

main "$@"
