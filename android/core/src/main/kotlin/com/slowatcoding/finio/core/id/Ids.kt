package com.slowatcoding.finio.core.id

import java.util.UUID

/** `crypto.randomUUID()` — every entity id minted by either client is a v4 UUID string. */
fun newId(): String = UUID.randomUUID().toString()
