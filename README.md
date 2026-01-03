# 🍽 Mammame: Pregnancy Food Safety Search (English UI)

<p align="center">
  <img src="https://img.shields.io/badge/Compose-Multiplatform-blue?style=for-the-badge" alt="Compose Multiplatform">
  <img src="https://img.shields.io/badge/Platform-Web-green?style=for-the-badge" alt="Web">
  <img src="https://img.shields.io/badge/UI-English-orange?style=for-the-badge" alt="English UI">
  <img src="https://img.shields.io/badge/AI-Gemini%203%20Flash-purple?style=for-the-badge" alt="Gemini AI">
</p>

**Mammame** is a food safety search application designed to help pregnant users
quickly understand whether specific foods are generally considered safe,
conditional, or recommended to avoid—based on **multiple publicly available sources**.

In addition to dataset-based search, Mammame provides an **AI-assisted fallback**
when structured data is unavailable or inconclusive.
Using a lightweight large language model, the system generates short,
food-specific summaries to support user understanding without offering
prescriptive medical advice.

Instead of presenting a single definitive answer, Mammame highlights
**areas of agreement and disagreement across sources**, allowing users to make
informed decisions based on context, credibility, and AI-assisted explanations.

> This repository contains the **English UI version** of Mammame.

---

## 🌟 Key Features

### 🌟 Key Features

- **Food-Centered Search**  
  Search pregnancy-related food safety information by food name.

- **Multi-Source Comparison**  
  Aggregates guidance from medical, governmental, corporate, and community sources.

- **Reliability-Aware Summaries**  
  Groups sources by credibility level (high / medium / low) and summarizes dominant opinions
  while highlighting disagreements.

- **AI-Assisted Fallback & Summaries**  
  When no matching dataset entries are found, an AI-powered backend (Google Gemini 3 Flash)
  generates short, food-specific summaries using a structured, non-prescriptive prompt.

- **Verdict-Based Filtering**  
  Filter sources by verdict: Safe / Conditional / Caution / Avoid

- **Responsive Web UI**  
  Built with Compose Multiplatform (Web) to ensure consistent behavior across screen sizes.

---

## ⚙️ How It Works

Mammame follows a lightweight, transparent aggregation pipeline:

1. **User Query**  
   The user searches for a food item (e.g., *“sushi”*, *“coffee”*, *“mint”*).

2. **Dataset Matching**  
   The query is matched against a curated CSV-based dataset of food safety entries.

3. **Source Grouping & Deduplication**  
   - Entries are grouped by source (URL or publisher).
   - Only the strongest warning per source is retained.

4. **Reliability-Based Ranking**  
   Sources are categorized into:
   - High reliability (e.g., medical or institutional)
   - Medium reliability (e.g., corporate or press)
   - Low reliability (e.g., personal or community)

5. **Summary Generation**  
   A short, human-readable summary highlights dominant patterns and notable disagreements.

---

## 🛠 Tech Stack

### Language
- Kotlin

### Framework
- Compose Multiplatform (Web)

### Data
- CSV-based ingestion
- Client-side parsing and ranking

### UI
- Responsive layout
- Reliability indicators and source-level links

---

## 🤖 AI-Assisted Guidance

Mammame integrates a lightweight large language model (**Google Gemini 3 Flash**)
as a **fallback mechanism**, activated only when structured dataset matches
are unavailable.

The AI component:
- Receives a food-specific query and structured prompt
- Generates short, contextual summaries
- Avoids prescriptive or diagnostic language
- Streams responses directly into the UI for transparency

AI-generated content is always presented as **supplementary context**, not as
a replacement for source-based information.

---

## 🧩 System Overview

```text
## 🧩 System Overview

```text
┌──────────────────────────┐
│       User Search        │
│     "Is coffee safe?"    │
└─────────────┬────────────┘
              ↓
┌──────────────────────────┐
│   Food Dataset Matching  │
│  (CSV-based ingestion)   │
└─────────────┬────────────┘
              ↓
┌──────────────────────────┐
│  Source Deduplication    │
│  + Verdict Normalization │
└─────────────┬────────────┘
              ↓
┌──────────────────────────┐
│  Reliability Grouping    │
│  (High / Mid / Low)      │
└─────────────┬────────────┘
              ↓
┌──────────────────────────┐
│  Verdict Summary + UI    │
│  Filters & Source Links  │
└──────────────────────────┘
              │
              │  (No matching results found)
              ▼
┌──────────────────────────┐
│   AI Fallback Trigger    │
│  "AI guideline" button   │
│     becomes visible      │
└─────────────┬────────────┘
              ↓
┌──────────────────────────┐
│  Gemini-based AI Server  │
│ (gemini-3-flash-preview) │
│                          │
│  • Structured prompt     │
│  • Food-specific query   │
│  • Non-prescriptive tone │
└─────────────┬────────────┘
              ↓
┌──────────────────────────┐
│  AI-generated Summary    │
│  (Short, contextual,     │
│   non-medical guidance)  │
└─────────────┬────────────┘
              ↓
┌──────────────────────────┐
│  Streaming UI Response   │
│  Displayed in context    │
│  alongside search UI     │
└──────────────────────────┘

```
```md
When no matching food entries are found in the dataset, Mammame provides an
**AI-assisted fallback**.

Instead of returning an empty result, the interface surfaces an optional
AI button. When triggered, the user query is sent to a custom backend powered
by **Google Gemini 3 Flash**, which generates a short, food-specific summary
based on a structured prompt.

The AI response is:
- Non-prescriptive and informational
- Explicitly scoped to the queried food
- Displayed as a contextual reference, not a definitive answer
```

---

## ⚠️ Disclaimer

Mammame does **not** provide medical advice.

All information presented in this application is aggregated from publicly
available sources for general reference purposes only.
Food safety decisions during pregnancy should always be made in consultation
with qualified healthcare professionals.

---

## 📜 License & Attribution

This project was initially bootstrapped from the
[JetBrains Compose Multiplatform Template](https://github.com/JetBrains/compose-multiplatform-template),
which is licensed under the **Apache License 2.0**.

Significant modifications and original work have been made on top of the template.

---

## 🚧 Project Status

This project is under active development and experimentation.

- Data sources may expand or change
- UI and ranking logic may evolve
- No guarantees are made regarding completeness or correctness

A Korean-language version of Mammame is actively maintained and shared separately (https://mammame.app).
That version operates **without AI-assisted summaries**.


---

## 📬 Contact

Questions, suggestions, or collaboration ideas are welcome.

📧 **mammame.app@gmail.com**  
or feel free to open an issue on GitHub.

