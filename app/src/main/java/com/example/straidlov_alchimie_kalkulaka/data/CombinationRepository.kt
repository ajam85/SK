package com.example.straidlov_alchimie_kalkulaka.data

import android.content.Context
import com.example.straidlov_alchimie_kalkulaka.model.CombinationStatus
import com.example.straidlov_alchimie_kalkulaka.model.GridCombination
import com.example.straidlov_alchimie_kalkulaka.model.GridUtils
import com.example.straidlov_alchimie_kalkulaka.model.StateConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.w3c.dom.Element
import java.io.File
import java.io.StringReader
import java.text.Normalizer
import java.util.Locale
import kotlin.random.Random
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource

data class CombinationStats(
    val total: Int = GridUtils.TOTAL_COMBINATIONS,
    val untested: Int = GridUtils.TOTAL_COMBINATIONS,
    val nonfunctional: Int = 0,
    val functional: Int = 0
) {
    val totalTested: Int get() = nonfunctional + functional
    val percentTested: Float get() = if (total > 0) (totalTested.toFloat() / total) * 100f else 0f
}

class CombinationRepository(private val context: Context) {

    private val fileName = "combinations_data.json"
    private val coordinateSystemVersion = 2
    private val bundledSeedFileName = "initial_combinations.json"
    private val bundledSeedVersion = 1
    private var loadedBundledSeedVersion = 0
    private var firebaseSyncStarted = false
    private val firebaseSync = FirebaseSyncClient(context)
    private val firebaseSyncScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val firebaseSyncState: StateFlow<FirebaseSyncState> = firebaseSync.state
    val firebaseSyncError: StateFlow<String?> = firebaseSync.lastError
    val firebaseSyncLog: StateFlow<List<FirebaseSyncLogEntry>> = firebaseSync.log

    // Sparse or fast storage for modified entries (id -> GridCombination)
    private val combinationMap = mutableMapOf<Int, GridCombination>()

    private val _stateConfig = MutableStateFlow(StateConfig())
    val stateConfig: StateFlow<StateConfig> = _stateConfig.asStateFlow()

    private val _stats = MutableStateFlow(CombinationStats())
    val stats: StateFlow<CombinationStats> = _stats.asStateFlow()

    private val _dataVersion = MutableStateFlow(0L)
    val dataVersion: StateFlow<Long> = _dataVersion.asStateFlow()

