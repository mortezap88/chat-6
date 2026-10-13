package com.example.network

/**
 * Intelligent context-based analyzer that decides whether a user prompt warrants live Tavily / Tabulae web search.
 *
 * Requirements:
 * 1. Selective & Token-Efficient: Do NOT search for every message. Search takes extra network time,
 *    consumes Tavily search credits, and adds large web context token overhead.
 * 2. Explicit triggers: User asks to search ("search the web for...", "google...", "look up online...", etc.).
 * 3. Implicit live internet queries: User implicitly asks for real-time, current, or recent external facts
 *    (e.g., breaking news, today's sports scores, live weather, current stock prices, new TV shows/movies out right now,
 *    is a service down right now, who currently holds an office).
 * 4. General conversation, explanations, math, science, coding, philosophy, personal thoughts, and static history
 *    do NOT trigger web search and respond instantly without extra token burn.
 * 5. Contextual follow-ups: Only continue searching if the PREVIOUS turn was actually web-enhanced AND the user
 *    asks a follow-up directive ("give me the list", "which ones", "like what", "tell me more about that").
 */
object TavilyContextDecider {

    fun shouldSearch(
        query: String,
        recentContext: String? = null,
        wasPreviousTurnWebEnhanced: Boolean = false
    ): Boolean {
        val trimmed = query.trim()
        if (trimmed.isBlank() || trimmed.equals("silence", ignoreCase = true)) return false
        val lower = trimmed.lowercase().removeSuffix("?").removeSuffix(".").trim()

        // 1. Clock, Calendar, and Device time queries -> Never search (read from device)
        val pureClockQueries = listOf(
            "what day is today", "what date is today", "what is today's date", "what's today's date",
            "today's date", "what is the date", "what day is it", "what time is it", "what's the time"
        )
        if (pureClockQueries.any { lower == it || lower == "$it today" }) {
            return false
        }

        // 2. Pure Greetings, Chit-chat, Personal matters & Small talk -> Never search
        val pureGreetings = listOf(
            "hello", "hey", "hi", "good morning", "good night", "good evening", "good afternoon",
            "bye", "see you", "thank you", "thanks", "ok", "okay", "cool", "nice", "awesome",
            "how are you", "who are you", "what's up", "whats up"
        )
        if (pureGreetings.any { lower == it || lower.startsWith("$it ") }) {
            // Only if it doesn't also contain explicit search commands
            if (!lower.contains("search") && !lower.contains("look up") && !lower.contains("google")) {
                return false
            }
        }

        // Personal feelings, diary, grammar check, translation, storytelling
        val personalPhrases = listOf(
            "i feel", "i am feeling", "i'm feeling", "i am sad", "i'm sad", "i am happy", "i'm happy",
            "i'm tired", "i am tired", "my day", "my dog", "my cat", "my friend", "my mom", "my dad",
            "tell me a joke", "write a poem", "tell me a story", "write a story", "roleplay",
            "is this sentence correct", "correct my grammar", "say this more politely",
            "translate this to", "how do i pronounce", "what does this mean"
        )
        if (personalPhrases.any { lower.startsWith(it) || lower == it }) {
            return false
        }

        // 3. Coding, Architecture, and Programming assistance -> LLM does this natively without web search
        // (Unless explicitly asked to search or check the latest 2025/2026 release)
        val isExplicitCodeRequest = lower.startsWith("how to ") || lower.startsWith("write a ") ||
                lower.startsWith("implement ") || lower.startsWith("debug ") ||
                lower.startsWith("explain code") || lower.startsWith("why is this error") ||
                lower.startsWith("fix this")
        val containsExplicitSearch = lower.contains("search") || lower.contains("look up") ||
                lower.contains("google") || lower.contains("browse") || lower.contains("online")

        if (isExplicitCodeRequest && !containsExplicitSearch) {
            return false
        }

        // 4. EXPLICIT search & internet commands -> ALWAYS search
        val explicitSearchPrefixes = listOf(
            "search the web", "search online", "search the internet", "search internet",
            "search for", "search about", "look up online", "look up on the web", "look up",
            "lookup", "google", "browse the web", "browse online", "find online",
            "find on the web", "check online", "check the web", "web search"
        )
        if (explicitSearchPrefixes.any { lower.startsWith(it) || lower.contains(" $it") }) {
            return true
        }

        // 5. CONTEXTUAL FOLLOW-UPS (ONLY when the previous turn was actually web-enhanced)
        if (wasPreviousTurnWebEnhanced) {
            val followUpTriggers = listOf(
                "okay then give me that", "ok then give me that", "give me that", "give me them",
                "give me those", "give me the list", "give me some", "okay give me that",
                "like what", "such as", "which ones", "what shows", "what are they", "what is it",
                "what are those", "show me", "list them", "tell me more", "tell me about that",
                "tell me the names", "name them", "what are the names", "can you give me that",
                "can you tell me"
            )
            if (followUpTriggers.any { lower == it || lower.startsWith(it) || lower.endsWith(it) }) {
                return true
            }
        }

        // 6. IMPLICIT LIVE INTERNET / CURRENT REAL-TIME QUERIES
        // The user doesn't say "search", but asks for real-time information that changes constantly:

        // A. Breaking news & current headlines
        val newsTriggers = listOf(
            "latest news", "breaking news", "recent news", "today's news", "news today",
            "current headlines", "what happened today", "what happened this morning",
            "what is happening with", "what happened with"
        )
        if (newsTriggers.any { lower.contains(it) }) {
            return true
        }

        // B. Real-time status / outages
        val statusTriggers = listOf(
            "is down right now", "is down today", "outage right now", "current status of",
            "server status", "is there an outage"
        )
        if (statusTriggers.any { lower.contains(it) }) {
            return true
        }

        // C. Sports live scores and recent game winners
        val sportsTriggers = listOf(
            "who won yesterday", "who won last night", "who won the game", "game score last night",
            "match score yesterday", "live score", "current score of", "premier league standings",
            "champions league score", "nba score", "nfl score"
        )
        if (sportsTriggers.any { lower.contains(it) }) {
            return true
        }

        // D. Financial / Live Market / Weather
        val liveDataTriggers = listOf(
            "current price of", "stock price of", "crypto price", "bitcoin price right now",
            "weather today", "weather right now", "current weather", "weather forecast today",
            "forecast for this week"
        )
        if (liveDataTriggers.any { lower.contains(it) }) {
            return true
        }

        // E. Current incumbents / political / leadership
        val currentOfficeTriggers = listOf(
            "who is the current president", "who is the current prime minister", "who is the current ceo",
            "current leader of", "current champion"
        )
        if (currentOfficeTriggers.any { lower.contains(it) }) {
            return true
        }

        // F. Recent entertainment releases / trending items (demanding current fresh data)
        val trendingOrLatest = lower.contains("latest") || lower.contains("newest") ||
                lower.contains("trending") || lower.contains("hot new") || lower.contains("right now")
        val mediaTarget = lower.contains("tv show") || lower.contains("tv series") ||
                lower.contains("shows to watch") || lower.contains("movies in theaters") ||
                lower.contains("box office") || lower.contains("streaming on netflix") ||
                lower.contains("streaming now") || lower.contains("new movies") ||
                lower.contains("model released") || lower.contains("ai model released")

        if (trendingOrLatest && mediaTarget) {
            return true
        }

        // G. Specific query about "what's hot" / "what's trending"
        if (lower.contains("what are the hot") || lower.contains("what's hot right now") ||
            lower.contains("trending right now") || lower.contains("trending today")
        ) {
            return true
        }

        // Default: Do NOT search. Save tokens, save latency, and avoid blue answers on ordinary messages.
        return false
    }

