# LeetSync


LeetSync is a Spring Boot backend designed to organize coding-problem solutions in a GitHub repository. It processes submission data, formats solution files with metadata, and synchronizes them with GitHub while avoiding duplicate commits for identical code.

The backend currently provides REST APIs for submissions from LeetCode, Codeforces, and GeeksforGeeks. Browser-extension integration is planned as the next major phase.

## Features

* **GitHub integration:** Read repository information and create or update files using the GitHub REST API.
* **Multi-platform support:** Submission endpoints for LeetCode, Codeforces, and GeeksforGeeks.
* **Organized solutions:** Store each problem in its own directory, with filenames based on the programming language.
* **Submission metadata:** Include platform, problem details, language, runtime, and memory information when available.
* **Duplicate prevention:** Skip synchronization when the existing solution contains identical source code.
* **Performance-based updates:** Compare available runtime and memory percentiles when deciding whether to replace an existing solution.
* **Automatic README generation:** Generate a README for each problem and maintain a solved-problem count in the root README.
* **Authentication error handling:** Return application-specific errors for GitHub authentication and API failures.

## Tech Stack

* Java 21
* Spring Boot 3.5.6
* Gradle
* PostgreSQL
* Spring Data JPA and Hibernate
* Spring Web and WebFlux
* Jakarta Bean Validation
* GitHub REST API

## Architecture

The backend follows a layered structure:

1. **Controllers:** Expose REST endpoints for authentication, solution management, and submission synchronization.
2. **Services:** Validate submission conditions, generate file paths, format code, and coordinate synchronization.
3. **GitHub client:** Communicates with the GitHub REST API to read and write repository files.
4. **Persistence layer:** Uses Spring Data JPA and PostgreSQL for solution-related data.

### Submission workflow

1. A client sends submission details to the backend.
2. The backend checks the request and verifies the target GitHub repository.
3. It generates the problem directory and formats the solution with metadata.
4. It checks whether the solution already exists.
5. Identical code is skipped. Different code may replace the existing solution according to the available performance information.
6. When required, the backend updates the problem README and the root solved-problem count.
7. The API returns the synchronization status and relevant file path.

## API Reference

The backend runs on port `8080` by default.

### Authentication

| Method | Endpoint                    | Purpose                      |
| ------ | --------------------------- | ---------------------------- |
| POST   | `/api/auth/github/validate` | Validate a GitHub credential |

### Solution management and GitHub operations

| Method | Endpoint                                               | Purpose                                  |
| ------ | ------------------------------------------------------ | ---------------------------------------- |
| POST   | `/api/solutions`                                       | Create a solution record                 |
| GET    | `/api/solutions`                                       | Retrieve solution records                |
| GET    | `/api/solutions/{id}`                                  | Retrieve a solution by ID                |
| PUT    | `/api/solutions/{id}`                                  | Update a solution record                 |
| DELETE | `/api/solutions/{id}`                                  | Delete a solution record                 |
| GET    | `/api/solutions/github/user`                           | Retrieve the authenticated GitHub user   |
| GET    | `/api/solutions/github/repository/{owner}/{repo}`      | Retrieve repository information          |
| GET    | `/api/solutions/github/repository/{owner}/{repo}/file` | Read a repository file                   |
| PUT    | `/api/solutions/github/repository/{owner}/{repo}/file` | Create or update a repository file       |
| POST   | `/api/solutions/github/sync/{owner}/{repo}`            | Synchronize a solution with a repository |

The file endpoints accept additional query parameters such as `path` and, for reading, `branch`.

### Submission synchronization

| Method | Endpoint               | Purpose                                |
| ------ | ---------------------- | -------------------------------------- |
| POST   | `/api/sync/submission` | Synchronize a submission               |
| POST   | `/api/sync/leetcode`   | Synchronize a LeetCode submission      |
| POST   | `/api/sync/codeforces` | Synchronize a Codeforces submission    |
| POST   | `/api/sync/gfg`        | Synchronize a GeeksforGeeks submission |

The synchronization endpoints use an `Authorization` header and an `X-GitHub-Owner` header.

├── 0002-add-two-numbers
│   ├── solution.java
│   └── README.md
## Example Submission Request

└── 0003-longest-substring-without-repeating-characters
A submission request uses the following JSON structure:

