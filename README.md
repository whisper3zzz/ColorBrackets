# ColorBrackets

**ColorBrackets** is a JetBrains IDE plugin that adds rainbow colors to brackets and highlights the current scope with a vertical line, making it easier to read and understand nested code structures.

## Features

- **Rainbow Brackets**: Automatically colors matching brackets `()`, `[]`, `{}`, `<>` based on their nesting level.
- **Scope Highlight**: Displays a colored vertical line on the left side of the editor to indicate the current code block scope. The line color matches the bracket color of the current block.
- **Optimized Performance**: Designed to work smoothly even with large files and deep nesting.

## Supported Languages

The plugin works with any language supported by the IntelliJ Platform that uses standard bracket syntax. It provides the best experience (including Scope Highlight) for C-style languages using `{}` blocks.

- **Perfect Support** (Rainbow Brackets + Scope Highlight):
  - Java, Kotlin
  - C, C++ (CLion)
  - C# (Rider)
  - JavaScript, TypeScript
  - Go, Rust, Dart, PHP, Swift

- **Basic Support** (Rainbow Brackets only):
  - Python, Ruby, Lua (languages without `{}` block delimiters)

## Installation

1. Download the latest release `.zip` file from the [Releases](https://github.com/whisper3zzz/ColorBrackets/releases) page.
2. Open your JetBrains IDE (IntelliJ IDEA, PyCharm, WebStorm, etc.).
3. Go to **Settings/Preferences** -> **Plugins**.
4. Click the gear icon ⚙️ and select **Install Plugin from Disk...**.
5. Select the downloaded `.zip` file.
6. Restart the IDE.

## Automatic Updates

To receive automatic updates without manually downloading new versions:

1. Open **Settings/Preferences** -> **Plugins**.
2. Click the gear icon ⚙️ and select **Manage Plugin Repositories...**.
3. Click **+** and add the following URL:
   ```
   https://raw.githubusercontent.com/whisper3zzz/ColorBrackets/main/updatePlugins.xml
   ```
4. Click **OK**.
5. The IDE will now automatically check for updates to ColorBrackets.

## Building from Source

To build the plugin locally:

```bash
# Linux/macOS
./gradlew buildPlugin

# Windows
.\gradlew.bat buildPlugin
```

The compiled plugin file will be located in `build/distributions/ColorBrackets-x.x.x.zip`.