    suspend fun loadData() = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) {
            val seeded = applyBundledSeedIfNeeded()
            recalculateStats()
            if (seeded) _dataVersion.value = System.currentTimeMillis()
            saveData()
            startFirebaseSyncIfNeeded()
            return@withContext
        }

        try {
            val jsonString = file.readText()
            parseAndLoadJson(jsonString)
            val seeded = applyBundledSeedIfNeeded()
            if (seeded) {
                recalculateStats()
                _dataVersion.value = System.currentTimeMillis()
                saveData()
            } else {
                // Persists migrated timestamps so legacy zero values do not return on restart.
                saveData()
            }
            startFirebaseSyncIfNeeded()
        } catch (e: Exception) {
            e.printStackTrace()
            recalculateStats()
            startFirebaseSyncIfNeeded()
        }
    }

    suspend fun saveData() = withContext(Dispatchers.IO) {
        try {
            val jsonString = exportJson()
            val file = File(context.filesDir, fileName)
            file.writeText(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCombination(id: Int): GridCombination {
        val validId = id.coerceIn(0, GridUtils.TOTAL_COMBINATIONS - 1)
        return combinationMap[validId] ?: GridCombination(id = validId)
    }

    suspend fun setStatus(
        id: Int,
        status: CombinationStatus,
        note: String? = null,
        author: String? = null
    ) {
        val validId = id.coerceIn(0, GridUtils.TOTAL_COMBINATIONS - 1)
        val existing = combinationMap[validId]
        val currentNote = note ?: existing?.note ?: ""
        val currentAuthor = author ?: existing?.author ?: ""

        if (status == CombinationStatus.UNTESTED && currentNote.isBlank() && currentAuthor.isBlank()) {
            combinationMap.remove(validId)
        } else {
            combinationMap[validId] = GridCombination(
                id = validId,
                status = status,
                note = currentNote,
                author = currentAuthor,
                updatedAt = System.currentTimeMillis(),
                updatedBy = firebaseSync.currentUserId().ifBlank { "local" }
            )
        }

        recalculateStats()
        _dataVersion.value = System.currentTimeMillis()
        saveData()
        combinationMap[validId]?.let(firebaseSync::publish)
    }

    suspend fun setNote(id: Int, note: String) {
        val validId = id.coerceIn(0, GridUtils.TOTAL_COMBINATIONS - 1)
        val existing = getCombination(validId)
        setStatus(validId, existing.status, note, existing.author)
    }

    suspend fun setAuthor(id: Int, author: String) {
        val validId = id.coerceIn(0, GridUtils.TOTAL_COMBINATIONS - 1)
        val existing = getCombination(validId)
        setStatus(validId, existing.status, existing.note, author)
    }

    /**
     * Returns the highest-ranked untested recipe. Solved recipes are skipped,
     * so both functional and nonfunctional recipes disappear from suggestions.
     */
    fun findRecommendedUntested(fallbackId: Int = 0): Int {
        var bestId = -1
        var bestScore = Int.MIN_VALUE

        for (id in 0 until GridUtils.TOTAL_COMBINATIONS) {
            val item = combinationMap[id]
            if (item != null && item.status != CombinationStatus.UNTESTED) continue

            val score = GridUtils.recommendationScore(GridUtils.decodeIndex(id))
            if (score > bestScore) {
                bestScore = score
                bestId = id
            }
        }

        return if (bestId >= 0) bestId else fallbackId.coerceIn(0, GridUtils.TOTAL_COMBINATIONS - 1)
    }

    fun findRandomUntested(): Int {
        val untestedIds = mutableListOf<Int>()
        // Sample quickly or pick randomly
        for (i in 0 until 500) {
            val randId = Random.nextInt(GridUtils.TOTAL_COMBINATIONS)
            val item = combinationMap[randId]
            if (item == null || item.status == CombinationStatus.UNTESTED) {
                return randId
            }
        }
        // Fallback search
        for (i in 0 until GridUtils.TOTAL_COMBINATIONS) {
            val item = combinationMap[i]
            if (item == null || item.status == CombinationStatus.UNTESTED) {
                untestedIds.add(i)
                if (untestedIds.size >= 100) break
            }
        }
        return if (untestedIds.isNotEmpty()) untestedIds.random() else Random.nextInt(GridUtils.TOTAL_COMBINATIONS)
    }

    fun getSavedCombinations(
        filterStatus: CombinationStatus? = null,
        searchQuery: String = ""
    ): List<GridCombination> {
        return combinationMap.values
            .filter { entry ->
                val matchesStatus = (filterStatus == null || entry.status == filterStatus)
                val matchesQuery = if (searchQuery.isBlank()) true else {
                    val query = searchQuery.trim().lowercase()
                    val patternStr = GridUtils.formatPatternString(GridUtils.decodeIndex(entry.id))
                    (entry.id + 1).toString().contains(query) ||
                            entry.note.lowercase().contains(query) ||
                            entry.author.lowercase().contains(query) ||
                            patternStr.contains(query)
                }
                matchesStatus && matchesQuery
            }
            .sortedWith(
                compareByDescending<GridCombination> {
                    it.status == CombinationStatus.FUNCTIONAL
                }.thenByDescending { it.updatedAt }
            )
    }

    suspend fun saveStateConfig(config: StateConfig) {
        _stateConfig.value = config
        saveData()
    }

    fun forceSync() {
        startFirebaseSyncIfNeeded()
        combinationMap.values
            .filter { it.status != CombinationStatus.UNTESTED }
            .forEach(firebaseSync::publish)
    }

    fun exportJson(): String {
        val root = JSONObject()

        val config = _stateConfig.value
        val configObj = JSONObject()
        configObj.put("state0Name", config.state0Name)
        configObj.put("state1Name", config.state1Name)
        configObj.put("state2Name", config.state2Name)
        configObj.put("state3Name", config.state3Name)
        configObj.put("defaultAuthor", config.defaultAuthor)
        configObj.put("state0ColorHex", config.state0ColorHex)
        configObj.put("state1ColorHex", config.state1ColorHex)
        configObj.put("state2ColorHex", config.state2ColorHex)
        configObj.put("state3ColorHex", config.state3ColorHex)
        root.put("config", configObj)
        root.put("coordinateSystemVersion", coordinateSystemVersion)
        root.put("bundledSeedVersion", loadedBundledSeedVersion)

        val array = JSONArray()
        for (entry in combinationMap.values) {
            if (entry.status != CombinationStatus.UNTESTED || entry.note.isNotBlank() || entry.author.isNotBlank()) {
                val item = JSONObject()
                item.put("id", entry.id)
                item.put("status", entry.status.ordinal)
                item.put("note", entry.note)
                item.put("author", entry.author)
                item.put("updatedAt", entry.updatedAt)
                item.put("updatedBy", entry.updatedBy)
                array.put(item)
            }
        }
        root.put("entries", array)
        return root.toString(2)
    }

    suspend fun importJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            parseAndLoadJson(jsonString)
            if (applyBundledSeedIfNeeded()) {
                _dataVersion.value = System.currentTimeMillis()
            }
            saveData()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Imports XML rows using the spreadsheet columns: code, order, success,
     * note and author/discoverer. Imported rows are merged with local data and
     * are queued for Firebase synchronization after the import.
     */
    suspend fun importXml(xmlString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val importedEntries = parseXmlEntries(xmlString)
            if (importedEntries.isEmpty()) return@withContext false

            importedEntries.forEach { imported ->
                val local = combinationMap[imported.id]
                val shouldApply = local == null ||
                    (local.status == CombinationStatus.UNTESTED &&
                        local.note.isBlank() && local.author.isBlank()) ||
                    imported.updatedAt >= local.updatedAt

                if (shouldApply) {
                    combinationMap[imported.id] = imported
                }
            }

            recalculateStats()
            _dataVersion.value = System.currentTimeMillis()
            saveData()
            startFirebaseSyncIfNeeded()
            importedEntries
                .filter { it.status != CombinationStatus.UNTESTED }
                .forEach(firebaseSync::publish)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun parseXmlEntries(xmlString: String): List<GridCombination> {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            isExpandEntityReferences = false
            runCatching {
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                setFeature("http://xml.org/sax/features/external-general-entities", false)
                setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            }
        }
        val document = factory.newDocumentBuilder().parse(InputSource(StringReader(xmlString)))
        val root = document.documentElement ?: return emptyList()
        parseSpreadsheetXmlEntries(root).takeIf { it.isNotEmpty() }?.let { return it }
        val rowNames = setOf("kombinace", "combination", "entry", "item", "record", "row", "polozka", "zaznam")
        val rows = mutableListOf<Element>()
        val rootName = xmlName(root)
        if (rootName in rowNames && hasXmlField(root)) rows += root

        val elements = root.getElementsByTagName("*")
        for (i in 0 until elements.length) {
            val element = elements.item(i) as? Element ?: continue
            if (xmlName(element) in rowNames && hasXmlField(element)) rows += element
        }

        val candidates = if (rows.isNotEmpty()) rows else listOf(root)
        return candidates.mapNotNull(::parseXmlRow)
            .associateBy { it.id }
            .values
            .toList()
    }

    private fun parseSpreadsheetXmlEntries(root: Element): List<GridCombination> {
        val rowElements = mutableListOf<Element>()
        if (xmlName(root) == "row") rowElements += root
        val descendants = root.getElementsByTagName("*")
        for (i in 0 until descendants.length) {
            val element = descendants.item(i) as? Element ?: continue
            if (xmlName(element) == "row") rowElements += element
        }
        if (rowElements.isEmpty()) return emptyList()

        val rows = rowElements.map { row ->
            val cells = mutableListOf<String>()
            val children = row.childNodes
            for (i in 0 until children.length) {
                val cell = children.item(i) as? Element ?: continue
                if (xmlName(cell) == "cell") cells += cell.textContent.trim()
            }
            cells
        }
        val headerIndex = rows.indexOfFirst { row ->
            row.any {
                normalizeXmlName(it) in setOf("kod", "9mistnykod", "devitimistnykod", "poradi", "zdabylakombinaceuspesna", "poznamka")
            }
        }
        if (headerIndex < 0) return emptyList()

        val headers = rows[headerIndex].map(::normalizeXmlName)
        return rows.drop(headerIndex + 1).mapNotNull { cells ->
            val values = headers.mapIndexedNotNull { index, header ->
                cells.getOrNull(index)?.let { header to it }
            }.toMap()
            parseXmlValues(values)
        }.associateBy { it.id }.values.toList()
    }

    private fun hasXmlField(element: Element): Boolean {
        val fieldNames = setOf(
            "kod", "code", "devitimistnykod", "9mistnykod", "poradi", "order", "index",
            "id", "stav", "status", "uspesnost", "zdabylakombinaceuspesna", "success",
            "poznamka", "note", "autor", "author", "objevitel", "discoverer"
        )
        val attributes = element.attributes
        for (i in 0 until attributes.length) {
            if (normalizeXmlName(attributes.item(i).nodeName) in fieldNames) return true
        }
        val descendants = element.getElementsByTagName("*")
        for (i in 0 until descendants.length) {
            if (normalizeXmlName(descendants.item(i).nodeName) in fieldNames) return true
        }
        return false
    }

    private fun parseXmlRow(element: Element): GridCombination? {
        val values = linkedMapOf<String, String>()
        val attributes = element.attributes
        for (i in 0 until attributes.length) {
            val attribute = attributes.item(i)
            values[normalizeXmlName(attribute.nodeName)] = attribute.nodeValue.trim()
        }
        val descendants = element.getElementsByTagName("*")
        for (i in 0 until descendants.length) {
            val field = descendants.item(i) as? Element ?: continue
            val text = field.textContent?.trim().orEmpty()
            if (text.isNotBlank()) values[normalizeXmlName(field.nodeName)] = text
        }

        return parseXmlValues(values)
    }

    private fun parseXmlValues(values: Map<String, String>): GridCombination? {
        val code = firstXmlValue(values, "kod", "code", "devitimistnykod", "9mistnykod")
        val order = firstXmlValue(values, "poradi", "order", "position", "index")?.toIntOrNull()
        val id = when {
            !code.isNullOrBlank() && code.length == 9 && code.all { it in '0'..'3' } -> {
                GridUtils.encodeGrid(code.map { it - '0' }.toIntArray())
            }
            order != null && order in 1..GridUtils.TOTAL_COMBINATIONS -> order - 1
            else -> firstXmlValue(values, "id")?.toIntOrNull()
                ?.takeIf { it in 0 until GridUtils.TOTAL_COMBINATIONS }
        } ?: return null

        var note = firstXmlValue(values, "poznamka", "note", "description").orEmpty()
        var author = firstXmlValue(values, "autor", "author", "objevitel", "discoverer").orEmpty()
        if (author.isBlank() && note.contains(" - objevitel ", ignoreCase = true)) {
            val parts = note.split(" - objevitel ", limit = 2)
            note = parts[0].trim()
            author = parts.getOrNull(1).orEmpty().trim()
        }

        val status = parseXmlStatus(
            firstXmlValue(values, "stav", "status", "uspesnost", "zdabylakombinaceuspesna", "success")
        )
        if (status == CombinationStatus.UNTESTED && note.isBlank() && author.isBlank()) return null

        val updatedAt = firstXmlValue(values, "updatedat", "updated", "cas", "timestamp")
            ?.toLongOrNull()
            ?: System.currentTimeMillis()
        val updatedBy = firstXmlValue(values, "updatedby", "writer", "source")
            .orEmpty()
            .ifBlank { "xml-import" }

        return GridCombination(
            id = id,
            status = status,
            note = note,
            author = author,
            updatedAt = updatedAt,
            updatedBy = updatedBy
        )
    }

    private fun parseXmlStatus(value: String?): CombinationStatus {
        val raw = value?.trim().orEmpty()
        if (raw.isBlank()) return CombinationStatus.UNTESTED
        if (raw.contains("✓") || raw.equals("ano", true) || raw.equals("yes", true)) {
            return CombinationStatus.FUNCTIONAL
        }
        if (raw.equals("x", true) || raw.equals("ne", true) || raw.equals("no", true)) {
            return CombinationStatus.NONFUNCTIONAL
        }
        return when (normalizeXmlName(raw)) {
            "functional", "success", "successful", "uspesne", "funkcni", "positive", "ok" -> CombinationStatus.FUNCTIONAL
            "nonfunctional", "failed", "failure", "neuspesne", "nefunkcni", "negative" -> CombinationStatus.NONFUNCTIONAL
            else -> CombinationStatus.UNTESTED
        }
    }

    private fun firstXmlValue(values: Map<String, String>, vararg names: String): String? {
        return names.asSequence()
            .map { normalizeXmlName(it) }
            .mapNotNull { values[it] }
            .firstOrNull { it.isNotBlank() }
    }

    private fun xmlName(element: Element): String {
        return normalizeXmlName(element.localName ?: element.nodeName.substringAfterLast(':'))
    }

    private fun normalizeXmlName(value: String): String {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .lowercase(Locale.ROOT)
            .replace("[^a-z0-9]".toRegex(), "")
    }

    private fun parseAndLoadJson(jsonString: String) {
        combinationMap.clear()
        val root = JSONObject(jsonString)
        val storedCoordinateSystemVersion = root.optInt("coordinateSystemVersion", 1)
        loadedBundledSeedVersion = root.optInt("bundledSeedVersion", 0)

        if (root.has("config")) {
            val cfgObj = root.getJSONObject("config")
            val config = StateConfig(
                state0Name = cfgObj.optString("state0Name", "Stav 0"),
                state1Name = cfgObj.optString("state1Name", "Stav 1"),
                state2Name = cfgObj.optString("state2Name", "Stav 2"),
                state3Name = cfgObj.optString("state3Name", "Stav 3"),
                defaultAuthor = cfgObj.optString("defaultAuthor", ""),
                state0ColorHex = cfgObj.optLong("state0ColorHex", 0xFF3B82F6),
                state1ColorHex = cfgObj.optLong("state1ColorHex", 0xFF10B981),
                state2ColorHex = cfgObj.optLong("state2ColorHex", 0xFFF59E0B),
                state3ColorHex = cfgObj.optLong("state3ColorHex", 0xFFEF4444)
            )
            _stateConfig.value = config
        }

        if (root.has("entries")) {
            val array = root.getJSONArray("entries")
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val storedId = item.getInt("id")
                val id = if (storedCoordinateSystemVersion < coordinateSystemVersion) {
                    GridUtils.migrateLegacyId(storedId)
                } else {
                    storedId
                }.coerceIn(0, GridUtils.TOTAL_COMBINATIONS - 1)
                val statusOrdinal = item.getInt("status")
                val note = item.optString("note", "")
                val author = item.optString("author", "")
                val updatedAt = item.optLong("updatedAt", 0L).takeIf { it > 0L }
                    ?: if (CombinationStatus.fromInt(statusOrdinal) != CombinationStatus.UNTESTED) {
                        System.currentTimeMillis()
                    } else {
                        0L
                    }
                val updatedBy = item.optString("updatedBy", "")

                combinationMap[id] = GridCombination(
                    id = id,
                    status = CombinationStatus.fromInt(statusOrdinal),
                    note = note,
                    author = author,
                    updatedAt = updatedAt,
                    updatedBy = updatedBy
                )
            }
        }

        recalculateStats()
        _dataVersion.value = System.currentTimeMillis()
    }

    private fun applyBundledSeedIfNeeded(): Boolean {
        if (loadedBundledSeedVersion >= bundledSeedVersion) return false

        return try {
            val seedJson = context.assets.open(bundledSeedFileName).bufferedReader().use { it.readText() }
            val root = JSONObject(seedJson)
            val entries = root.getJSONArray("entries")

            for (i in 0 until entries.length()) {
                val item = entries.getJSONObject(i)
                val id = item.getInt("id").coerceIn(0, GridUtils.TOTAL_COMBINATIONS - 1)
                val existing = combinationMap[id]
                val canSeed = existing == null || (
                    existing.status == CombinationStatus.UNTESTED &&
                        existing.note.isBlank() &&
                        existing.author.isBlank()
                    )

                if (canSeed) {
                    combinationMap[id] = GridCombination(
                        id = id,
                        status = CombinationStatus.fromInt(item.getInt("status")),
                        note = item.optString("note", ""),
                        author = item.optString("author", ""),
                        updatedAt = item.optLong("updatedAt", 0L).takeIf { it > 0L }
                            ?: System.currentTimeMillis(),
                        updatedBy = item.optString("updatedBy", "")
                    )
                }
            }

            loadedBundledSeedVersion = bundledSeedVersion
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun startFirebaseSyncIfNeeded() {
        if (!firebaseSyncStarted) {
            firebaseSyncStarted = true
        }
        firebaseSync.start(
            onRemoteCombination = ::handleRemoteCombination,
            onReady = ::publishLocalCombinations
        )
    }

    private fun publishLocalCombinations() {
        firebaseSyncScope.launch {
            combinationMap.values
                .filter { it.status != CombinationStatus.UNTESTED }
                .forEach(firebaseSync::publish)
        }
    }

    private fun handleRemoteCombination(remote: GridCombination) {
        firebaseSyncScope.launch {
            val local = combinationMap[remote.id]
            when {
                local == null || isNewer(remote, local) -> {
                    combinationMap[remote.id] = remote
                    recalculateStats()
                    _dataVersion.value = System.currentTimeMillis()
                    saveData()
                }
                isNewer(local, remote) -> firebaseSync.publish(local)
            }
        }
    }

    private fun isNewer(candidate: GridCombination, current: GridCombination): Boolean {
        return when {
            candidate.updatedAt != current.updatedAt -> candidate.updatedAt > current.updatedAt
            else -> {
                val candidateWriterRank = if (candidate.updatedBy.isBlank() || candidate.updatedBy == "local") 0 else 1
                val currentWriterRank = if (current.updatedBy.isBlank() || current.updatedBy == "local") 0 else 1
                candidateWriterRank > currentWriterRank || (
                    candidateWriterRank == currentWriterRank && candidate.updatedBy > current.updatedBy
                    )
            }
        }
    }

    private fun recalculateStats() {
        var nonfunctional = 0
        var functional = 0

        for (entry in combinationMap.values) {
            when (entry.status) {
                CombinationStatus.NONFUNCTIONAL -> nonfunctional++
                CombinationStatus.FUNCTIONAL -> functional++
                CombinationStatus.UNTESTED -> {}
            }
        }

        val total = GridUtils.TOTAL_COMBINATIONS
        val totalTestedCount = nonfunctional + functional
        val untestedCount = (total - totalTestedCount).coerceAtLeast(0)

        _stats.value = CombinationStats(
            total = total,
            untested = untestedCount,
            nonfunctional = nonfunctional,
            functional = functional
        )
    }
}
