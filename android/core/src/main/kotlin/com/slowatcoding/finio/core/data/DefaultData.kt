package com.slowatcoding.finio.core.data

import com.slowatcoding.finio.core.model.Category
import com.slowatcoding.finio.core.model.CategoryType
import com.slowatcoding.finio.core.model.Label
import com.slowatcoding.finio.core.model.Settings
import com.slowatcoding.finio.core.model.TrackingMode
import com.slowatcoding.finio.core.model.Theme
import com.slowatcoding.finio.core.period.DEFAULT_MONTH_START_DAY

// Port of web/src/data/defaultData.ts. Ids, order and colours are part of the backup contract:
// a fresh install on either client must seed byte-identical defaults.

/** Catch-all category that orphaned rows fall back to when their category is deleted. */
const val MISC_CATEGORY_ID = "cat-24"

val defaultCategories: List<Category> = listOf(
    Category("cat-1", "Food", "utensils", "#ef4444", CategoryType.Expense),
    Category("cat-2", "Transport", "car", "#f97316", CategoryType.Expense),
    Category("cat-3", "Shopping", "shopping-bag", "#8b5cf6", CategoryType.Expense),
    Category("cat-4", "Entertainment", "film", "#ec4899", CategoryType.Expense),
    Category("cat-5", "Utilities", "zap", "#06b6d4", CategoryType.Expense),
    Category("cat-6", "Healthcare", "heart-pulse", "#10b981", CategoryType.Expense),
    Category("cat-8", "Housing", "home", "#64748b", CategoryType.Expense),
    Category("cat-15", "Travel", "plane", "#ef4444", CategoryType.Expense),
    Category("cat-16", "Gifts", "gift", "#f97316", CategoryType.Expense),
    Category("cat-17", "Personal Care", "scissors", "#8b5cf6", CategoryType.Expense),
    Category("cat-18", "Subscriptions", "repeat", "#ec4899", CategoryType.Expense),
    Category("cat-19", "Vehicles", "truck", "#06b6d4", CategoryType.Expense),
    Category("cat-20", "Financial", "dollar-sign", "#10b981", CategoryType.Expense),
    Category("cat-9", "Salary", "briefcase", "#22c55e", CategoryType.Income),
    Category("cat-10", "Side income", "laptop", "#146b54", CategoryType.Income),
    Category("cat-23", "Interest", "dollar-sign", "#10b981", CategoryType.Income),
    Category("cat-13", "Transfer", "repeat", "#3b82f6", CategoryType.Both),
    Category("cat-24", "Miscellaneous", "circle-ellipsis", "#94a3b8", CategoryType.Both),
    Category("cat-26", "Insurance", "umbrella", "#0ea5e9", CategoryType.Expense),
    Category("cat-27", "Loan / EMI", "banknote", "#f43f5e", CategoryType.Expense),
    Category("cat-29", "Fitness & Wellness", "dumbbell", "#14b8a6", CategoryType.Expense),
    Category("cat-33", "Bonus", "sparkles", "#fbbf24", CategoryType.Income),
)

/**
 * Ids added after the original v1 category set — appended to existing installs by migration
 * rather than shipped only to fresh ones.
 */
val NEW_DEFAULT_CATEGORY_IDS: List<String> = listOf(
    "cat-26", "cat-27", "cat-29", "cat-33",
)

val defaultLabels: List<Label> = listOf(
    Label("lbl-1", "Essential", "#22c55e"),
    Label("lbl-2", "Discretionary", "#f59e0b"),
    Label("lbl-3", "Recurring", "#3b82f6"),
    Label("lbl-4", "Tax", "#ef4444"),
    Label("lbl-5", "Obligation", "#10b981"),
    Label("lbl-6", "Investment", "#8b5cf6"),
    Label("lbl-7", "Lending", "#ec4899"),
    Label("lbl-8", "For Self", "#64748b"),
    Label("lbl-9", "For Others", "#06b6d4"),
    Label("lbl-10", "Reimbursable", "#14b8a6"),
    Label("lbl-11", "Business", "#6366f1"),
    Label("lbl-12", "One-time", "#f43f5e"),
    Label("lbl-13", "Planned", "#0ea5e9"),
    Label("lbl-14", "Family", "#eab308"),
)

/**
 * The web's `defaultSettings`. Spelled out field by field rather than `Settings()` because the
 * Kotlin data-class defaults are not the same: `notifyDailyLog` is **true** here.
 */
val defaultSettings: Settings = Settings(
    theme = Theme.System,
    trackingMode = TrackingMode.Full,
    amoledDark = false,
    // Deliberately blank: the first-run wizard asks for a name.
    userName = "",
    autoLocalBackup = false,
    monthStartDay = DEFAULT_MONTH_START_DAY,
    hideAmounts = false,
    // Master off, sub-toggles on: nobody is opted in without a tap.
    notificationsEnabled = false,
    notifyBills = true,
    notifyBudgets = true,
    notifyCreditDue = true,
    notifyLeadDays = 2,
    notifyDailyLog = true,
)
