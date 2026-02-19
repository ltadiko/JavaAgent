package com.jobagent.jobagent.common.rag;

import org.springframework.ai.vectorstore.SearchRequest;

import java.util.UUID;

/**
 * Utility methods for building tenant-scoped RAG search requests.
 */
public final class RagSearchHelper {

    private RagSearchHelper() {}

    /**
     * Build a tenant-scoped search for CV chunks relevant to a query.
     */
    public static SearchRequest cvChunkSearch(String query, UUID tenantId) {
        return SearchRequest.builder()
                .query(query)
                .topK(RagConstants.DEFAULT_TOP_K)
                .similarityThreshold(RagConstants.DEFAULT_SIMILARITY_THRESHOLD)
                .filterExpression(
                        RagConstants.META_TENANT_ID + " == '" + tenantId + "' && "
                        + RagConstants.META_DOC_TYPE + " == '" + RagConstants.DOC_TYPE_CV_CHUNK + "'")
                .build();
    }

    /**
     * Build a tenant-scoped search for CV chunks of a specific CV.
     */
    public static SearchRequest cvChunkSearchByCv(String query, UUID tenantId, UUID cvId) {
        return SearchRequest.builder()
                .query(query)
                .topK(RagConstants.DEFAULT_TOP_K)
                .similarityThreshold(RagConstants.DEFAULT_SIMILARITY_THRESHOLD)
                .filterExpression(
                        RagConstants.META_TENANT_ID + " == '" + tenantId + "' && "
                        + RagConstants.META_CV_ID + " == '" + cvId + "' && "
                        + RagConstants.META_DOC_TYPE + " == '" + RagConstants.DOC_TYPE_CV_CHUNK + "'")
                .build();
    }
}
