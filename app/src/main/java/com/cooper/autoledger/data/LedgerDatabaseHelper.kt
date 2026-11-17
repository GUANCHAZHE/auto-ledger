package com.cooper.autoledger.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class LedgerDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.enableWriteAheadLogging()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_BILLS_TABLE)
        db.execSQL(SQL_CREATE_TIMESTAMP_INDEX)
        db.execSQL(SQL_CREATE_DIGEST_INDEX)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 1) {
            onCreate(db)
        }
    }

    companion object {
        const val DATABASE_NAME = "auto_ledger.db"
        const val DATABASE_VERSION = 1

        const val TABLE_BILLS = "bills"
        const val COL_ID = "id"
        const val COL_AMOUNT_CENTS = "amount_cents"
        const val COL_TYPE = "type"
        const val COL_CATEGORY = "category"
        const val COL_MERCHANT = "merchant"
        const val COL_NOTE = "note"
        const val COL_CHANNEL = "channel"
        const val COL_SOURCE_PKG = "source_pkg"
        const val COL_TIMESTAMP_MS = "timestamp_ms"
        const val COL_AUTO_CAPTURED = "auto_captured"
        const val COL_RAW_DIGEST = "raw_digest"

        private const val SQL_CREATE_BILLS_TABLE = """
            CREATE TABLE IF NOT EXISTS $TABLE_BILLS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_AMOUNT_CENTS INTEGER NOT NULL,
                $COL_TYPE TEXT NOT NULL,
                $COL_CATEGORY TEXT NOT NULL,
                $COL_MERCHANT TEXT NOT NULL,
                $COL_NOTE TEXT NOT NULL DEFAULT '',
                $COL_CHANNEL TEXT NOT NULL,
                $COL_SOURCE_PKG TEXT NOT NULL DEFAULT '',
                $COL_TIMESTAMP_MS INTEGER NOT NULL,
                $COL_AUTO_CAPTURED INTEGER NOT NULL DEFAULT 1,
                $COL_RAW_DIGEST TEXT NOT NULL DEFAULT ''
            )
        """

        private const val SQL_CREATE_TIMESTAMP_INDEX = """
            CREATE INDEX IF NOT EXISTS idx_bills_timestamp_ms
            ON $TABLE_BILLS ($COL_TIMESTAMP_MS DESC)
        """

        private const val SQL_CREATE_DIGEST_INDEX = """
            CREATE INDEX IF NOT EXISTS idx_bills_raw_digest
            ON $TABLE_BILLS ($COL_RAW_DIGEST)
        """
    }
}
