# =============================================================================
# Browser Automation - Setup Script for Windows (PowerShell)
# =============================================================================
# This script installs all prerequisites and sets up the project for development.
#
# Usage (run in PowerShell as Administrator):
#   Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
#   .\scripts\setup.ps1
#
# What it does:
#   1. Checks/installs Java 17+ (via winget or Chocolatey)
#   2. Checks/installs Maven 3.6+
#   3. Installs Playwright browsers (Chromium)
#   4. Builds the project
#   5. Runs the test suite
# =============================================================================

$ErrorActionPreference = "Stop"

# Colors
function Write-Info    { Write-Host "[INFO] $args" -ForegroundColor Cyan }
function Write-Ok      { Write-Host "[OK]   $args" -ForegroundColor Green }
function Write-Warn    { Write-Host "[WARN] $args" -ForegroundColor Yellow }
function Write-Err     { Write-Host "[ERR]  $args" -ForegroundColor Red }

function Write-Header {
    Write-Host ""
    Write-Host "======================================================" -ForegroundColor White
    Write-Host "  Browser Automation - Setup (Windows)" -ForegroundColor White
    Write-Host "======================================================" -ForegroundColor White
    Write-Host ""
}

# Check if a command exists
function Test-Command {
    param([string]$Name)
    return [bool](Get-Command $Name -ErrorAction SilentlyContinue)
}

# Check Java version >= 17
function Test-JavaVersion {
    if (-not (Test-Command "java")) { return $false }
    try {
        $output = & java -version 2>&1 | Select-Object -First 1
        if ($output -match '"(\d+)') {
            $major = [int]$Matches[1]
            return $major -ge 17
        }
    } catch {}
    return $false
}

# Check Maven version >= 3.6
function Test-MavenVersion {
    if (-not (Test-Command "mvn")) { return $false }
    try {
        $output = & mvn -version 2>&1 | Select-Object -First 1
        if ($output -match '(\d+)\.(\d+)') {
            $major = [int]$Matches[1]
            $minor = [int]$Matches[2]
            return ($major -ge 3) -and ($minor -ge 6)
        }
    } catch {}
    return $false
}

# Check if winget is available
function Test-Winget {
    return [bool](Get-Command "winget" -ErrorAction SilentlyContinue)
}

# Check if Chocolatey is available
function Test-Choco {
    return [bool](Get-Command "choco" -ErrorAction SilentlyContinue)
}

# Install Java 17
function Install-Java {
    Write-Info "Installing Java 17..."

    if (Test-Winget) {
        Write-Info "Installing via winget..."
        winget install --id Microsoft.OpenJDK.17 --accept-source-agreements --accept-package-agreements
    }
    elseif (Test-Choco) {
        Write-Info "Installing via Chocolatey..."
        choco install openjdk17 -y
    }
    else {
        Write-Err "Neither winget nor Chocolatey found."
        Write-Err "Please install Java 17 manually:"
        Write-Err "  Option 1: winget install Microsoft.OpenJDK.17"
        Write-Err "  Option 2: Download from https://adoptium.net/temurin/releases/?version=17"
        Write-Err "  Option 3: Install Chocolatey (https://chocolatey.org/install) then: choco install openjdk17"
        throw "Java 17 installation failed"
    }

    # Refresh PATH
    $env:Path = [System.Environment]::GetEnvironmentVariable("Path", "Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path", "User")

    if (Test-JavaVersion) {
        Write-Ok "Java 17+ installed successfully"
    } else {
        Write-Warn "Java installed but may need a new terminal session to take effect."
        Write-Warn "Close this terminal, open a new one, and re-run this script."
    }
}

# Install Maven
function Install-Maven {
    Write-Info "Installing Maven..."

    if (Test-Winget) {
        Write-Info "Installing via winget..."
        winget install --id Apache.Maven --accept-source-agreements --accept-package-agreements
    }
    elseif (Test-Choco) {
        Write-Info "Installing via Chocolatey..."
        choco install maven -y
    }
    else {
        Write-Warn "Neither winget nor Chocolatey found. Installing Maven manually..."
        Install-MavenManual
        return
    }

    # Refresh PATH
    $env:Path = [System.Environment]::GetEnvironmentVariable("Path", "Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path", "User")

    if (Test-MavenVersion) {
        Write-Ok "Maven 3.6+ installed successfully"
    } else {
        Write-Warn "Maven installed but may need a new terminal session."
    }
}