    /**
     * Cleans conversational directives (e.g. "search the web about", "look up", "google")
     * from queries before submitting to the web search API to get crisp, accurate search results.
     */
    fun extractEffectiveSearchQuery(query: String, recentContext: String? = null): String {
        val trimmed = query.trim()
        val lower = trimmed.lowercase().removeSuffix("?").removeSuffix(".").trim()

        // Handle vague contextual follow-up queries like "okay then give me that"
        val followUpPatterns = listOf(
            "okay then give me that", "ok then give me that", "give me that", "give me them",
            "give me those", "give me the list", "give me some", "okay give me that",
            "what is the latest ones", "what's the latest ones", "what are the latest ones",
            "the latest ones", "latest ones", "newest ones", "what are the newest ones",
            "what is the latest", "what's the latest", "what are the latest",
            "like what", "which ones", "what shows", "show me", "list them", "name them",
            "tell me", "tell me about that", "tell me more"
        )
        if (followUpPatterns.any { lower == it || lower.startsWith(it) }) {
            val ctxLower = recentContext?.lowercase().orEmpty()
            return when {
                ctxLower.contains("show") || ctxLower.contains("tv") || ctxLower.contains("series") ->
                    "latest hot TV shows streaming right now"
                ctxLower.contains("movie") || ctxLower.contains("film") ->
                    "latest movie releases box office streaming"
                ctxLower.contains("model") || ctxLower.contains("gemini") || ctxLower.contains("ai") || ctxLower.contains("llm") ->
                    "latest AI models releases announcements"
                recentContext?.isNotBlank() == true -> {
                    val keyWords = recentContext.split(Regex("\\s+")).filter { it.length > 4 }.takeLast(4).joinToString(" ")
                    if (keyWords.isNotBlank()) "latest $keyWords" else "latest updates"
                }
                else -> "latest updates"
            }
        }

        val prefixesToStrip = listOf(
            "search the web about",
            "search the web for",
            "search the web",
            "search the internet about",
            "search the internet for",
            "search the internet",
            "search online about",
            "search online for",
            "search online",
            "search for",
            "search",
            "please search about",
            "please search for",
            "please search",
            "can you search the web for",
            "can you search the web about",
            "can you search online for",
            "can you search for",
            "can you search",
            "look up online",
            "look up on the web",
            "look up",
            "lookup",
            "google",
            "find online",
            "find on the web"
        )

        for (prefix in prefixesToStrip) {
            if (lower.startsWith(prefix)) {
                val candidate = trimmed.substring(prefix.length).trim()
                    .removePrefix(":")
                    .removePrefix("-")
                    .removeSuffix("?")
                    .trim()
                if (candidate.isNotBlank() && candidate.length > 2) {
                    return candidate
                }
            }
        }
        return trimmed.removeSuffix("?").trim()
    }
}
