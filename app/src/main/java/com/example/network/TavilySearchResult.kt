package com.example.network

data class TavilySource(
    val title: String,
    val url: String,
    val content: String,
    val score: Double = 0.0
)

data class TavilySearchResult(
    val query: String,
    val answer: String? = null,
    val results: List<TavilySource> = emptyList()
)
