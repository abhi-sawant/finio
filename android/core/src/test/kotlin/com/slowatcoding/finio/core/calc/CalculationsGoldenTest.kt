package com.slowatcoding.finio.core.calc

import com.slowatcoding.finio.core.golden.Golden
import com.slowatcoding.finio.core.golden.b
import com.slowatcoding.finio.core.golden.d
import com.slowatcoding.finio.core.golden.i
import com.slowatcoding.finio.core.golden.s
import com.slowatcoding.finio.core.model.Account
import com.slowatcoding.finio.core.model.Budget
import com.slowatcoding.finio.core.model.Category
import com.slowatcoding.finio.core.model.DebtEntry
import com.slowatcoding.finio.core.model.Goal
import com.slowatcoding.finio.core.model.GoalContribution
import com.slowatcoding.finio.core.model.Person
import com.slowatcoding.finio.core.model.Transaction
import com.slowatcoding.finio.core.model.TransactionType
import com.slowatcoding.finio.core.period.json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Test

class CalculationsGoldenTest {
    private val ledger = FixtureLedger("calculations")

    private fun txs(c: Golden.Case, i: Int): List<Transaction> = ledger.decodeArg(c, i)

    @Test
    fun matchesTypeScript() = Golden.verify("calculations") { c ->
        when (c.fn) {
            "ledger" -> c.out
            "constants" -> buildJsonObject {
                put("MAX_ROLLOVER_LOOKBACK", JsonPrimitive(MAX_ROLLOVER_LOOKBACK))
                put("BUDGET_NEAR_LIMIT_PERCENT", JsonPrimitive(BUDGET_NEAR_LIMIT_PERCENT))
                put("TRANSFER_CATEGORY_ID", JsonPrimitive(TRANSFER_CATEGORY_ID))
            }
            "miscLast" -> JsonArray(miscLast(ledger.decodeArg<List<Category>>(c, 0)).map { JsonPrimitive(it.id) })
            "isCategoryValidForType" ->
                JsonPrimitive(isCategoryValidForType(Golden.decode(c.arg(0)), Golden.decode<TransactionType>(c.arg(1))))
            "findTransferCategory" -> nullable(findTransferCategory(ledger.decodeArg(c, 0))?.let { Golden.encode(it) })
            "transactionCategoryAmounts" -> Golden.encode(transactionCategoryAmounts(Golden.decode<Transaction>(c.arg(0))))
            "getTotalIncome" -> JsonPrimitive(getTotalIncome(txs(c, 0)))
            "getTotalExpenses" -> JsonPrimitive(getTotalExpenses(txs(c, 0)))
            "activeAccounts" -> JsonArray(activeAccounts(ledger.decodeArg(c, 0)).map { JsonPrimitive(it.id) })
            "isLiquidAccount" -> JsonPrimitive(isLiquidAccount(Golden.decode<Account>(c.arg(0))))
            "getCreditUtilization" -> JsonPrimitive(getCreditUtilization(Golden.decode(c.arg(0))))
            "getTotalAccountBalance" -> JsonPrimitive(getTotalAccountBalance(ledger.decodeArg(c, 0)))
            "getNetWorth" -> JsonPrimitive(getNetWorth(ledger.decodeArg(c, 0)))
            "getTotalCreditOutstanding" -> JsonPrimitive(getTotalCreditOutstanding(ledger.decodeArg(c, 0)))
            "getTotalDepositValue" -> JsonPrimitive(getTotalDepositValue(ledger.decodeArg(c, 0), c.arg(1).instantArg))
            "getCreditCardDueInfo" -> nullable(
                getCreditCardDueInfo(Golden.decode(c.arg(0)), c.arg(1).instantArg)?.let {
                    buildJsonObject {
                        put("outstanding", JsonPrimitive(it.outstanding))
                        put("minimumDue", JsonPrimitive(it.minimumDue))
                        put("dueDate", it.dueDate.json())
                        put("daysUntilDue", JsonPrimitive(it.daysUntilDue))
                        put("isOverdue", JsonPrimitive(it.isOverdue))
                    }
                },
            )
            "transactionsInPeriod" -> ids(transactionsInPeriod(txs(c, 0), c.arg(1).rangeArg()))
            "getMonthTransactions" -> ids(getMonthTransactions(txs(c, 0), c.arg(1).instantArg, c.arg(2).i))
            "getCurrentMonthTransactions" -> ids(getCurrentMonthTransactions(txs(c, 0), c.arg(1).i, c.arg(2).instantArg))
            "getPreviousMonthTransactions" -> ids(getPreviousMonthTransactions(txs(c, 0), c.arg(1).i, c.arg(2).instantArg))
            "groupTransactionsByDate" -> JsonArray(
                groupTransactionsByDate(txs(c, 0)).map { g ->
                    buildJsonObject { put("date", JsonPrimitive(g.date)); put("transactions", ids(g.transactions)) }
                },
            )
            "sortTransactionsDateDesc" -> ids(sortTransactionsDateDesc(txs(c, 0)))
            "buildSearchIndex" -> buildSearchIndex(ledger.decodeArg(c, 0), ledger.decodeArg(c, 1), ledger.decodeArg(c, 2)).let { idx ->
                fun m(map: Map<String, String>) = buildJsonObject { map.forEach { (k, v) -> put(k, JsonPrimitive(v)) } }
                buildJsonObject {
                    put("categoryNames", m(idx.categoryNames))
                    put("accountNames", m(idx.accountNames))
                    put("labelNames", m(idx.labelNames))
                }
            }
            "transactionMatchesQuery" -> {
                val index = buildSearchIndex(ledger.decodeArg(c, 2), ledger.decodeArg(c, 3), ledger.decodeArg(c, 4))
                val q = c.arg(1).s
                JsonArray(txs(c, 0).map { JsonPrimitive(transactionMatchesQuery(it, q, index)) })
            }
            "budgetScopeKey" -> JsonPrimitive(budgetScopeKey(Golden.decode<Budget>(c.arg(0))))
            "budgetMatchedAmount" -> {
                val budget = Golden.decode<Budget>(c.arg(0))
                JsonArray(txs(c, 1).map { JsonPrimitive(budgetMatchedAmount(budget, it)) })
            }
            "computeBudgetStatuses" -> JsonArray(
                computeBudgetStatuses(ledger.decodeArg(c, 0), txs(c, 1), c.arg(2).optionsArg()).map { it.json() },
            )
            "budgetHealth" -> c.arg(0).jsonObject.let {
                JsonPrimitive(budgetHealth(it.getValue("isOver").b, it.getValue("percent").d).wire)
            }
            "computeBudgetHistory" -> JsonArray(
                computeBudgetHistory(Golden.decode(c.arg(0)), txs(c, 1), c.arg(2).optionsArg(), c.arg(3).i).map { it.json() },
            )
            "getDashboardStats" -> getDashboardStats(txs(c, 0), txs(c, 1), ledger.decodeArg(c, 2), c.arg(3).optionsArg()).let {
                buildJsonObject {
                    put("dailyAverage", JsonPrimitive(it.dailyAverage))
                    put("projectedMonth", JsonPrimitive(it.projectedMonth))
                    put("biggestExpense", nullable(it.biggestExpense?.let { t -> Golden.encode(t) }))
                    put(
                        "topCategory",
                        nullable(
                            it.topCategory?.let { tc ->
                                buildJsonObject { put("category", Golden.encode(tc.category)); put("amount", JsonPrimitive(tc.amount)) }
                            },
                        ),
                    )
                    put("monthOverMonthChange", JsonPrimitive(it.monthOverMonthChange))
                    put("savingsRate", JsonPrimitive(it.savingsRate))
                    put("savingsRateChange", it.savingsRateChange?.let(::JsonPrimitive) ?: JsonNull)
                }
            }
            "computeGoalStatus" -> computeGoalStatus(
                Golden.decode<Goal>(c.arg(0)), Golden.decode<List<GoalContribution>>(c.arg(1)), c.arg(2).instantArg,
            ).let {
                buildJsonObject {
                    put("goal", JsonPrimitive(it.goal.id))
                    put("current", JsonPrimitive(it.current))
                    put("remaining", JsonPrimitive(it.remaining))
                    put("percent", JsonPrimitive(it.percent))
                    put("isComplete", JsonPrimitive(it.isComplete))
                    put("projectedDate", it.projectedDate.json())
                }
            }
            "computePersonBalance" -> computePersonBalance(Golden.decode<Person>(c.arg(0)), Golden.decode<List<DebtEntry>>(c.arg(1))).let {
                buildJsonObject {
                    put("person", JsonPrimitive(it.person.id))
                    put("balance", JsonPrimitive(it.balance))
                    put("lastActivity", it.lastActivity?.let(::JsonPrimitive) ?: JsonNull)
                }
            }
            "getTotalOwedToYou" -> JsonPrimitive(getTotalOwedToYou(Golden.decode(c.arg(0)), Golden.decode(c.arg(1))))
            "getTotalYouOwe" -> JsonPrimitive(getTotalYouOwe(Golden.decode(c.arg(0)), Golden.decode(c.arg(1))))
            "transactionsToCsv" -> JsonPrimitive(transactionsToCsv(txs(c, 0), ledger.decodeArg(c, 1), ledger.decodeArg(c, 2)))
            else -> null
        }
    }
}
