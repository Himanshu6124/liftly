package com.himanshu.liftly.core.data.db

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection

/** Runs [block] in one write transaction; DAO calls inside it share the connection. */
suspend fun <T> AppDatabase.writeTransaction(block: suspend () -> T): T =
    useWriterConnection { connection -> connection.immediateTransaction { block() } }