# Install Maven manually (fallback)
function Install-MavenManual {
    $MVN_VERSION = "3.9.6"
    $MVN_URL = "https://dlcdn.apache.org/maven/maven-3/$MVN_VERSION/binaries/apache-maven-$MVN_VERSION-bin.zip"
    $MVN_DIR = "$env:USERPROFILE\.maven"
    $MVN_ZIP = "$env:TEMP\maven.zip"

    Write-Info "Downloading Maven $MVN_VERSION..."
    Invoke-WebRequest -Uri $MVN_URL -OutFile $MVN_ZIP
    Expand-Archive -Path $MVN_ZIP -DestinationPath $MVN_DIR -Force
    Remove-Item $MVN_ZIP

    $mvnBin = Join-Path $MVN_DIR "apache-maven-$MVN_VERSION\bin"
    $env:Path = "$mvnBin;$env:Path"

    # Set user PATH permanently
    $userPath = [System.Environment]::GetEnvironmentVariable("Path", "User")
    if ($userPath -notlike "*$mvnBin*") {
        [System.Environment]::SetEnvironmentVariable("Path", "$mvnBin;$userPath", "User")
        Write-Ok "Maven $MVN_VERSION installed to $MVN_DIR and added to PATH"
    }
}

# Main setup flow
function Main {
    Write-Header

    Write-Info "Windows version: $([System.Environment]::OSVersion.VersionString)"
    Write-Info "PowerShell version: $($PSVersionTable.PSVersion)"

    # Step 1: Java
    Write-Host ""
    Write-Info "Step 1/5: Checking Java 17+..."
    if (Test-JavaVersion) {
        $javaVer = & java -version 2>&1 | Select-Object -First 1
        Write-Ok "Java 17+ found: $javaVer"
    } else {
        Write-Warn "Java 17+ not found"
        Install-Java
    }

    # Step 2: Maven
    Write-Host ""
    Write-Info "Step 2/5: Checking Maven 3.6+..."
    if (Test-MavenVersion) {
        $mvnVer = & mvn -version 2>&1 | Select-Object -First 1
        Write-Ok "Maven found: $mvnVer"
    } else {
        Write-Warn "Maven 3.6+ not found"
        Install-Maven
    }

    # Step 3: Playwright browsers
    Write-Host ""
    Write-Info "Step 3/5: Installing Playwright browsers..."
    try {
        & mvn exec:java -e "-Dexec.mainClass=com.microsoft.playwright.CLI" "-Dexec.args=install --with-deps chromium" -B 2>&1 | Out-Null
    } catch {
        & mvn exec:java -e "-Dexec.mainClass=com.microsoft.playwright.CLI" "-Dexec.args=install chromium" -B
    }
    Write-Ok "Playwright Chromium browser installed"

    # Step 4: Build
    Write-Host ""
    Write-Info "Step 4/5: Building project..."
    & mvn clean compile test-compile -B -q
    if ($LASTEXITCODE -ne 0) { throw "Build failed" }
    Write-Ok "Project built successfully"

    # Step 5: Run tests
    Write-Host ""
    Write-Info "Step 5/5: Running test suite..."
    & mvn test -B -q
    if ($LASTEXITCODE -ne 0) { throw "Tests failed" }
    Write-Ok "All tests passed"

    # Summary
    Write-Host ""
    Write-Host "======================================================" -ForegroundColor Green
    Write-Host "  Setup Complete!" -ForegroundColor Green
    Write-Host "======================================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "  Next steps:" -ForegroundColor White
    Write-Host "    1. Set your LLM API key:" -ForegroundColor White
    Write-Host '       $env:GEMINI_API_KEY = "your-key"' -ForegroundColor Gray
    Write-Host '       $env:OPENAI_API_KEY = "your-key"' -ForegroundColor Gray
    Write-Host '       $env:ANTHROPIC_API_KEY = "your-key"' -ForegroundColor Gray
    Write-Host ""
    Write-Host "    2. Run the E2E test:" -ForegroundColor White
    Write-Host '       $env:GEMINI_API_KEY = "your-key"; mvn test -Dgroups=e2e' -ForegroundColor Gray
    Write-Host ""
    Write-Host "    3. Try the example:" -ForegroundColor White
    Write-Host "       See examples\EndToEndExample.java" -ForegroundColor Gray
    Write-Host ""
    Write-Host "======================================================" -ForegroundColor Green
}

Main
