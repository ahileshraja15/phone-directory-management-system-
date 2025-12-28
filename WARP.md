# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

Project: Phone Directory System (Java)

Commands
- Run (Windows PowerShell)
  - ./scripts/run.ps1
- Run (macOS/Linux)
  - chmod +x scripts/run.sh
  - ./scripts/run.sh
- What the scripts do
  - Compile all sources into out/
  - Execute the entrypoint class com.example.phonedir.App on the produced classpath
- Direct compile/run (without scripts)
  - javac -d out $(find src/main/java -name "*.java")
  - java -cp out com.example.phonedir.App
- Clean build outputs and saved data (if needed)
  - Remove out/ and data/ directories
- Linting
  - No Java linter/tooling is configured in this repo
- Tests
  - No test suite is present; running a single test is not applicable

High-level architecture
- Package: com.example.phonedir
- Core domain types
  - Person (abstract): Serializable base holding immutable name
  - Contact: extends Person, implements Searchable; holds phoneNumber and email; defines matches(query) against name/phone/email; toString for display
  - Searchable (interface): boolean matches(String query)
  - DuplicateContactException: checked exception for enforcing unique contacts by name
- Directory and persistence
  - PhoneDirectory: Serializable aggregate managing Map<String, Contact> keyed by normalized name (lowercased/trimmed)
    - addContact(Contact): throws DuplicateContactException on name clash
    - getByName(String): exact lookup via normalized key
    - search(String): delegates to Contact.matches over all entries
    - saveToFile(File): serializes the directory; ensures parent folders exist
    - loadFromFile(File): deserializes and type-checks saved data
- Application entrypoint
  - App: main program flow
    - Attempts to load directory from data/phonebook.ser; if missing, starts new
    - Adds sample contacts and intentionally triggers a duplicate to demonstrate exception handling
    - Demonstrates search by substring (e.g., "555", "Alice")
    - Saves directory back to data/phonebook.ser
- Scripts
  - scripts/run.ps1 (PowerShell) and scripts/run.sh (Bash) orchestrate compile and run via javac/java

Notes distilled from README.md
- First run starts with an empty directory and saves to data/phonebook.ser
- Subsequent runs will load the saved directory
- Duplicate add is intentionally attempted to show custom exception handling

GUI
- Run (Windows PowerShell)
  - ./scripts/run_gui.ps1
- Run (macOS/Linux)
  - chmod +x scripts/run_gui.sh
  - ./scripts/run_gui.sh
- UI overview
  - Swing (Nimbus look-and-feel with light theme tweaks)
  - Features: live search, add/update, delete, save; persists to data/phonebook.ser

Mobile-like Contacts UI
- Run (Windows PowerShell)
  - ./scripts/run_mobile_gui.ps1
- Run (macOS/Linux)
  - chmod +x scripts/run_mobile_gui.sh
  - ./scripts/run_mobile_gui.sh
- UI overview
  - Two-pane: list with A–Z index and detail pane
  - Actions: Call, Message, Email, Share, Edit, Delete; toggles: Favorite, Block; filters: favorites-only and hide-blocked
