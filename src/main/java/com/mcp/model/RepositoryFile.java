package com.mcp.model;

public record RepositoryFile(
        String name,
        String relativePath,
        String type,
        long sizeBytes
) {
}
