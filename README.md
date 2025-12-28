# Phone Directory System (Java)

A simple Java program that manages a phone directory with contacts. Demonstrates:
- Inheritance (Person → Contact)
- Interface usage (Searchable)
- Collections (HashMap for Name → Contact)
- File I/O with serialization for persistence
- Exception handling (duplicate contacts, file-not-found on load)

## Structure
- `src/main/java/com/example/phonedir/`
  - `Person.java` (abstract base)
  - `Searchable.java` (interface)
  - `Contact.java` (extends Person, implements Searchable)
  - `DuplicateContactException.java`
  - `PhoneDirectory.java` (HashMap-backed directory with save/load)
  - `App.java` (demo entrypoint)
  - `GuiApp.java` and `PhoneDirectoryUI.java` (Swing GUI entrypoint and UI)
- `data/` (serialized data stored here)
- `out/` (compiled classes)
- `scripts/` (helper scripts)

## Run (Windows PowerShell)
```
./scripts/run.ps1
```

## Run (macOS/Linux)
```
chmod +x scripts/run.sh
./scripts/run.sh
```

Both scripts will:
1) Compile sources into `out/`
2) Run `com.example.phonedir.App`

## GUI (Windows PowerShell)
```
./scripts/run_gui.ps1
```

## GUI (macOS/Linux)
```
chmod +x scripts/run.sh
./scripts/run_gui.sh
```

The GUI provides:
- Live search over Name/Phone/Email
- Add/Update and Delete actions
- Save to `data/phonebook.ser`

## Mobile-like Contacts UI (Windows PowerShell)
```
./scripts/run_mobile_gui.ps1
```

## Mobile-like Contacts UI (macOS/Linux)
```
chmod +x scripts/run_mobile_gui.sh
./scripts/run_mobile_gui.sh
```

The mobile-like UI adds:
- Two-pane layout: contact list with A–Z index and detail panel
- Actions: Call, Message, Email, Share, Edit, Delete
- Toggles: Favorites, Blocked (with filters to show favorites only / hide blocked)
- Save to `data/phonebook.ser`

## Notes
- First run starts with an empty directory and saves to `data/phonebook.ser`.
- Subsequent runs will load the saved directory.
- A duplicate add is attempted in the console demo to demonstrate custom exception handling.
