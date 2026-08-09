package com.talentgraph.assessment.judge0;

/**
 * Execution boundary abstraction for sandboxed code execution.
 *
 * <p>TalentGraph core domain depends strictly on this interface, NEVER on raw Judge0 HTTP details.
 * Candidates' source code is NEVER executed inside the backend JVM.
 */
public interface CodeExecutionProvider {

    /** Submit code to sandboxed execution engine and return token */
    Judge0SubmissionResponseDto submitCode(String sourceCode, int languageId, String stdin, String expectedOutput, Double cpuTimeLimitSeconds, Integer memoryLimitMb);

    /** Fetch current execution status/result for a submission token */
    Judge0SubmissionResponseDto getSubmissionResult(String token);

    /** Submit code and poll until execution completes or times out */
    Judge0SubmissionResponseDto submitAndPoll(String sourceCode, int languageId, String stdin, String expectedOutput, Double cpuTimeLimitSeconds, Integer memoryLimitMb);
}
