package com.slowatcoding.finio.core.money

import com.slowatcoding.finio.core.js.jsRound

/** Round to paise — web/src/store/balance.ts `roundMoney`, bit-for-bit (JS Math.round semantics). */
fun roundMoney(value: Double): Double = jsRound(value * 100) / 100
