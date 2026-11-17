package com.cooper.autoledger.data

import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.cooper.autoledger.model.BillRecord
import com.cooper.autoledger.model.Category
import com.cooper.autoledger.model.CurrencyUtils
import com.cooper.autoledger.model.ParsedScreenBill
import com.cooper.autoledger.model.PaymentChannel
import com.cooper.autoledger.model.TransactionType
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LedgerRepository private constructor(context: Context) {

    private val appContext: Context = context.applicationContext
    private val dbHelper: LedgerDatabaseHelper = LedgerDatabaseHelper(appContext)
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val dbLock = Any()

    private val _records = MutableStateFlow<List<BillRecord>>(emptyList())
    val records: StateFlow<List<BillRecord>> = _records.asStateFlow()

    private val _autoCaptureEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_AUTO_CAPTURE_ENABLED, true)
    )
    val autoCaptureEnabled: StateFlow<Boolean> = _autoCaptureEnabled.asStateFlow()

    init {
        refresh()
    }

    fun setAutoCaptureEnabled(enabled: Boolean) {
        synchronized(dbLock) {
            prefs.edit().putBoolean(KEY_AUTO_CAPTURE_ENABLED, enabled).apply()
            _autoCaptureEnabled.value = enabled
        }
    }

    fun insertRecord(record: BillRecord): Long {
        synchronized(dbLock) {
            val db = dbHelper.writableDatabase
            val normalizedRecord = record.copy(
                amountCents = abs(record.amountCents),
                rawDigest = record.rawDigest.ifBlank {
                    computeDigest(abs(record.amountCents), record.type, record.channel)
                }
            )
            val rowId = db.insertOrThrow(
                LedgerDatabaseHelper.TABLE_BILLS,
                null,
                normalizedRecord.toContentValues(includeId = false)
            )
            _records.value = queryAllLocked(db)
            return rowId
        }
    }

    fun insertFromParsedIfNotDuplicate(
        parsed: ParsedScreenBill,
        dedupWindowMs: Long = 90_000L
    ): BillRecord? {
        val normalizedCents = abs(parsed.amountCents)
        if (normalizedCents <= 0L) return null

        synchronized(dbLock) {
            val db = dbHelper.writableDatabase
            val nowMs = System.currentTimeMillis()
            val windowStartMs = nowMs - dedupWindowMs.coerceAtLeast(0L)
            val digest = computeDigest(normalizedCents, parsed.type, parsed.channel)

            val isDuplicate = db.query(
                LedgerDatabaseHelper.TABLE_BILLS,
                arrayOf(LedgerDatabaseHelper.COL_ID),
                "${LedgerDatabaseHelper.COL_TIMESTAMP_MS} >= ? AND " +
                    "(${LedgerDatabaseHelper.COL_RAW_DIGEST} = ? OR " +
                    "(${LedgerDatabaseHelper.COL_AMOUNT_CENTS} = ? AND " +
                    "${LedgerDatabaseHelper.COL_TYPE} = ? AND " +
                    "${LedgerDatabaseHelper.COL_CHANNEL} = ?))",
                arrayOf(
                    windowStartMs.toString(),
                    digest,
                    normalizedCents.toString(),
                    parsed.type.name,
                    parsed.channel.name
                ),
                null,
                null,
                null,
                "1"
            ).use { cursor ->
                cursor.moveToFirst()
            }

            if (isDuplicate) {
                return null
            }

            val toSave = BillRecord(
                id = 0L,
                amountCents = normalizedCents,
                type = parsed.type,
                category = parsed.suggestedCategory,
                merchant = parsed.merchant.trim().ifEmpty { parsed.channel.displayName },
                note = "",
                channel = parsed.channel,
                sourcePkg = parsed.sourcePkg.trim(),
                timestampMs = nowMs,
                autoCaptured = true,
                rawDigest = digest
            )

            val rowId = db.insertOrThrow(
                LedgerDatabaseHelper.TABLE_BILLS,
                null,
                toSave.toContentValues(includeId = false)
            )
            val saved = toSave.copy(id = rowId)
            _records.value = queryAllLocked(db)
            return saved
        }
    }

    fun updateRecord(record: BillRecord) {
        if (record.id <= 0L) return
        synchronized(dbLock) {
            val db = dbHelper.writableDatabase
            val normalizedCents = abs(record.amountCents)
            val updated = record.copy(
                amountCents = normalizedCents,
                rawDigest = computeDigest(normalizedCents, record.type, record.channel)
            )
            db.update(
                LedgerDatabaseHelper.TABLE_BILLS,
                updated.toContentValues(includeId = false),
                "${LedgerDatabaseHelper.COL_ID} = ?",
                arrayOf(record.id.toString())
            )
            _records.value = queryAllLocked(db)
        }
    }

    fun deleteRecord(id: Long) {
        if (id <= 0L) return
        synchronized(dbLock) {
            val db = dbHelper.writableDatabase
            db.delete(
                LedgerDatabaseHelper.TABLE_BILLS,
                "${LedgerDatabaseHelper.COL_ID} = ?",
                arrayOf(id.toString())
            )
            _records.value = queryAllLocked(db)
        }
    }

    fun exportCsv(): String {
        val snapshot = synchronized(dbLock) {
            queryAllLocked(dbHelper.readableDatabase)
        }
        val zone = ZoneId.systemDefault()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        return buildString {
            append(CSV_HEADER).append('\n')
            for (record in snapshot) {
                val timeStr = Instant.ofEpochMilli(record.timestampMs)
                    .atZone(zone)
                    .format(formatter)
                val typeLabel = if (record.type == TransactionType.EXPENSE) "支出" else "收入"
                val sourceLabel = if (record.autoCaptured) {
                    if (record.sourcePkg.isNotBlank()) {
                        "屏幕自动(${record.sourcePkg})"
                    } else {
                        "屏幕自动"
                    }
                } else {
                    "手动记账"
                }

                append(record.id).append(',')
                append(escapeCsv(timeStr)).append(',')
                append(escapeCsv(typeLabel)).append(',')
                append(escapeCsv(record.category.displayName)).append(',')
                append(escapeCsv(record.merchant)).append(',')
                append(CurrencyUtils.formatYuan(record.amountCents)).append(',')
                append(escapeCsv(record.channel.displayName)).append(',')
                append(escapeCsv(sourceLabel)).append(',')
                append(escapeCsv(record.note)).append('\n')
            }
        }
    }

    fun refresh() {
        synchronized(dbLock) {
            _records.value = queryAllLocked(dbHelper.readableDatabase)
        }
    }

    private fun queryAllLocked(db: SQLiteDatabase): List<BillRecord> {
        val result = ArrayList<BillRecord>()
        db.query(
            LedgerDatabaseHelper.TABLE_BILLS,
            null,
            null,
            null,
            null,
            null,
            "${LedgerDatabaseHelper.COL_TIMESTAMP_MS} DESC, ${LedgerDatabaseHelper.COL_ID} DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(cursor.toBillRecord())
            }
        }
        return result
    }

    private fun Cursor.toBillRecord(): BillRecord {
        val id = getLong(getColumnIndexOrThrow(LedgerDatabaseHelper.COL_ID))
        val amountCents = getLong(getColumnIndexOrThrow(LedgerDatabaseHelper.COL_AMOUNT_CENTS))
        val typeRaw = getString(getColumnIndexOrThrow(LedgerDatabaseHelper.COL_TYPE)).orEmpty()
        val categoryRaw = getString(getColumnIndexOrThrow(LedgerDatabaseHelper.COL_CATEGORY)).orEmpty()
        val merchant = getString(getColumnIndexOrThrow(LedgerDatabaseHelper.COL_MERCHANT)).orEmpty()
        val note = getString(getColumnIndexOrThrow(LedgerDatabaseHelper.COL_NOTE)).orEmpty()
        val channelRaw = getString(getColumnIndexOrThrow(LedgerDatabaseHelper.COL_CHANNEL)).orEmpty()
        val sourcePkg = getString(getColumnIndexOrThrow(LedgerDatabaseHelper.COL_SOURCE_PKG)).orEmpty()
        val timestampMs = getLong(getColumnIndexOrThrow(LedgerDatabaseHelper.COL_TIMESTAMP_MS))
        val autoCaptured = getInt(getColumnIndexOrThrow(LedgerDatabaseHelper.COL_AUTO_CAPTURED)) != 0
        val rawDigest = getString(getColumnIndexOrThrow(LedgerDatabaseHelper.COL_RAW_DIGEST)).orEmpty()

        val type = runCatching { TransactionType.valueOf(typeRaw) }
            .getOrDefault(TransactionType.EXPENSE)
        val defaultCategory = if (type == TransactionType.INCOME) {
            Category.OTHER_INCOME
        } else {
            Category.OTHER_EXPENSE
        }
        val category = runCatching { Category.valueOf(categoryRaw) }
            .getOrDefault(defaultCategory)
        val channel = runCatching { PaymentChannel.valueOf(channelRaw) }
            .getOrDefault(PaymentChannel.OTHER)

        return BillRecord(
            id = id,
            amountCents = amountCents,
            type = type,
            category = category,
            merchant = merchant,
            note = note,
            channel = channel,
            sourcePkg = sourcePkg,
            timestampMs = timestampMs,
            autoCaptured = autoCaptured,
            rawDigest = rawDigest
        )
    }

    private fun BillRecord.toContentValues(includeId: Boolean): ContentValues {
        return ContentValues().apply {
            if (includeId && id > 0L) {
                put(LedgerDatabaseHelper.COL_ID, id)
            }
            put(LedgerDatabaseHelper.COL_AMOUNT_CENTS, amountCents)
            put(LedgerDatabaseHelper.COL_TYPE, type.name)
            put(LedgerDatabaseHelper.COL_CATEGORY, category.name)
            put(LedgerDatabaseHelper.COL_MERCHANT, merchant)
            put(LedgerDatabaseHelper.COL_NOTE, note)
            put(LedgerDatabaseHelper.COL_CHANNEL, channel.name)
            put(LedgerDatabaseHelper.COL_SOURCE_PKG, sourcePkg)
            put(LedgerDatabaseHelper.COL_TIMESTAMP_MS, timestampMs)
            put(LedgerDatabaseHelper.COL_AUTO_CAPTURED, if (autoCaptured) 1 else 0)
            put(LedgerDatabaseHelper.COL_RAW_DIGEST, rawDigest)
        }
    }

    private fun escapeCsv(value: String): String {
        val sanitized = value.replace("\r\n", " ").replace('\r', ' ').replace('\n', ' ')
        return if (sanitized.contains(',') || sanitized.contains('"')) {
            "\"${sanitized.replace("\"", "\"\"")}\""
        } else {
            sanitized
        }
    }

    companion object {
        private const val PREFS_NAME = "auto_ledger_prefs"
        private const val KEY_AUTO_CAPTURE_ENABLED = "auto_capture_enabled"
        private const val CSV_HEADER = "ID,时间,类型,分类,商户,金额(元),渠道,来源,备注"
        private const val HEX_CHARS = "0123456789abcdef"

        @Volatile
        private var INSTANCE: LedgerRepository? = null

        fun getInstance(context: Context): LedgerRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LedgerRepository(context.applicationContext).also { INSTANCE = it }
            }
        }

        internal fun computeDigest(
            amountCents: Long,
            type: TransactionType,
            channel: PaymentChannel
        ): String {
            val payload = "$amountCents|${type.name}|${channel.name}"
            val bytes = MessageDigest.getInstance("SHA-256")
                .digest(payload.toByteArray(Charsets.UTF_8))
            return buildString(bytes.size * 2) {
                for (b in bytes) {
                    val v = b.toInt() and 0xFF
                    append(HEX_CHARS[v ushr 4])
                    append(HEX_CHARS[v and 0x0F])
                }
            }
        }
    }
}
