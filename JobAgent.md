%% JobAgent Use Case Diagram
%% Users interact with AI Agents to search jobs, generate letters, and apply

usecaseDiagram
actor User
rectangle JobAgentSystem {
User --> (Register/Login)
User --> (Upload CV)
User --> (Search Jobs)
User --> (Generate Motivation Letter)
User --> (Apply to Job)
User --> (View Applied Jobs)
}

    (Search Jobs) --> (JobFinderAgent)
    (Generate Motivation Letter) --> (MotivationWriterAgent)
    (Apply to Job) --> (ApplyAgent)
    (Upload CV) --> (CVAnalyzerAgent)