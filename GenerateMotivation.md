sequenceDiagram
participant User
participant Frontend
participant Backend
participant JobFinderAgent
participant CVAnalyzerAgent
participant MotivationWriterAgent
participant ApplyAgent
participant Database

    User->>Frontend: Upload CV
    Frontend->>Backend: POST /uploadCV
    Backend->>CVAnalyzerAgent: Extract skills, experience
    CVAnalyzerAgent-->>Backend: Return parsed CV
    Backend->>Database: Save CV and extracted info
    Backend-->>Frontend: CV upload success

    User->>Frontend: Search Jobs (title, location)
    Frontend->>Backend: GET /search-jobs
    Backend->>JobFinderAgent: Find jobs matching criteria
    JobFinderAgent-->>Backend: Return job list
    Backend-->>Frontend: Display job list

    User->>Frontend: Select job & generate motivation letter
    Frontend->>Backend: POST /generate-motivation-letter
    Backend->>MotivationWriterAgent: Generate letter from CV + Job Description
    MotivationWriterAgent-->>Backend: Return motivation letter
    Backend-->>Frontend: Display letter for review
    User->>Frontend: Confirm letter

    User->>Frontend: Apply to job
    Frontend->>Backend: POST /apply-job
    Backend->>ApplyAgent: Fill form with user info, CV, letter
    ApplyAgent-->>Backend: Submission result
    Backend->>Database: Save application status
    Backend-->>Frontend: Show application success/failure