```json
{
  "source": "LEETCODE",
  "problemId": "1",
  "problemTitle": "Two Sum",
  "language": "Java",
  "code": "class Solution { }",
  "repository": "leetcode-solutions",
  "branch": "main",
  "runtime": "2 ms",
  "runtimePercentile": 95.2,
  "memory": "42 MB",
  "memoryPercentile": 78.1,
  "accepted": true
}
```

The repository and branch fields allow the client to specify the destination. When the branch is omitted, the backend uses the repository's default branch.

Runtime, memory, percentile, and acceptance fields are optional in the request model. The synchronization service rejects explicitly unaccepted submissions, but currently permits a missing acceptance value.

### Example Response

```json
{
  "status": "CREATED",
  "message": "Auto-commit: Solved 1. Two Sum",
  "path": "0001-two-sum/solution.java",
  "commitSha": "example-commit-sha",
  "changed": true
}
```

Possible statuses include `CREATED`, `UPDATED`, and `SKIPPED`.

## Repository Structure

Solutions are organized by coding platform. Each problem directory uses a zero-padded problem number and a sanitized problem title, and contains the solution file and a problem-specific README.

```text
LeetCode Solutions/
└── 0001-two-sum/
    ├── solution.java
    └── README.md

Codeforces Solutions/
└── 0001-example-problem/
    ├── solution.cpp
    └── README.md

GeeksforGeeks Solutions/
└── 0001-example-problem/
    ├── solution.py
    └── README.md
```

Global stats: 2 Problems Solved
The examples illustrate the directory layout; actual filenames and extensions depend on the problem and programming language.

### Supported language extensions

| Language                        | Extension |
| ------------------------------- | --------- |
| Java                            | `.java`   |
| C                               | `.c`      |
| C++                             | `.cpp`    |
| Python                          | `.py`     |
| JavaScript                      | `.js`     |
| TypeScript                      | `.ts`     |
| C#                              | `.cs`     |
| Kotlin                          | `.kt`     |
| Go                              | `.go`     |
| Rust                            | `.rs`     |
| Swift                           | `.swift`  |
| PHP                             | `.php`    |
| Other or unrecognized languages | `.txt`    |

## Getting Started

### Prerequisites

* Java 21
* PostgreSQL
* Git

### 1. Clone the repository

```bash
git clone https://github.com/aarnav-code/LeetSync.git
cd LeetSync/backend
```

Adjust the directory change if you clone the backend repository directly rather than the parent repository.

### 2. Create the database

Create a PostgreSQL database named `leetsync`.

```sql
CREATE DATABASE leetsync;
```

### 3. Configure environment variables

Configure these environment variables before starting the application:

```text
DB_USERNAME=your_postgres_username
DB_PASSWORD=your_postgres_password
```

The application reads these values from the environment rather than storing database credentials in `application.yml`.

### 4. Start the backend

On Windows:

```powershell
.\gradlew.bat bootRun
```

On macOS or Linux:

```bash
./gradlew bootRun
```

The application is configured to listen on port `8080`.

### 5. Run tests

On Windows:

```powershell
.\gradlew.bat test
```

On macOS or Linux:

```bash
./gradlew test
```

## Configuration

The main application configuration is located at:

`src/main/resources/application.yml`

Relevant settings include:

* PostgreSQL connection details through environment variables.
* Server port, defaulting to `8080`.
* GitHub API base URL and user-agent configuration.
* CORS origin patterns for browser-extension and local development clients.

## Current Status

The project is focused on building the backend foundation for automated coding-submission synchronization.

**Implemented in the current backend:**

* REST endpoints for solution management and submission synchronization.
* GitHub repository and file operations.
* Solution formatting and metadata generation.
* Duplicate-code checks and performance-based update logic.
* Per-problem README generation and root solved-problem statistics.

**Planned next:**

* Build the browser extension interface.
* Detect accepted submissions on supported coding platforms.
* Extract solution code and submission metadata from platform pages.
* Connect the extension to the backend.
* Improve user-facing error handling and authentication recovery.

## Contributing

Suggestions, bug reports, and improvements are welcome. As the project evolves, the goal is to make saving and organizing coding solutions on GitHub as seamless as possible.

---

*LeetSync is a work in progress.*

<!-- LEETSYNC:COUNTED:LEETCODE:7 -->
