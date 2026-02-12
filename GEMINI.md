# Gemini Context: PDF Management Platform

This document provides a summary of the project structure, technologies, and development conventions to guide future interactions with the Gemini CLI.

## Project Overview

This is a comprehensive PDF Management Platform designed for ingesting, processing, and searching PDF documents. It features a modular, multi-service backend built with Java and Spring Boot, and a modern frontend built with React.

### Architecture

The application follows a microservices-like architecture, with different modules responsible for specific business capabilities.

*   **Backend (Java/Spring Boot):**
    *   **`pdf-inbound-api`**: The entry point for all client interactions, exposing a REST API for document uploads and management.
    *   **`pdf-application`**: The core service that orchestrates the main business logic, including ingestion, OCR, and enrichment workflows.
    *   **`pdf-core`**: Contains shared components like DTOs, mappers, events, and utilities used across multiple modules.
    *   **`pdf-outbound-database`**: Manages data persistence using Spring Data JPA with a PostgreSQL database.
    *   **`pdf-infrastructure-security`**: Handles authentication and authorization using Spring Security and JWT.
    *   **`pdf-outbound-ocr`**: Integrates with OCR libraries (Tess4J/PDFBox) to extract text from documents.
    *   **`pdf-outbound-llm`**: Connects to an LLM service (Ollama) to enrich document content (e.g., extract titles, dates, tags).
    *   **`pdf-outbound-search`**: Manages document indexing and searching with Elasticsearch.

*   **Frontend (React/TypeScript):**
    *   Located in the `pdf-frontend` directory.
    *   Built with **Vite**, **React**, and **TypeScript**.
    *   Styled using **Tailwind CSS** and **shadcn/ui**.
    *   Uses **TanStack React Query** for server state management and **React Router** for navigation.

*   **Infrastructure (Docker):**
    *   The `docker-compose-infra.yml` file defines the necessary backing services:
        *   PostgreSQL (`postgres:15`)
        *   Elasticsearch (`8.13.0`)
        *   Ollama (for local LLM tasks)
        *   pgAdmin (for database administration)

## Building and Running the Project

### Backend

1.  **Build all modules:**
    From the project root directory, run:
    ```bash
    mvn clean package
    ```

2.  **Start infrastructure:**
    ```bash
    docker compose -f docker-compose-infra.yml up -d
    ```

3.  **Run the API service:**
    To start the main application entry point:
    ```bash
    mvn spring-boot:run -pl pdf-inbound-api
    ```
    The API will be available at `http://localhost:8080`.

### Frontend

1.  **Navigate to the frontend directory:**
    ```bash
    cd pdf-frontend
    ```

2.  **Install dependencies:**
    ```bash
    npm install
    ```

3.  **Run the development server:**
    ```bash
    npm run dev
    ```
    The frontend will be available at `http://localhost:5173` (or another port if 5173 is busy).

## Development Conventions

### Testing

The project employs a layered testing strategy, detailed in `docs/TESTING_STRATEGY.md`.

*   **Backend Testing:**
    *   **Unit Tests:** Pure JUnit/Mockito tests for business logic, with no Spring context.
    *   **Slice Tests:** Spring Boot slice tests (`@DataJpaTest`, `@WebMvcTest`) for repositories and controllers.
    *   **Integration Tests:** Use **Testcontainers** for targeted tests against real infrastructure (PostgreSQL, Elasticsearch).
    *   **Test Command:** `mvn verify` will run all tests and generate a JaCoCo code coverage report.

*   **Frontend Testing:**
    *   **Component/Unit Tests:** Use **Vitest** and **React Testing Library**.
    *   **Test Command:** `npm test`

### Code Style & Linting

*   **Backend:** The code uses Lombok and MapStruct extensively to reduce boilerplate. Standard Java conventions apply.
*   **Frontend:** Code style is enforced by **ESLint**. Run `npm run lint` to check for issues.
