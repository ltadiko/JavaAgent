package com.jobagent.jobagent.common.rag;

/**
 * Constants for RAG document metadata and types.
 * Used when ingesting documents into and querying from PgVectorStore.
 */
public final class RagConstants {

    private RagConstants() {}

    // ─── Metadata keys ───────────────────────────────────────────────
    public static final String META_TENANT_ID = "tenant_id";
    public static final String META_CV_ID = "cv_id";
    public static final String META_SECTION = "section";
    public static final String META_DOC_TYPE = "doc_type";

    // ─── Document types ──────────────────────────────────────────────
    public static final String DOC_TYPE_CV_CHUNK = "cv_chunk";
    public static final String DOC_TYPE_COMPANY_KNOWLEDGE = "company_knowledge"; // v2

    // ─── CV sections ─────────────────────────────────────────────────
    public static final String SECTION_SKILLS = "SKILLS";
    public static final String SECTION_EXPERIENCE = "EXPERIENCE";
    public static final String SECTION_EDUCATION = "EDUCATION";
    public static final String SECTION_SUMMARY = "SUMMARY";
    public static final String SECTION_PROJECTS = "PROJECTS";

    // ─── Default RAG parameters ──────────────────────────────────────
    public static final int DEFAULT_TOP_K = 5;
    public static final double DEFAULT_SIMILARITY_THRESHOLD = 0.65;
}
