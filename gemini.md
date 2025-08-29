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

---

### 1. **Project Setup**
**Tasks:**
- Create Maven JavaFX project structure:
  ```bash
  mvn archetype:generate \
    -DgroupId=com.mik.icc \
    -DartifactId=icc-profile-editor \
    -DarchetypeArtifactId=maven-archetype-quickstart \
    -DinteractiveMode=false
