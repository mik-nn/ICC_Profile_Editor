# GEMINI.md — ICC Profile Editor Implementation Plan

## 🎯 Project Goal
Build a **cross‑platform ICC profile editor** in **Java + JavaFX** with:
- Header and tag viewing/editing (hex/text modes, encoding switch)
- Copy header/tags from another profile
- Mimaki‑specific tools (multi‑encoding media name search/replace, Cxf→DevS/CIED conversion)
- Two‑tab UI: Common + Mimaki

---

## 🛠 Tech Stack
- Java 17+
- JavaFX 21+
- Maven
- Custom ICC parser/writer
- Java `Charset` API
- JUnit 5 for tests
- JPackage for distribution

---

## 📋 Implementation Steps

> **Rule:** After completing and verifying each step, mark it with ✅ and run:
> ```bash
> git add .
> git commit -m "✅ Step X completed: <short description>"
> git push origin main
> ```

1. **Project Setup**
   - Create Maven JavaFX project structure
   - Add dependencies to `pom.xml`
   - Verify `mvn javafx:run` launches empty UI  
   **Status:** ⬜

2. **Core ICC Parser**
   - Implement `ICCProfileReader` (header + tag table parsing)
   - Implement `ICCProfileWriter` (save modified profiles)
   - Unit tests for parsing/writing  
   **Status:** ⬜

3. **Encoding Utilities**
   - `EncodingUtils` for UTF‑8, UTF‑16LE, ASCII conversions
   - Detect encoding from BOM or user selection  
   **Status:** ⬜

4. **UI — Common Tab**
   - Header editor form
   - Tag list table (signature, type, size, description)
   - Tag editor pane with Hex/Text modes + encoding dropdown  
   **Status:** ⬜

5. **UI — Mimaki Tab**
   - Search/replace media names in MMK* tags (multi‑encoding)
   - Cxf→DevS/CIED conversion tool  
   **Status:** ⬜

6. **Copy Header/Tags Feature**
   - Load second profile in background
   - Copy selected header/tags into current profile  
   **Status:** ⬜

7. **Validation & Undo/Redo**
   - Validate ICC structure before save
   - Implement undo/redo stack  
   **Status:** ⬜

8. **Packaging**
   - JPackage builds for Windows, macOS, Linux
   - Test installers on each OS  
   **Status:** ⬜

---

## 📏 Coding Standards
- Follow Java naming conventions
- Keep UI responsive (JavaFX best practices)
- All new code must have unit tests
- Commit messages: `feat:`, `fix:`, `chore:`, `✅ Step X completed: ...`

---

## 🤖 Gemini CLI Usage
- Use `gemini edit` for generating or refactoring code
- Use `gemini ask` for explanations or design discussions
- Always reference this `GEMINI.md` for context before coding

---

## 📌 Notes
- ICC spec reference: [International Color Consortium](https://color.org/index.xalter)
- Mimaki tag behavior documented in `docs/mimaki_notes.md`
