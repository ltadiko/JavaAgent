#!/bin/bash
cd /Users/mac-Z01LTATI/IdeaProjects/JavaAgent/jobagent-ui
npx playwright test --reporter=line > /Users/mac-Z01LTATI/IdeaProjects/JavaAgent/e2e-result.log 2>&1
echo "EXIT_CODE=$?" >> /Users/mac-Z01LTATI/IdeaProjects/JavaAgent/e2e-result.log
