
# API Automation Assistant

A Java + Spring Boot project that combines Spring AI, RAG (vector retrieval), and MCP-based browser automation (Playwright + Chrome/CDP). This started as a fun build inspired by a hackathon prototype, with the goal of recreating the same agentic workflow in a cleaner, more modular way using Java/Spring abstractions.

## Why this exists

In the hackathon version (Python), we had RAG working, but automation was the real hurdle. Prompt-only automation was fragile, and free models came with strict requests-per-minute and tokens-per-minute limits, which made repeated retries and long prompts painful.

This project explores a better approach: use an MCP (Model Context Protocol) server as the tool execution layer. The LLM requests structured tool calls, the MCP server executes them deterministically via Playwright, and the LLM iterates until the task is complete.

## What it does

### Part 1: RAG ingestion and retrieval
- Ingests documents into a vector store
- Queries those documents to return step-by-step troubleshooting or instructions
- Keeps the repository clean: proprietary/internal documents are not committed

### Part 2: Task execution via Playwright (MCP-powered)
- Exposes a controller endpoint that accepts a task request body
- Builds a prompt containing:
  - relevant steps (optionally fetched from RAG)
  - available tool definitions
- Runs a loop where:
  - the LLM requests tool calls (navigate, click, etc.)
  - the server forwards tool calls to the Playwright MCP server
  - the MCP server controls Chrome via CDP and returns results
  - results are sent back to the LLM until it returns a final response

## Architecture

### Components
- **User**
  - Submits a task or issue description via API
- **Spring Boot Server**
  - Orchestrates RAG retrieval, prompt construction, and the agent loop
  - Routes tool call requests/results between the LLM and MCP server
- **Vector Store (RAG)**
  - Stores embeddings and returns relevant instructions
- **LLM (Gemini in the original concept; configurable)**
  - Decides when to call tools and when the task is complete
- **Playwright MCP Server**
  - Executes tool requests using Playwright
- **Chrome Browser**
  - Controlled via CDP (Chrome DevTools Protocol)

### Execution flow
1. User submits a task request
2. Server optionally queries the vector store for relevant steps
3. Server sends a prompt to the LLM (steps + tool definitions)
4. LLM requests a tool call (e.g., navigate, click)
5. Server forwards the tool call to the Playwright MCP server
6. MCP server executes the action in Chrome and returns results
7. Server sends tool results back to the LLM
8. Steps 4–7 repeat until the LLM returns `TASK_COMPLETE`
9. Server returns the final resolution summary to the user

## Diagrams

This Repository includes "Our Sketch.pdf". It shows my reasoning and architecture thinking behind this project


## Setup and running (fill in your project-specific values)

### Prerequisites

* Java (your version)
* Maven or Gradle
* Chrome or Chromium installed
* Playwright for Java dependencies
* LLM provider credentials (Gemini or another provider via Spring AI)
* Vector store backend configured in the application

### Configuration

Create `application.yml` or `application.properties` with:

* LLM provider settings (API key, model name)
* Vector store settings (backend type, endpoint, index/collection)
* MCP server settings (host/port and transport: stdio/http depending on your implementation)
* Optional guardrails (domain allowlist, action limits)

### Run

1. Start the Playwright MCP server
2. Start the Spring Boot application
3. Send a POST request to the task endpoint with a task JSON body
4. Read the final resolution summary returned by the API

