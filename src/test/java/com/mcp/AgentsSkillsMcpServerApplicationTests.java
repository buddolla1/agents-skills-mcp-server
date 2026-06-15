package com.mcp;

import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerProperties;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerStreamableHttpProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AgentsSkillsMcpServerApplicationTests {

    @Autowired
    private McpServerProperties mcpServerProperties;

    @Autowired
    private McpServerStreamableHttpProperties streamableHttpProperties;

    @Test
    void contextLoads() {
    }

    @Test
    void configuresStreamableHttpEndpointUnderApiPath() {
        assertThat(mcpServerProperties.getProtocol())
                .isEqualTo(McpServerProperties.ServerProtocol.STREAMABLE);
        assertThat(streamableHttpProperties.getMcpEndpoint()).isEqualTo("/api/mcp");
    }
}
