# Implementation Plan — ICC Profile Editor

## 🎯 Project Goal
Develop a **cross‑platform ICC profile editor** in **Java + JavaFX** with:
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

## 📋 Step‑by‑Step Implementation

> **Rule:** After completing and verifying each step, mark it with ✅ and run:
> ```bash
> git add .
> git commit -m "✅ Step X completed: <short description>"
> git push origin main
> ```

---

### ✅ 1. **Project Setup**
- Create Maven JavaFX project structure:
  ```bash
  mvn archetype:generate \
    -DgroupId=com.mik.icc \
    -DartifactId=icc-profile-editor \
    -DarchetypeArtifactId=maven-archetype-quickstart \
    -DinteractiveMode=false
```

### ✅ 2. **Configure JavaFX and Maven**
**Tasks:**
- Add JavaFX dependencies to `pom.xml`.
- Configure Maven to run JavaFX application.

### ✅ 3. **Implement Basic UI**
**Tasks:**
- Create a simple JavaFX window with a "Hello World" label.
- Ensure the application runs successfully.

### ✅ 4. **Refine ICC Profile Parsing and Data Handling**
**Tasks:**
- Ensure robust parsing of ICC profiles.
- Implement proper data structures for header and tags.
- Handle different data types within tags (e.g., text, binary, numbers).

### ✅ 5. **Implement Header and Tag Editing Functionality**
**Tasks:**
- Enable editing of header fields.
- Implement hex/text modes for tag data editing.
- Add encoding switch for text-based tags.

### ✅ 6. **Implement Copy Header/Tags Functionality**
**Tasks:**
- Complete the "Copy from another profile" feature to actually copy data, not just display it.

### ✅ 7. **Develop Mimaki-Specific Tools**
**Tasks:**
- Implement multi-encoding media name search/replace.
- Implement Cxf→DevS/CIED conversion.

### ✅ 8. **Enhance UI/UX**
**Tasks:**
- Improve the user interface for better usability.
- Implement the "Mimaki" tab.

### ✅ 9. **Testing and Distribution**
**Tasks:**
- Write comprehensive unit tests.
- Configure JPackage for distribution.

### 10. **Enhance Mimaki Tools**
**Tasks:**
- Implement media name search/replace for `MMK1` and `MMK2` tags.
- Research and implement Cxf→DevS/CIED conversion.

### ✅ 11. **Implement Specialized Tag Editors**
**Tasks:**
- Extend `TagType` and `TagData` to support more ICC standard tag types (e.g., `XYZType`, `curvType`).
- Develop specialized UI components for editing each standard tag type.