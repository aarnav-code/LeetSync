# LeetSync

LeetSync is a Chrome extension and Spring Boot backend that automatically synchronizes accepted LeetCode submissions with GitHub.

Instead of manually copying solutions after solving a problem, LeetSync retrieves the submitted code, checks the submission verdict, and sends eligible submissions to the backend for synchronization. Solutions are organized into individual problem directories, helping developers maintain a structured coding portfolio on GitHub.

## Demo

**LeetSync in Action**

<!-- Demo video will be embedded here. -->

*A short walkthrough demonstrating submission detection, synchronization, and the resulting GitHub files will be added here.*

## Features

* **LeetCode Integration:** Retrieve submission details and source code from LeetCode.
* **Accepted-Submission Filtering:** Prevent unsuccessful submissions from being synchronized.
* **GitHub Integration:** Synchronize solutions with a configured GitHub repository.
* **Automatic Commits:** Commit synchronized solutions to GitHub.
* **Organized Solutions:** Store each problem in its own directory under `LeetCode Solutions/`.
* **Language-Aware Filenames:** Use appropriate file extensions for solution files.
* **Duplicate Handling:** Avoid unnecessary synchronization when the existing solution contains identical code.
* **Problem Documentation:** Maintain problem-specific README files alongside solutions.

## Tech Stack

* Java 21
* Spring Boot
* Gradle
* PostgreSQL
* Spring Web
* Spring Data JPA
* Chrome Extension APIs
* GitHub REST API

## Architecture

LeetSync consists of two main components.

### Chrome Extension

The Chrome extension integrates with LeetCode submission pages to retrieve submission details and source code, identify the submission verdict, and make eligible submissions available for synchronization.

### Spring Boot Backend

The backend receives submission data, validates the request, prepares the destination path, checks the existing solution, and coordinates synchronization with GitHub.

### Submission Workflow

1. Submit a solution on LeetCode.
2. Open LeetSync on the relevant submission page.
3. The extension retrieves the submission code and verdict.
4. The extension sends an eligible submission to the backend.
5. The backend prepares the solution file and checks whether synchronization is necessary.
6. The backend creates or updates the relevant files in GitHub.
7. The synchronization result indicates whether the solution was created, updated, or skipped.

## API Reference

The backend runs on port `8080` by default.

### GitHub Authentication

| Method | Endpoint                    | Purpose                     |
| ------ | --------------------------- | --------------------------- |
| POST   | `/api/auth/github/validate` | Validate GitHub credentials |

### Solution Synchronization

| Method | Endpoint                                    | Purpose                                         |
| ------ | ------------------------------------------- | ----------------------------------------------- |
| POST   | `/api/solutions/github/sync/{owner}/{repo}` | Synchronize a solution with a GitHub repository |

### Example Submission Request

```json
{
  "problemNumber": 1,
  "problemTitle": "Two Sum",
  "language": "Java",
  "code": "class Solution { }",
  "accepted": true
}
```

This example illustrates the main submission fields. The actual request should contain the complete solution code and match the backend request model.

### Example Response

```json
{
  "status": "CREATED",
  "message": "Solution synchronized successfully",
  "path": "LeetCode Solutions/0001-two-sum/solution.java",
  "commitSha": "example-commit-sha",
  "changed": true
}
```

This response is illustrative. Actual response messages, status values, paths, and commit hashes depend on the backend implementation and synchronization result.

## Repository Structure

The repository contains the backend source code and the collection of synchronized LeetCode solutions.

```text
LeetSync/
├── src/
│   └── main/
├── gradle/
├── build.gradle
├── settings.gradle
├── gradlew
├── gradlew.bat
├── README.md
└── LeetCode Solutions/
    ├── 0001-two-sum/
    │   ├── solution.java
    │   └── README.md
    ├── 0002-add-two-numbers/
    │   ├── solution.java
    │   └── README.md
    ├── 0003-longest-substring-without-repeating-characters/
    │   ├── solution.java
    │   └── README.md
    └── ...
```

The directory tree is illustrative and omits other backend files and synchronized solutions.

Each problem directory uses a zero-padded problem number followed by a sanitized problem title. Solution files use an extension appropriate to the programming language.

## Getting Started

### Prerequisites

* Java 21
* PostgreSQL
* Git
* Google Chrome
* A GitHub account and an appropriately scoped GitHub access token

### 1. Clone the Repository

```bash
git clone https://github.com/aarnav-code/LeetSync.git
cd LeetSync
```

### 2. Create the Database

Create a PostgreSQL database named `leetsync`.

```sql
CREATE DATABASE leetsync;
```

### 3. Configure Database Credentials

Configure the environment variables expected by the application:

```text
DB_USERNAME=your_postgres_username
DB_PASSWORD=your_postgres_password
```

Use the database settings configured in `src/main/resources/application.yml`. Never commit database credentials or GitHub access tokens to the repository.

### 4. Start the Backend

On Windows:

```powershell
.\gradlew.bat bootRun
```

On macOS or Linux:

```bash
./gradlew bootRun
```

The backend is configured to listen on port `8080` by default.

### 5. Load the Chrome Extension

1. Open `chrome://extensions` in Chrome.
2. Enable **Developer mode**.
3. Click **Load unpacked**.
4. Select the directory containing the extension's `manifest.json`.
5. Open LeetCode and test the extension on a submission page.

Ensure that the backend is running and the extension is configured to communicate with it.

### 6. Run Backend Tests

On Windows:

```powershell
.\gradlew.bat test
```

On macOS or Linux:

```bash
./gradlew test
```

## Current Status

### Implemented

* LeetCode browser extension integration.
* Submission code and verdict retrieval.
* Accepted-submission filtering.
* Spring Boot synchronization backend.
* GitHub repository synchronization and automatic commits.
* Organized solution directories.
* Duplicate-solution handling.

### Future Improvements

* Support for additional coding platforms, including Codeforces and GeeksforGeeks.
* Additional runtime and memory performance metadata.
* Improved configuration and user-facing error handling.
* Expanded automated testing and documentation.

## Contributing

Suggestions, bug reports, and improvements are welcome. LeetSync aims to make saving and organizing coding solutions on GitHub simpler and more consistent.

---

*LeetSync is an ongoing project.*
