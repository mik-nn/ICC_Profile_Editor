# 🎨 ICC Profile Editor

A **cross‑platform ICC profile editor** written in **Java + JavaFX**.  
Designed for color management professionals, print technicians, and developers who need to inspect, edit, and manipulate ICC profiles — with **specialized support for Mimaki printer profiles**.

---

## ✨ Features

### 🗂 Common ICC Editing
- View and edit **ICC profile headers** (128‑byte structure)
- List all **tags** with signature, type, size, and description
- Edit tag data in:
  - **Hex mode** — raw byte editor with offsets
  - **Text mode** — decoded text with selectable encoding (UTF‑8, UTF‑16LE, ASCII)
- Copy **header** or **selected tags** from another ICC profile
- Validate profile structure before saving

### 🖨 Mimaki‑Specific Tools
- Search & replace **media names** in `MMK1`, `MMK2`, etc. tags in **UTF‑8** and **UTF‑16LE**
- Convert `Cxf` tags into two separate tags: `DevS` and `CIED`
- Batch process Mimaki‑specific tag operations

---

## 🖥 User Interface

The application has **two main tabs**:

1. **Common Tab**  
   - Header editor (form view)
   - Tag list (sortable table)
   - Tag editor (Hex/Text switchable with encoding dropdown)

2. **Mimaki Tab**  
   - Media name search/replace (multi‑encoding)
   - Cxf → DevS/CIED conversion tool

---

## 🚀 Getting Started

### Prerequisites
- **Java 17** or newer
- JavaFX runtime (if not using bundled build)

### Clone the Repository
```bash
git clone https://github.com/mik-nn/ICC_Profile_Editor.git
cd ICC_Profile_Editor
