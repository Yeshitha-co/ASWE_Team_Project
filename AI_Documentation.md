# AI Tool Usage Documentation

## 1. Overview

This document describes the use of AI tools during the development of LicenseGraph. It records the tools used, their purposes, representative prompts, how AI-assisted code is identified, and the associated token usage.

AI tools were used to support development and problem-solving. The team made implementation decisions, reviewed AI suggestions, and remains responsible for the final implementation.

## 2. AI Tools Used

The team used **ChatGPT and Claude** as programming assistants during development. 

Their use included:

- **Project planning and workflow:** Outlining the high-level project structure, identifying module responsibilities, and dividing implementation tasks so team members could work independently with minimal overlap and fewer potential merge conflicts.
- **Interface and integration planning:** Establishing shared method signatures, parameter types, return types, and expected behavior between modules so that team members could implement their respective components against consistent interfaces.
- **Implementation assistance:** Helping with repetitive code and routine implementation patterns while following the project's requirements and existing code structure.
- **Design reasoning:** Exploring implementation approaches, considering design trade-offs, and reasoning about interactions and dependencies between modules.
- **Technical understanding:** Explaining unfamiliar APIs, programming concepts, and potential edge cases to support implementation decisions.

## 3. Representative Prompts

The following examples illustrate the types of prompts used during development. They are representative examples, not a complete transcript of actual interactions.

**Project planning and division of work**
- "Help outline the high-level architecture of this project and identify the responsibilities of each module."
- "Suggest a division of implementation tasks so team members can work independently with minimal overlap and fewer merge conflicts."
- "Identify dependencies between modules and suggest an implementation order that supports parallel development."

**Shared interfaces and integration**
- "Help define the interfaces between these modules, including method signatures, parameter types, return types, and expected behavior."
- "Review these proposed interfaces for consistency so that modules agree on the same arguments and return types."
- "Identify assumptions between modules that should be agreed upon before implementation begins."

**Design reasoning and technical understanding**
- "Explain the trade-offs between these implementation approaches and how they affect the other modules."
- "What edge cases should be considered when implementing this functionality?"

These are requests actually given for Module E. They are not a full transcript.

- Implement Module E from the work plan: register a client at `POST /api/clients` with a random API key whose SHA-256 hash is the only stored secret, require `X-API-Key` everywhere else, and map shared exceptions to the Section 10 error body.
- Add a REST Assured base that registers a caller, then cross-client tests: another client's policy, asset, edge, or decision is 404, a body that names something outside the caller is 422, and a missing or unknown key is 401.
- Fix the remaining Module E Checkstyle violations, including tests, without changing behavior and without reformatting Modules A–D.

## 4. Identification of AI-Assisted Code

AI-generated or AI-assisted code, including test code, is identified in the relevant source files using comments such as:

```java
/**
 * AI-assisted:
 */
```

This marker is used for code generated or materially assisted by an AI tool, including code subsequently edited manually. The team reviews the resulting code and remains responsible for its correctness and integration.

## 5. Token Usage

Token usage for both tools is recorded below for the project development period.

| AI tool | Total token usage |
|---|---|
| ChatGPT | TBC |
| Claude | TBC |
| **Combined total** | **To be calculated** |

### Usage by team member

| Team member | Module | AI tool | Token usage |
|---|---|---|---|
| Abhishek Saxena | B. Asset Registry, Persistence and Conflict Checks | Claude | ~217K |
| Progga Paromita Dutta | D. Use Decisions and Audit Log | ChatGPT (OpenAI) | ~200K |
| Yeshitha Bhuvanesh | A.  Policy Registry and Use Rules | ChatGPT (OpenAI) | ~210K |
| Riley Tang | E. Platform and Security | Claude | 200k |
