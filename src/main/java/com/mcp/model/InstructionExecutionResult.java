package com.mcp.model;

import java.util.List;

public record InstructionExecutionResult(
        String operation,
        String instructionType,
        String instructionName,
        String instructionRelativePath,
        String instructionDescription,
        List<String> declaredTools,
        String targetName,
        String targetRelativePath,
        String targetType,
        String instructionContent,
        String targetContent,
        String note,
        String executionOutput
) {
}
