package com.slowatcoding.finio.platform

import com.slowatcoding.finio.platform.storage.DebouncedJsonWriter
import com.slowatcoding.finio.platform.storage.JsonFileStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class JsonFileStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    @Serializable data class Box(val n: Int)

    @Test fun missingFileReadsNull() {
        assertNull(JsonFileStore(tmp.root, "x.json").readText())
        assertFalse(JsonFileStore(tmp.root, "x.json").exists())
    }

    @Test fun writeIsReadBackAndPreviousGoodBecomesBak() {
        val store = JsonFileStore(tmp.root, "x.json")
        store.write(Box.serializer(), Box(1))
        store.write(Box.serializer(), Box(2))
        assertEquals(Box(2), store.read(Box.serializer()))
        assertEquals("""{"n":1}""", store.backupFile.readText())
        assertFalse(java.io.File(tmp.root, "x.json.tmp").exists())
    }

    @Test fun corruptMainFallsBackToBak() {
        val store = JsonFileStore(tmp.root, "x.json")
        store.write(Box.serializer(), Box(1))
        store.write(Box.serializer(), Box(2))
        store.file.writeText("{\"n\":2, trunca")
        val fresh = JsonFileStore(tmp.root, "x.json")
        assertEquals(Box(1), fresh.read(Box.serializer()))
        // The corrupt main must not be rotated over the good backup on the next write.
        fresh.write(Box.serializer(), Box(3))
        assertEquals(Box(3), fresh.read(Box.serializer()))
        assertEquals("""{"n":1}""", fresh.backupFile.readText())
    }

    @Test fun mainThatParsesButDoesNotDecodeFallsBack() {
        val store = JsonFileStore(tmp.root, "x.json")
        store.write(Box.serializer(), Box(1))
        store.write(Box.serializer(), Box(2))
        store.file.writeText("""{"n":"nope"}""")
        assertEquals(Box(1), JsonFileStore(tmp.root, "x.json").read(Box.serializer()))
    }

    @Test fun missingMainWithBakReads() {
        val store = JsonFileStore(tmp.root, "x.json")
        store.write(Box.serializer(), Box(1))
        store.write(Box.serializer(), Box(2))
        store.file.delete() // crash between "main → bak" and "tmp → main"
        assertTrue(store.exists())
        assertEquals(Box(1), store.read(Box.serializer()))
    }

    @Test fun deleteRemovesBoth() {
        val store = JsonFileStore(tmp.root, "x.json")
        store.writeText("1"); store.writeText("2")
        store.delete()
        assertFalse(store.exists())
    }

    @Test fun debouncedWriterCoalescesToTheLatestAndFlushes() = runBlocking {
        val store = JsonFileStore(tmp.root, "d.json")
        val scope = CoroutineScope(SupervisorJob())
        val writer = DebouncedJsonWriter(store, scope, delayMs = 10_000)
        var produced = 0
        for (i in 1..5) writer.schedule { produced++; """{"n":$i}""" }
        assertTrue(writer.hasPending())
        assertNull(store.readText())
        writer.flush()
        assertEquals("""{"n":5}""", store.readText())
        assertEquals(1, produced)
        assertFalse(writer.hasPending())
        writer.flushBlocking() // nothing pending: no-op
        scope.cancel()
    }

    @Test fun debouncedWriterWritesAfterTheDelay() = runBlocking {
        val store = JsonFileStore(tmp.root, "d.json")
        val scope = CoroutineScope(SupervisorJob())
        val writer = DebouncedJsonWriter(store, scope, delayMs = 20)
        writer.schedule { """{"n":1}""" }
        var waited = 0
        while (store.readText() == null && waited < 2_000) { Thread.sleep(10); waited += 10 }
        assertEquals("""{"n":1}""", store.readText())
        scope.cancel()
    }
}
