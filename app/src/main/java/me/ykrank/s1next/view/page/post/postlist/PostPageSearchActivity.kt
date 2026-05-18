package me.ykrank.s1next.view.page.post.postlist

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.format.DateUtils
import android.text.style.BackgroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ImageButton
import android.widget.SearchView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.ykrank.androidtools.util.ImeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.ykrank.s1next.App
import me.ykrank.s1next.R
import me.ykrank.s1next.data.api.model.Post
import me.ykrank.s1next.data.api.model.Thread
import me.ykrank.s1next.data.api.model.wrapper.PostsWrapper
import me.ykrank.s1next.data.cache.api.ApiCacheConstants
import me.ykrank.s1next.data.cache.api.ApiCacheFlow
import me.ykrank.s1next.data.cache.biz.CacheBiz
import me.ykrank.s1next.util.HtmlUtils
import me.ykrank.s1next.view.activity.BaseActivity
import org.jsoup.Jsoup
import kotlin.math.max
import kotlin.math.min

class PostPageSearchActivity : BaseActivity() {

    private lateinit var searchView: SearchView
    private lateinit var recyclerView: RecyclerView
    private lateinit var stateView: TextView
    private lateinit var scopeSwitch: SwitchCompat
    private lateinit var scopeTitleView: TextView
    private lateinit var scopeHintView: TextView
    private lateinit var adapter: ResultAdapter

