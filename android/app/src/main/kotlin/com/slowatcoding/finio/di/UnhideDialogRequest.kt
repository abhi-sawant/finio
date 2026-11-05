package com.slowatcoding.finio.di

enum class UnhideDialogKind { Setup, Verify }

/**
 * Why the dialog is open. `Toggle` is the eye button (the dialog finishes by flipping the
 * amounts); `Change` / `Remove` come from Settings and leave the amounts as they are.
 */
enum class UnhideIntent { Toggle, Change, Remove }

data class UnhideDialogRequest(val kind: UnhideDialogKind, val intent: UnhideIntent)
