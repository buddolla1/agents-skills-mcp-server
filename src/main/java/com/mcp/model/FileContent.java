package com.mcp.model;

public record FileContent(
        String name,
        String relativePath,
        String type,
        String content
) {
}