    private var payload: SearchPayload? = null
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_page_search)

        payload = pendingPayload
        searchView = findViewById(R.id.search_view)
        recyclerView = findViewById(R.id.search_results)
        stateView = findViewById(R.id.search_state)
        scopeSwitch = findViewById(R.id.search_scope_switch)
        scopeTitleView = findViewById(R.id.search_scope_title)
        scopeHintView = findViewById(R.id.search_scope_hint)

        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).navigationIcon = null
        findViewById<View>(R.id.spinner).visibility = View.GONE
        findViewById<ImageButton>(R.id.searchback).setOnClickListener {
            finishAfterTransition()
        }

        adapter = ResultAdapter { result ->
            setResult(
                Activity.RESULT_OK,
                Intent()
                    .putExtra(EXTRA_PAGE, result.pageNum)
                    .putExtra(EXTRA_POSITION, result.adapterPosition)
            )
            finishAfterTransition()
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        setupScopeSwitch()
        setupSearchView()
        val lastQuery = searchCacheKey?.let { lastQueryByThread[it] }.orEmpty()
        if (lastQuery.isNotBlank()) {
            searchView.setQuery(lastQuery, false)
            renderQuery(lastQuery)
        } else {
            renderQuery("")
        }
    }

    override fun onDestroy() {
        searchJob?.cancel()
        if (isFinishing) {
            pendingPayload = null
        }
        super.onDestroy()
    }

    private fun setupSearchView() {
        searchView.queryHint = getString(R.string.post_page_search_hint)
        searchView.inputType = InputType.TYPE_CLASS_TEXT
        searchView.imeOptions = searchView.imeOptions or EditorInfo.IME_ACTION_SEARCH or
                EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_FLAG_NO_FULLSCREEN
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                renderQuery(query.orEmpty())
                ImeUtils.hideIme(searchView)
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                renderQuery(newText.orEmpty())
                return true
            }
        })
        searchView.post {
            searchView.requestFocus()
            ImeUtils.showIme(searchView)
        }
    }

    private fun setupScopeSwitch() {
        val currentPageOnly = searchCacheKey
            ?.let { lastCurrentPageOnlyByThread[it] }
            ?: true
        scopeSwitch.isChecked = !currentPageOnly
        updateScopeText(null)
        scopeSwitch.setOnCheckedChangeListener { _, isChecked ->
            updateLastCurrentPageOnly(!isChecked)
            updateScopeText(null)
            renderQuery(searchView.query?.toString().orEmpty())
        }
    }

    private fun updateScopeText(cachedPageCount: Int?) {
        val currentPayload = payload
        if (scopeSwitch.isChecked) {
            scopeTitleView.setText(R.string.post_page_search_scope_cached)
            scopeHintView.text = if (cachedPageCount != null) {
                getString(R.string.post_page_search_scope_cached_hint_loaded, cachedPageCount)
            } else {
                getString(R.string.post_page_search_scope_cached_hint)
            }
        } else {
            scopeTitleView.setText(R.string.post_page_search_scope_current)
            scopeHintView.text = getString(
                R.string.post_page_search_scope_current_hint,
                currentPayload?.currentPageNum ?: 1
            )
        }
    }

    private fun renderQuery(query: String) {
        searchJob?.cancel()
        val trimmedQuery = query.trim()
        val currentPayload = payload
        updateLastQuery(trimmedQuery)
        when {
            currentPayload == null || currentPayload.currentPosts.isEmpty() -> {
                adapter.submitList(emptyList())
                recyclerView.visibility = View.GONE
                stateView.visibility = View.VISIBLE
                stateView.setText(R.string.post_page_search_no_data)
            }

            trimmedQuery.isEmpty() -> {
                adapter.submitList(emptyList())
                recyclerView.visibility = View.GONE
                stateView.visibility = View.VISIBLE
                stateView.setText(R.string.post_page_search_empty_query)
            }

            else -> {
                val includeCachedPages = scopeSwitch.isChecked
                searchJob = lifecycleScope.launch {
                    val searchPages = withContext(Dispatchers.IO) {
                        buildSearchPages(currentPayload, includeCachedPages)
                    }
                    val results = withContext(Dispatchers.Default) {
                        search(searchPages.pages, trimmedQuery)
                    }
                    updateScopeText(searchPages.cachedPageCount)
                    adapter.submitList(results)
                    if (results.isEmpty()) {
                        recyclerView.visibility = View.GONE
                        stateView.visibility = View.VISIBLE
                        stateView.text = getString(R.string.post_page_search_no_results, trimmedQuery)
                    } else {
                        recyclerView.visibility = View.VISIBLE
                        stateView.visibility = View.VISIBLE
                        stateView.text = getString(
                            R.string.post_page_search_results_title,
                            results.size,
                            trimmedQuery
                        )
                    }
                }
            }
        }
    }

    private fun buildSearchPages(payload: SearchPayload, includeCachedPages: Boolean): SearchPages {
        val currentPage = SearchPage(
            pageNum = payload.currentPageNum,
            thread = payload.thread,
            posts = payload.currentPosts
        )
        if (!includeCachedPages) {
            return SearchPages(listOf(currentPage), null)
        }

        val threadId = payload.threadId ?: payload.thread?.id ?: return SearchPages(listOf(currentPage), 1)
        val totalPages = max(payload.totalPages, payload.currentPageNum).coerceAtLeast(1)
        val cacheBiz = App.appComponent.cacheBiz
        val pages = linkedMapOf(payload.currentPageNum to currentPage)
        payload.memoryPages.forEach { snapshot ->
            if (snapshot.posts.isNotEmpty()) {
                pages[snapshot.pageNum] = SearchPage(
                    pageNum = snapshot.pageNum,
                    thread = snapshot.thread ?: payload.thread,
                    posts = snapshot.posts
                )
            }
        }
        pages[payload.currentPageNum] = currentPage
        for (pageNum in 1..totalPages) {
            if (pages.containsKey(pageNum)) {
                continue
            }
            val wrapper = readCachedPage(cacheBiz, threadId, pageNum) ?: continue
            val posts = wrapper.data?.postList
            if (!posts.isNullOrEmpty()) {
                pages[pageNum] = SearchPage(
                    pageNum = pageNum,
                    thread = wrapper.data?.postListInfo ?: payload.thread,
                    posts = ArrayList(posts)
                )
            }
        }
        return SearchPages(pages.values.sortedBy { it.pageNum }, pages.size)
    }

    private fun readCachedPage(
        cacheBiz: CacheBiz,
        threadId: String,
        pageNum: Int
    ): PostsWrapper? {
        val groupedCache = cacheBiz.getTextZipNewest(
            listOf(ApiCacheConstants.CacheType.Posts.type, threadId, pageNum.toString())
        )
        val keyedCache = groupedCache ?: cacheBiz.getTextZipByKey(
            ApiCacheFlow.getKey(
                App.appComponent.user.uid,
                ApiCacheConstants.CacheType.Posts,
                listOf(threadId, pageNum)
            )
        )
        val json = keyedCache?.decodeZipString ?: return null
        return runCatching {
            App.preAppComponent.jsonMapper.readValue(json, PostsWrapper::class.java)
        }.getOrNull()
    }

    private fun search(pages: List<SearchPage>, query: String): List<PostSearchResult> {
        val normalizedQuery = foldCase(compactWhitespace(query))
        if (normalizedQuery.isEmpty()) {
            return emptyList()
        }
        val results = pages.flatMap { page ->
            page.posts.mapIndexedNotNull { index, post ->
                val source = buildSearchText(page, post)
                if (source.isEmpty()) {
                    return@mapIndexedNotNull null
                }
                val match = findBestMatch(foldCase(source), normalizedQuery)
                    ?: return@mapIndexedNotNull null
                PostSearchResult(
                    pageNum = page.pageNum,
                    adapterPosition = index,
                    floor = post.number?.let { "#$it" } ?: "#${index + 1}",
                    author = post.authorName.orEmpty(),
                    dateText = buildDateText(post),
                    snippet = buildSnippet(source, match),
                    score = match.score
                )
            }
        }
        return results.sortedWith(
            compareBy<PostSearchResult> { it.score }
                .thenBy { it.pageNum }
                .thenBy { it.adapterPosition }
        )
    }

    private fun buildSearchText(page: SearchPage, post: Post): String {
        val parts = mutableListOf<String>()
        if (page.pageNum == 1 && post.isFirst) {
            page.thread?.title?.takeIf { it.isNotBlank() }?.let { parts += it }
        }
        post.number?.takeIf { it.isNotBlank() }?.let { parts += it }
        post.authorName?.takeIf { it.isNotBlank() }?.let { parts += it }
        val replyText = HtmlUtils.unescapeHtml(Jsoup.parse(post.reply.orEmpty()).text()).orEmpty()
        if (replyText.isNotBlank()) {
            parts += replyText
        }
        return compactWhitespace(parts.joinToString(" "))
    }

    private fun buildDateText(post: Post): String {
        if (post.dateTime <= 0) {
            return ""
        }
        return DateUtils.getRelativeDateTimeString(
            this,
            post.dateTime * 1000L,
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.DAY_IN_MILLIS,
            0
        ).toString()
    }

    private fun findBestMatch(text: String, query: String): TextSearchMatch? {
        val exactMatch = findExactMatch(text, query)
        if (exactMatch != null) {
            return exactMatch
        }

        val tokenMatch = findTokenMatch(text, query)
        val fuzzyMatch = findFuzzySubsequenceMatch(text, query)
        val editDistanceMatch = findNearestEditDistanceMatch(text, query)
        return listOfNotNull(tokenMatch, fuzzyMatch, editDistanceMatch).minByOrNull { it.score }
    }

    private fun findExactMatch(text: String, query: String): TextSearchMatch? {
        val index = text.indexOf(query)
        if (index < 0) {
            return null
        }
        return TextSearchMatch(
            score = index / EXACT_POSITION_SCORE_STEP,
            start = index,
            end = index + query.length,
            ranges = listOf(index until index + query.length)
        )
    }

    private fun findTokenMatch(text: String, query: String): TextSearchMatch? {
        val tokens = query.split(' ')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (tokens.size <= 1) {
            return null
        }

        val ranges = mutableListOf<IntRange>()
        var searchFrom = 0
        for (token in tokens) {
            val tokenIndex = text.indexOf(token, searchFrom)
            if (tokenIndex < 0) {
                return null
            }
            ranges += tokenIndex until tokenIndex + token.length
            searchFrom = tokenIndex + token.length
        }

        val start = ranges.minOf { it.first }
        val end = ranges.maxOf { it.last } + 1
        val tokenLength = tokens.sumOf { it.length }
        val gap = (end - start - tokenLength).coerceAtLeast(0)
        return TextSearchMatch(
            score = TOKEN_MATCH_SCORE + gap + start / TOKEN_POSITION_SCORE_STEP,
            start = start,
            end = end,
            ranges = ranges
        )
    }

    private fun findFuzzySubsequenceMatch(text: String, query: String): TextSearchMatch? {
        val compactQuery = query.replace(" ", "")
        if (compactQuery.length < MIN_FUZZY_QUERY_LENGTH) {
            return null
        }

        var bestMatch: TextSearchMatch? = null
        for (startIndex in text.indices) {
            if (text[startIndex] != compactQuery[0]) {
                continue
            }

            val indices = mutableListOf<Int>()
            var textIndex = startIndex
            var queryIndex = 0
            while (textIndex < text.length && queryIndex < compactQuery.length) {
                if (text[textIndex] == compactQuery[queryIndex]) {
                    indices += textIndex
                    queryIndex++
                }
                textIndex++
            }

            if (queryIndex != compactQuery.length) {
                continue
            }

            val end = indices.last() + 1
            val gap = end - startIndex - compactQuery.length
            val maxGap = max(MIN_FUZZY_GAP, compactQuery.length * FUZZY_GAP_MULTIPLIER)
            if (gap > maxGap) {
                continue
            }

            val match = TextSearchMatch(
                score = FUZZY_MATCH_SCORE + gap * FUZZY_GAP_SCORE +
                        startIndex / FUZZY_POSITION_SCORE_STEP,
                start = startIndex,
                end = end,
                ranges = indices.map { it..it }
            )
            if (bestMatch == null || match.score < bestMatch.score) {
                bestMatch = match
            }
        }
        return bestMatch
    }

    private fun findNearestEditDistanceMatch(text: String, query: String): TextSearchMatch? {
        val compactQuery = query.replace(" ", "")
        if (compactQuery.length !in MIN_EDIT_QUERY_LENGTH..MAX_EDIT_QUERY_LENGTH) {
            return null
        }

        val maxDistance = max(1, compactQuery.length / EDIT_DISTANCE_RATIO)
        val minWindow = max(1, compactQuery.length - maxDistance)
        val maxWindow = compactQuery.length + maxDistance
        val searchableLength = min(text.length, MAX_EDIT_DISTANCE_TEXT_LENGTH)
        var bestMatch: TextSearchMatch? = null

        for (startIndex in 0 until searchableLength) {
            if (text[startIndex].isWhitespace()) {
                continue
            }
            for (windowLength in minWindow..maxWindow) {
                val end = startIndex + windowLength
                if (end > searchableLength) {
                    break
                }
                val candidate = text.substring(startIndex, end)
                val distance = boundedLevenshteinDistance(compactQuery, candidate, maxDistance)
                if (distance > maxDistance) {
                    continue
                }
                val match = TextSearchMatch(
                    score = EDIT_DISTANCE_MATCH_SCORE + distance * EDIT_DISTANCE_SCORE +
                            startIndex / EDIT_POSITION_SCORE_STEP,
                    start = startIndex,
                    end = end,
                    ranges = listOf(startIndex until end)
                )
                if (bestMatch == null || match.score < bestMatch.score) {
                    bestMatch = match
                }
            }
        }
        return bestMatch
    }

    private fun boundedLevenshteinDistance(source: String, target: String, maxDistance: Int): Int {
        var previous = IntArray(target.length + 1) { it }
        var current = IntArray(target.length + 1)

        for (sourceIndex in 1..source.length) {
            current[0] = sourceIndex
            var rowMin = current[0]
            for (targetIndex in 1..target.length) {
                val substitutionCost =
                    if (source[sourceIndex - 1] == target[targetIndex - 1]) 0 else 1
                val value = min(
                    min(previous[targetIndex] + 1, current[targetIndex - 1] + 1),
                    previous[targetIndex - 1] + substitutionCost
                )
                current[targetIndex] = value
                rowMin = min(rowMin, value)
            }
            if (rowMin > maxDistance) {
                return rowMin
            }
            val nextPrevious = current
            current = previous
            previous = nextPrevious
        }
        return previous[target.length]
    }

    private fun buildSnippet(source: String, match: TextSearchMatch): CharSequence {
        val snippetStart = (match.start - SNIPPET_CONTEXT).coerceAtLeast(0)
        val snippetEnd = (match.end + SNIPPET_CONTEXT).coerceAtMost(source.length)
        val prefix = if (snippetStart > 0) "..." else ""
        val suffix = if (snippetEnd < source.length) "..." else ""
        val builder = SpannableStringBuilder()
            .append(prefix)
            .append(source.substring(snippetStart, snippetEnd))
            .append(suffix)
        val highlightColor = ColorUtils.setAlphaComponent(
            mThemeManager.gentleAccentColor,
            HIGHLIGHT_BACKGROUND_ALPHA
        )
        val bodyOffset = prefix.length

        match.ranges.forEach { range ->
            val start = range.first.coerceAtLeast(snippetStart)
            val end = (range.last + 1).coerceAtMost(snippetEnd)
            if (start >= end) {
                return@forEach
            }
            val spanStart = bodyOffset + start - snippetStart
            val spanEnd = bodyOffset + end - snippetStart
            builder.setSpan(
                BackgroundColorSpan(highlightColor),
                spanStart,
                spanEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            builder.setSpan(
                StyleSpan(Typeface.BOLD),
                spanStart,
                spanEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return builder
    }

    private fun compactWhitespace(value: String): String {
        return value.replace(Regex("\\s+"), " ").trim()
    }

    private fun foldCase(value: String): String {
        return buildString(value.length) {
            value.forEach { append(it.lowercaseChar()) }
        }
    }

    private fun updateLastQuery(query: String) {
        val key = searchCacheKey ?: return
        if (query.isBlank()) {
            lastQueryByThread.remove(key)
        } else {
            lastQueryByThread[key] = query
        }
    }

    private fun updateLastCurrentPageOnly(currentPageOnly: Boolean) {
        val key = searchCacheKey ?: return
        lastCurrentPageOnlyByThread[key] = currentPageOnly
    }

    private val searchCacheKey: String?
        get() = payload?.threadId ?: payload?.thread?.id

    private class ResultAdapter(
        private val onClick: (PostSearchResult) -> Unit
    ) : RecyclerView.Adapter<ResultViewHolder>() {

        private var items: List<PostSearchResult> = emptyList()

        fun submitList(newItems: List<PostSearchResult>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_post_page_search_result, parent, false)
            return ResultViewHolder(view, onClick)
        }

        override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size
    }

    private class ResultViewHolder(
        itemView: View,
        private val onClick: (PostSearchResult) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val floorView: TextView = itemView.findViewById(R.id.search_result_floor)
        private val metaView: TextView = itemView.findViewById(R.id.search_result_meta)
        private val snippetView: TextView = itemView.findViewById(R.id.search_result_snippet)

        fun bind(result: PostSearchResult) {
            floorView.text = itemView.context.getString(
                R.string.post_page_search_result_floor,
                result.pageNum,
                result.floor
            )
            metaView.text = listOf(result.author, result.dateText)
                .filter { it.isNotBlank() }
                .joinToString("  ")
            snippetView.text = result.snippet
            itemView.setOnClickListener {
                onClick(result)
            }
        }
    }

    private data class PostSearchResult(
        val pageNum: Int,
        val adapterPosition: Int,
        val floor: String,
        val author: String,
        val dateText: String,
        val snippet: CharSequence,
        val score: Int
    )

    private data class TextSearchMatch(
        val score: Int,
        val start: Int,
        val end: Int,
        val ranges: List<IntRange>
    )

    private data class SearchPayload(
        val thread: Thread?,
        val threadId: String?,
        val currentPageNum: Int,
        val totalPages: Int,
        val currentPosts: ArrayList<Post>,
        val memoryPages: ArrayList<PageSnapshot>
    )

    private data class SearchPage(
        val pageNum: Int,
        val thread: Thread?,
        val posts: List<Post>
    )

    private data class SearchPages(
        val pages: List<SearchPage>,
        val cachedPageCount: Int?
    )

    data class PageSnapshot(
        val pageNum: Int,
        val thread: Thread?,
        val posts: ArrayList<Post>
    )

    companion object {
        const val EXTRA_PAGE = "page"
        const val EXTRA_POSITION = "position"

        private const val SNIPPET_CONTEXT = 44
        private const val HIGHLIGHT_BACKGROUND_ALPHA = 72
        private const val EXACT_POSITION_SCORE_STEP = 64
        private const val TOKEN_MATCH_SCORE = 30
        private const val TOKEN_POSITION_SCORE_STEP = 64
        private const val MIN_FUZZY_QUERY_LENGTH = 2
        private const val MIN_FUZZY_GAP = 4
        private const val FUZZY_GAP_MULTIPLIER = 2
        private const val FUZZY_MATCH_SCORE = 80
        private const val FUZZY_GAP_SCORE = 4
        private const val FUZZY_POSITION_SCORE_STEP = 96
        private const val MIN_EDIT_QUERY_LENGTH = 3
        private const val MAX_EDIT_QUERY_LENGTH = 24
        private const val EDIT_DISTANCE_RATIO = 3
        private const val MAX_EDIT_DISTANCE_TEXT_LENGTH = 4000
        private const val EDIT_DISTANCE_MATCH_SCORE = 120
        private const val EDIT_DISTANCE_SCORE = 24
        private const val EDIT_POSITION_SCORE_STEP = 96

        private val lastQueryByThread = mutableMapOf<String, String>()
        private val lastCurrentPageOnlyByThread = mutableMapOf<String, Boolean>()
        private var pendingPayload: SearchPayload? = null

        fun startForResult(
            fragment: Fragment,
            requestCode: Int,
            thread: Thread?,
            threadId: String?,
            pageNum: Int,
            totalPages: Int,
            posts: ArrayList<Post>,
            memoryPages: ArrayList<PageSnapshot> = arrayListOf()
        ) {
            pendingPayload = SearchPayload(
                thread,
                threadId,
                pageNum,
                totalPages,
                posts,
                memoryPages
            )
            fragment.startActivityForResult(
                Intent(fragment.requireContext(), PostPageSearchActivity::class.java),
                requestCode
            )
        }
    }
}
