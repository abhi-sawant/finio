package com.slowatcoding.finio.ui.common

import com.slowatcoding.finio.ui.components.ToastAction
import com.slowatcoding.finio.ui.components.toast

/**
 * The web's `toast.success(msg, { action: { label: 'Undo', onClick } })` — every destructive or
 * automatic change offers one (delete → restore, bulk delete → restore all, template add →
 * delete). Example:
 *
 *   val removed = store.deleteTransaction(id) ?: return
 *   undoToast("Transaction deleted") { store.restoreTransaction(removed) }
 */
fun undoToast(message: String, description: String? = null, onUndo: () -> Unit): Long =
    toast.success(message, description, ToastAction("Undo", onUndo))
