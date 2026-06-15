package com.mcp.tools;

import org.junit.jupiter.api.Test;
import org.springaicommunity.mcp.annotation.McpArg;
import org.springaicommunity.mcp.annotation.McpTool;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class AgentsSkillsMcpToolsAnnotationTest {

    @Test
    void exposesAllToolMethodsWithMcpToolAnnotations() {
        Set<String> annotatedMethods = Arrays.stream(AgentsSkillsMcpTools.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(McpTool.class))
                .map(Method::getName)
                .collect(Collectors.toSet());

        assertThat(annotatedMethods).containsExactlyInAnyOrder(
                "listAgents",
                "getAgent",
                "searchAgents",
                "listSkills",
                "getSkill",
                "searchSkills",
                "listInstructions",
                "getInstruction",
                "searchAll",
                "applySkillToFile",
                "runSkill",
                "runAgentTools"
        );
    }

    @Test
    void parameterizedToolMethodsDeclareMcpArguments() {
        assertThat(annotatedParameterNames("getAgent")).containsExactly("fileNameOrRelativePath");
        assertThat(annotatedParameterNames("searchAgents")).containsExactly("keyword");
        assertThat(annotatedParameterNames("getSkill")).containsExactly("fileNameOrRelativePath");
        assertThat(annotatedParameterNames("searchSkills")).containsExactly("keyword");
        assertThat(annotatedParameterNames("getInstruction")).containsExactly("fileNameOrRelativePath");
        assertThat(annotatedParameterNames("searchAll")).containsExactly("keyword");
        assertThat(annotatedParameterNames("applySkillToFile")).containsExactly("skillFileOrRelativePath", "filePath");
        assertThat(annotatedParameterNames("runSkill")).containsExactly("skillFileOrRelativePath", "filePath");
        assertThat(annotatedParameterNames("runAgentTools")).containsExactly("agentFileOrRelativePath", "filePath");
    }

    private List<String> annotatedParameterNames(String methodName) {
        Method method = Arrays.stream(AgentsSkillsMcpTools.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst()
                .orElseThrow();

        return Arrays.stream(method.getParameters())
                .map(parameter -> parameter.getAnnotation(McpArg.class))
                .filter(annotation -> annotation != null)
                .map(McpArg::name)
                .toList();
    }
}
