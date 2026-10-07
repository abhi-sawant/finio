package com.slowatcoding.finio.core.store

import com.slowatcoding.finio.core.calc.getNetWorth
import com.slowatcoding.finio.core.calc.getTotalAccountBalance
import com.slowatcoding.finio.core.data.defaultSettings
import com.slowatcoding.finio.core.js.iso
import com.slowatcoding.finio.core.js.toIso
import com.slowatcoding.finio.core.model.Account
import com.slowatcoding.finio.core.model.AccountType
import com.slowatcoding.finio.core.model.BudgetPeriod
import com.slowatcoding.finio.core.model.CategoryRule
import com.slowatcoding.finio.core.model.DepositCompounding
import com.slowatcoding.finio.core.model.DepositTerms
import com.slowatcoding.finio.core.model.ImportMode
import com.slowatcoding.finio.core.model.ImportPayload
import com.slowatcoding.finio.core.model.NetWorthSnapshot
import com.slowatcoding.finio.core.model.RecurrenceFrequency
import com.slowatcoding.finio.core.model.RecurringTransaction
import com.slowatcoding.finio.core.model.Goal
import com.slowatcoding.finio.core.model.GoalContribution
import com.slowatcoding.finio.core.model.Person
import com.slowatcoding.finio.core.model.DebtEntry
import com.slowatcoding.finio.core.model.RuleMatchType
import com.slowatcoding.finio.core.model.RuleScope
import com.slowatcoding.finio.core.model.Theme
import com.slowatcoding.finio.core.model.Transaction
import com.slowatcoding.finio.core.model.TransactionSplit
import com.slowatcoding.finio.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** Port of web/src/store/useFinanceStore.test.ts, one JUnit test per vitest `it`. */
class FinanceStoreTest {
    private var now: Instant = Instant.parse("2026-06-15T12:00:00.000Z")
    private var counter = 0
    private val store = FinanceStore(clock = { now }, idGen = { "id-${++counter}" })
    private val state get() = store.current

    private fun account(id: String, balance: Double, openingBalance: Double = balance) = Account(
        id = id, name = id, type = AccountType.Checking, color = "#000", icon = "landmark",
        balance = balance, openingBalance = openingBalance, createdAt = "2026-01-01T00:00:00.000Z",
    )

    private fun tx(
        id: String,
        type: TransactionType,
        amount: Double,
        accountId: String,
        toAccountId: String? = null,
        categoryId: String = "cat-1",
        date: String = "2026-06-01T00:00:00.000Z",
        note: String = "",
        labels: List<String> = emptyList(),
        splits: List<TransactionSplit>? = null,
    ) = Transaction(
        id = id, type = type, amount = amount, accountId = accountId, toAccountId = toAccountId,
        categoryId = categoryId, date = date, note = note, labels = labels,
        createdAt = "2026-06-01T00:00:00.000Z", splits = splits,
    )

    private fun seed(accounts: List<Account>, transactions: List<Transaction> = emptyList()) =
        store.setState { it.copy(accounts = accounts, transactions = transactions) }

    private val E = TransactionType.Expense
    private val I = TransactionType.Income
    private val T = TransactionType.Transfer

    // ---- addAccount / updateAccount ----------------------------------------------------------

    @Test fun seedsTheOpeningBalanceFromTheBalanceEntered() {
        store.addAccount(NewAccount("Cash", AccountType.Cash, "#000", "banknote", 5000.0))
        assertEquals(5000.0, state.accounts[0].openingBalance, 0.0)
    }

    @Test fun shiftsTheOpeningBalanceWhenTheCurrentBalanceIsEdited() {
        seed(listOf(account("a", 800.0, 1000.0)), listOf(tx("t1", E, 200.0, "a")))
        store.updateAccount("a") { it.copy(balance = 900.0) }
        assertEquals(1100.0, state.accounts[0].openingBalance, 0.0)
        assertEquals(0, store.recomputeBalances().changed)
        assertEquals(900.0, state.accounts[0].balance, 0.0)
    }

    // ---- recomputeBalances --------------------------------------------------------------------

    @Test fun repairsDriftedBalancesAndReportsWhatItCorrected() {
        seed(listOf(account("a", 999.0, 1000.0), account("b", 500.0, 500.0)), listOf(tx("t1", E, 100.0, "a")))
        assertEquals(BalanceDiff(1, -99.0), store.recomputeBalances())
        assertEquals(listOf(900.0, 500.0), state.accounts.map { it.balance })
    }

    @Test fun reportsNothingToDoWhenBalancesAgree() {
        seed(listOf(account("a", 900.0, 1000.0)), listOf(tx("t1", E, 100.0, "a")))
        assertEquals(BalanceDiff(0, 0.0), store.recomputeBalances())
    }

    // ---- deleteAccount ------------------------------------------------------------------------

    @Test fun deleteAccountReversesTheOtherSideOfATransfer() {
        seed(listOf(account("a", 700.0, 1000.0), account("b", 500.0, 200.0)), listOf(tx("t1", T, 300.0, "a", toAccountId = "b")))
        store.deleteAccount("a")
        assertEquals(1, state.accounts.size)
        assertEquals(200.0, state.accounts[0].balance, 0.0)
        assertTrue(state.transactions.isEmpty())
        assertEquals(0, store.recomputeBalances().changed)
    }

    // ---- setAccountArchived -------------------------------------------------------------------

    @Test fun archivingKeepsTransactionsAndBalance() {
        seed(listOf(account("a", 800.0, 1000.0)), listOf(tx("t1", E, 200.0, "a")))
        store.setAccountArchived("a", true)
        assertNotNull(state.accounts[0].archivedAt)
        assertEquals(800.0, state.accounts[0].balance, 0.0)
        assertEquals(1, state.transactions.size)
    }

    @Test fun reopeningDropsTheFlagEntirely() {
        seed(listOf(account("a", 100.0)))
        store.setAccountArchived("a", true)
        store.setAccountArchived("a", false)
        assertNull(state.accounts[0].archivedAt)
    }

    @Test fun archivedAccountsLeaveRunningTotals() {
        seed(listOf(account("a", 1000.0), account("b", 500.0)))
        store.setAccountArchived("b", true)
        assertEquals(2, state.accounts.size)
        assertEquals(1000.0, getTotalAccountBalance(state.accounts), 0.0)
        assertEquals(1000.0, getNetWorth(state.accounts), 0.0)
    }

    // ---- deleteTransaction / restoreTransaction ----------------------------------------------

    @Test fun deleteTransactionReturnsTheRowAndReversesItsDelta() {
        seed(listOf(account("a", 800.0, 1000.0)), listOf(tx("t1", E, 200.0, "a")))
        assertEquals("t1", store.deleteTransaction("t1")?.id)
        assertEquals(1000.0, state.accounts[0].balance, 0.0)
    }

    @Test fun deleteTransactionReturnsNullForAnUnknownId() {
        seed(listOf(account("a", 100.0)))
        assertNull(store.deleteTransaction("nope"))
    }

    @Test fun restoreTransactionReinsertsUnderTheOriginalId() {
        seed(listOf(account("a", 800.0, 1000.0)), listOf(tx("t1", E, 200.0, "a")))
        store.restoreTransaction(store.deleteTransaction("t1")!!)
        assertEquals(listOf("t1"), state.transactions.map { it.id })
        assertEquals(800.0, state.accounts[0].balance, 0.0)
    }

    @Test fun repeatedRestoreIsIgnored() {
        seed(listOf(account("a", 800.0, 1000.0)), listOf(tx("t1", E, 200.0, "a")))
        val removed = store.deleteTransaction("t1")!!
        store.restoreTransaction(removed)
        store.restoreTransaction(removed)
        assertEquals(1, state.transactions.size)
        assertEquals(800.0, state.accounts[0].balance, 0.0)
    }

    @Test fun deleteAndRestoreMoveBothSidesOfATransfer() {
        seed(listOf(account("a", 700.0, 1000.0), account("b", 800.0, 500.0)), listOf(tx("t1", T, 300.0, "a", toAccountId = "b")))
        val removed = store.deleteTransaction("t1")!!
        assertEquals(1000.0, state.accounts.first { it.id == "a" }.balance, 0.0)
        assertEquals(500.0, state.accounts.first { it.id == "b" }.balance, 0.0)
        store.restoreTransaction(removed)
        assertEquals(700.0, state.accounts.first { it.id == "a" }.balance, 0.0)
        assertEquals(800.0, state.accounts.first { it.id == "b" }.balance, 0.0)
    }

    // ---- bulkDeleteTransactions / restoreTransactions ----------------------------------------

    @Test fun bulkDeleteRemovesEveryListedRowAndReversesDeltas() {
        seed(listOf(account("a", 500.0, 1000.0)), listOf(tx("t1", E, 200.0, "a"), tx("t2", E, 300.0, "a"), tx("t3", E, 50.0, "a")))
        val removed = store.bulkDeleteTransactions(listOf("t1", "t2"))
        assertEquals(listOf("t1", "t2"), removed.map { it.id }.sorted())
        assertEquals(listOf("t3"), state.transactions.map { it.id })
        assertEquals(1000.0, state.accounts[0].balance, 0.0)
    }

    @Test fun bulkDeleteOfUnknownIdsChangesNothing() {
        seed(listOf(account("a", 100.0)))
        assertTrue(store.bulkDeleteTransactions(listOf("nope")).isEmpty())
        assertEquals(100.0, state.accounts[0].balance, 0.0)
    }

    @Test fun restoreTransactionsReappliesDeltasAndGuardsARepeat() {
        seed(listOf(account("a", 500.0, 1000.0)), listOf(tx("t1", E, 200.0, "a"), tx("t2", E, 300.0, "a")))
        val removed = store.bulkDeleteTransactions(listOf("t1", "t2"))
        store.restoreTransactions(removed)
        store.restoreTransactions(removed)
        assertEquals(listOf("t1", "t2"), state.transactions.map { it.id }.sorted())
        assertEquals(500.0, state.accounts[0].balance, 0.0)
    }

    // ---- bulkRecategorize / bulkAddLabel ------------------------------------------------------

    @Test fun bulkRecategorizeOnlyTouchesListedRows() {
        seed(listOf(account("a", 100.0)), listOf(tx("t1", E, 10.0, "a"), tx("t2", E, 10.0, "a"), tx("t3", E, 10.0, "a")))
        store.bulkRecategorize(listOf("t1", "t2"), "cat-2")
        val byId = state.transactions.associateBy { it.id }
        assertEquals("cat-2", byId["t1"]?.categoryId)
        assertEquals("cat-2", byId["t2"]?.categoryId)
        assertEquals("cat-1", byId["t3"]?.categoryId)
    }

    @Test fun bulkRecategorizeFlattensASplit() {
        seed(
            listOf(account("a", 100.0)),
            listOf(tx("t1", E, 100.0, "a", categoryId = "", splits = listOf(TransactionSplit("cat-1", 60.0), TransactionSplit("cat-2", 40.0)))),
        )
        store.bulkRecategorize(listOf("t1"), "cat-3")
        val t1 = state.transactions.first { it.id == "t1" }
        assertEquals("cat-3", t1.categoryId)
        assertNull(t1.splits)
    }

    @Test fun bulkRecategorizeNeverTouchesTransfersOrInvalidTypes() {
        seed(
            listOf(account("a", 100.0), account("b", 0.0)),
            listOf(
                tx("exp", E, 10.0, "a", categoryId = "cat-1"),
                tx("inc", I, 10.0, "a", categoryId = "cat-9"),
                tx("xfer", T, 10.0, "a", toAccountId = "b", categoryId = "cat-13"),
            ),
        )
        store.bulkRecategorize(listOf("exp", "inc", "xfer"), "cat-2")
        var byId = state.transactions.associateBy { it.id }
        assertEquals("cat-2", byId["exp"]?.categoryId)
        assertEquals("cat-9", byId["inc"]?.categoryId)
        assertEquals("cat-13", byId["xfer"]?.categoryId)
        store.bulkRecategorize(listOf("xfer", "inc"), "cat-24")
        byId = state.transactions.associateBy { it.id }
        assertEquals("cat-13", byId["xfer"]?.categoryId)
        assertEquals("cat-24", byId["inc"]?.categoryId)
    }

    @Test fun bulkRecategorizeIgnoresAnUnknownCategory() {
        seed(listOf(account("a", 100.0)), listOf(tx("t1", E, 10.0, "a")))
        assertEquals(0, store.bulkRecategorize(listOf("t1"), "nope"))
        assertEquals("cat-1", state.transactions[0].categoryId)
    }

    @Test fun bulkAddLabelNeverDuplicates() {
        seed(listOf(account("a", 100.0)), listOf(tx("t1", E, 10.0, "a"), tx("t2", E, 10.0, "a", labels = listOf("lbl-1"))))
        store.bulkAddLabel(listOf("t1", "t2"), "lbl-1")
        val byId = state.transactions.associateBy { it.id }
        assertEquals(listOf("lbl-1"), byId["t1"]?.labels)
        assertEquals(listOf("lbl-1"), byId["t2"]?.labels)
    }

    // ---- bulkAddTransactions ------------------------------------------------------------------

    @Test fun bulkAddInsertsRowsWithFreshIdsAndAppliesDeltas() {
        seed(listOf(account("a", 1000.0)))
        val added = store.bulkAddTransactions(
            listOf(
                NewTransaction(E, 100.0, "a", categoryId = "cat-1", date = "2026-06-01T00:00:00.000Z", note = "Coffee"),
                NewTransaction(I, 5000.0, "a", categoryId = "cat-1", date = "2026-06-02T00:00:00.000Z", note = "Salary"),
            ),
        )
        assertEquals(2, added)
        assertEquals(2, state.transactions.size)
        assertTrue(state.transactions.all { it.id.isNotEmpty() && it.createdAt.isNotEmpty() })
        assertEquals(5900.0, state.accounts[0].balance, 0.0)
    }

    @Test fun bulkAddOfNothingIsZero() {
        seed(listOf(account("a", 100.0)))
        assertEquals(0, store.bulkAddTransactions(emptyList()))
        assertTrue(state.transactions.isEmpty())
    }

    // ---- deleteCategory -----------------------------------------------------------------------

    @Test fun deleteCategoryReassignsADanglingSplitEntry() {
        seed(
            listOf(account("a", 100.0)),
            listOf(tx("t1", E, 150.0, "a", categoryId = "", splits = listOf(TransactionSplit("cat-1", 100.0), TransactionSplit("cat-2", 50.0)))),
        )
        store.deleteCategory("cat-1")
        assertEquals(listOf(TransactionSplit("cat-24", 100.0), TransactionSplit("cat-2", 50.0)), state.transactions[0].splits)
    }

    @Test fun deleteCategoryMergesAndCollapsesASplit() {
        seed(
            listOf(account("a", 150.0)),
            listOf(tx("t1", E, 150.0, "a", categoryId = "", splits = listOf(TransactionSplit("cat-1", 100.0), TransactionSplit("cat-24", 50.0)))),
        )
        store.deleteCategory("cat-1")
        assertNull(state.transactions[0].splits)
        assertEquals("cat-24", state.transactions[0].categoryId)
    }

    @Test fun refusesToDeleteTransferOrMiscellaneous() {
        val before = state.categories.size
        store.deleteCategory("cat-13")
        store.deleteCategory("cat-24")
        val ids = state.categories.map { it.id }
        assertTrue("cat-13" in ids && "cat-24" in ids)
        assertEquals(before, ids.size)
    }

    @Test fun neverFallsBackToTransfer() {
        store.setState { s -> s.copy(categories = s.categories.filter { it.id != "cat-24" }) }
        seed(listOf(account("a", 100.0)), listOf(tx("t1", E, 10.0, "a")))
        store.deleteCategory("cat-1")
        assertNotEquals("cat-13", state.transactions[0].categoryId)
        assertEquals("cat-24", state.transactions[0].categoryId)
    }

    // ---- deleteLabel --------------------------------------------------------------------------

    @Test fun deleteLabelDropsLabelScopedBudgets() {
        store.addBudget(NewBudget("", "lbl-1", 500.0, BudgetPeriod.Monthly, false))
        store.addBudget(NewBudget("cat-1", null, 900.0, BudgetPeriod.Monthly, false))
        store.deleteLabel("lbl-1")
        assertEquals(1, state.budgets.size)
        assertEquals("cat-1", state.budgets[0].categoryId)
        assertFalse(state.labels.any { it.id == "lbl-1" })
    }

    // ---- templates ----------------------------------------------------------------------------

    private fun coffee(note: String = "Morning coffee") = NewTemplate("Coffee", E, 150.0, "a", null, "cat-1", note)

    @Test fun addTemplateReturnsItsId() {
        val id = store.addTemplate(coffee())
        val created = state.templates[0]
        assertEquals(id, created.id)
        assertEquals("Coffee", created.name)
        assertTrue(created.createdAt.isNotEmpty())
    }

    @Test fun deleteTemplateRemovesIt() {
        store.deleteTemplate(store.addTemplate(coffee("")))
        assertTrue(state.templates.isEmpty())
    }

    @Test fun templatesAreClearedByReset() {
        store.addTemplate(coffee(""))
        store.resetToDefaults()
        assertTrue(state.templates.isEmpty())
    }

    // ---- categorization rules -----------------------------------------------------------------

    private fun addUberRule(
        pattern: String = "uber",
        categoryId: String = "cat-2",
        labelIds: List<String> = listOf("lbl-1"),
    ) = store.addRule(NewRule(pattern, RuleMatchType.Contains, RuleScope.Any, categoryId, labelIds, true))

    @Test fun rulesAreAppended() {
        addUberRule(pattern = "first")
        addUberRule(pattern = "second")
        assertEquals(listOf("first", "second"), state.rules.map(CategoryRule::pattern))
    }

    @Test fun moveRuleSwapsNeighboursAndStopsAtTheEnds() {
        val a = addUberRule(pattern = "a")
        val b = addUberRule(pattern = "b")
        store.moveRule(b, MoveDirection.Up)
        assertEquals(listOf("b", "a"), state.rules.map { it.pattern })
        store.moveRule(b, MoveDirection.Up)
        assertEquals(listOf("b", "a"), state.rules.map { it.pattern })
        store.moveRule(a, MoveDirection.Down)
        assertEquals(listOf("b", "a"), state.rules.map { it.pattern })
    }

    @Test fun applyRulesToExistingRecategorizesAndReportsForUndo() {
        seed(listOf(account("a", 1000.0)), listOf(tx("t1", E, 100.0, "a", note = "Uber to work"), tx("t2", E, 50.0, "a", note = "Groceries")))
        addUberRule()
        val (changed, previous) = store.applyRulesToExisting()
        assertEquals(1, changed)
        val (t1, t2) = state.transactions
        assertEquals("cat-2", t1.categoryId); assertEquals(listOf("lbl-1"), t1.labels)
        assertEquals("cat-1", t2.categoryId); assertEquals(emptyList<String>(), t2.labels)
        store.restoreCategorization(previous)
        assertEquals("cat-1", state.transactions[0].categoryId)
        assertEquals(emptyList<String>(), state.transactions[0].labels)
    }

    @Test fun applyingRulesLeavesBalancesAlone() {
        seed(listOf(account("a", 900.0, 1000.0)), listOf(tx("t1", E, 100.0, "a", note = "Uber")))
        addUberRule()
        store.applyRulesToExisting()
        assertEquals(900.0, state.accounts[0].balance, 0.0)
    }

    @Test fun applyRulesCanBeRestrictedToOneCategory() {
        seed(
            listOf(account("a", 1000.0)),
            listOf(tx("t1", E, 10.0, "a", note = "Uber", categoryId = "cat-24"), tx("t2", E, 10.0, "a", note = "Uber", categoryId = "cat-3")),
        )
        addUberRule()
        assertEquals(1, store.applyRulesToExisting(restrictToCategoryId = "cat-24").changed)
        assertEquals("cat-3", state.transactions[1].categoryId)
    }

    @Test fun deletingACategoryRepointsRules() {
        addUberRule(categoryId = "cat-2")
        store.deleteCategory("cat-2")
        assertEquals("cat-24", state.rules[0].categoryId)
    }

    @Test fun deletingALabelStripsItFromRules() {
        addUberRule(labelIds = listOf("lbl-1", "lbl-2"))
        store.deleteLabel("lbl-1")
        assertEquals(listOf("lbl-2"), state.rules[0].labelIds)
    }

    @Test fun rulesAreClearedByReset() {
        addUberRule()
        store.resetToDefaults()
        assertTrue(state.rules.isEmpty())
    }

    // ---- goals --------------------------------------------------------------------------------

    private fun emergencyFund(linkedAccountId: String? = null) =
        store.addGoal(NewGoal("Emergency Fund", "target", "#146b54", 10000.0, linkedAccountId = linkedAccountId))

    @Test fun addGoalReturnsItsId() {
        val id = emergencyFund()
        assertEquals(id, state.goals[0].id)
        assertEquals("Emergency Fund", state.goals[0].name)
        assertTrue(state.goals[0].createdAt.isNotEmpty())
    }

    @Test fun updateGoalChangesOnlyTheGivenFields() {
        val id = emergencyFund()
        store.updateGoal(id) { it.copy(targetAmount = 15000.0) }
        assertEquals(15000.0, state.goals[0].targetAmount, 0.0)
        assertEquals("Emergency Fund", state.goals[0].name)
    }

    @Test fun deleteGoalRemovesOnlyItsContributions() {
        val id = emergencyFund()
        val otherId = store.addGoal(NewGoal("Vacation", "plane", "#f59e0b", 5000.0))
        store.addContribution(NewContribution(id, 1000.0, "", ""))
        store.addContribution(NewContribution(otherId, 500.0, "", ""))
        store.deleteGoal(id)
        assertEquals(listOf(otherId), state.goals.map { it.id })
        assertEquals(listOf(otherId), state.goalContributions.map { it.goalId })
    }

    @Test fun deleteGoalUnlinksRecurringRulesButKeepsThem() {
        val id = emergencyFund()
        val ruleId = store.addRecurring(
            NewRecurring(E, 500.0, "a", categoryId = "cat-1", note = "", frequency = RecurrenceFrequency.Monthly, startDate = "2026-01-01T00:00:00.000Z", goalId = id),
        )
        store.deleteGoal(id)
        val rule = state.recurring.find { it.id == ruleId }
        assertNotNull(rule)
        assertNull(rule!!.goalId)
    }

    @Test fun goalsAreClearedByReset() {
        val id = emergencyFund()
        store.addContribution(NewContribution(id, 1000.0, "", ""))
        store.resetToDefaults()
        assertTrue(state.goals.isEmpty())
        assertTrue(state.goalContributions.isEmpty())
    }

    // ---- contributions ------------------------------------------------------------------------

    @Test fun addContributionReturnsItsId() {
        val id = store.addContribution(NewContribution("goal-1", 500.0, "2026-01-05", "Bonus"))
        assertEquals(id, state.goalContributions[0].id)
        assertEquals(500.0, state.goalContributions[0].amount, 0.0)
        assertTrue(state.goalContributions[0].createdAt.isNotEmpty())
    }

    @Test fun clampsAWithdrawalToWhatTheGoalHolds() {
        store.addContribution(NewContribution("goal-1", 300.0, "2026-01-05", ""))
        store.addContribution(NewContribution("goal-1", -500.0, "2026-01-06", ""))
        assertEquals(0.0, state.goalContributions.sumOf { it.amount }, 0.0)
    }

    @Test fun deleteContributionReturnsTheRow() {
        val id = store.addContribution(NewContribution("goal-1", 500.0, "2026-01-05", ""))
        assertEquals(id, store.deleteContribution(id)?.id)
        assertTrue(state.goalContributions.isEmpty())
    }

    @Test fun deleteContributionOfAMissingRowIsNull() {
        assertNull(store.deleteContribution("missing"))
    }

    @Test fun restoreContributionReinsertsVerbatim() {
        val id = store.addContribution(NewContribution("goal-1", 500.0, "2026-01-05", "Bonus"))
        val removed = store.deleteContribution(id)!!
        store.restoreContribution(removed)
        assertEquals(listOf(removed), state.goalContributions)
    }

    @Test fun restoreContributionGuardsADoubleUndo() {
        val removed = store.deleteContribution(store.addContribution(NewContribution("goal-1", 500.0, "2026-01-05", "")))!!
        store.restoreContribution(removed)
        store.restoreContribution(removed)
        assertEquals(1, state.goalContributions.size)
    }

    @Test fun deleteAccountClearsGoalLinks() {
        seed(listOf(account("a", 1000.0)))
        val id = emergencyFund(linkedAccountId = "a")
        store.deleteAccount("a")
        assertEquals(id, state.goals[0].id)
        assertNull(state.goals[0].linkedAccountId)
    }

    // ---- people -------------------------------------------------------------------------------

    @Test fun addPersonReturnsItsId() {
        val id = store.addPerson(NewPerson("Rahul", "user", "#146b54"))
        assertEquals(id, state.people[0].id)
        assertEquals("Rahul", state.people[0].name)
        assertTrue(state.people[0].createdAt.isNotEmpty())
    }

    @Test fun updatePersonChangesOnlyTheGivenFields() {
        val id = store.addPerson(NewPerson("Rahul", "user", "#146b54"))
        store.updatePerson(id) { it.copy(name = "Rahul Sharma") }
        assertEquals("Rahul Sharma", state.people[0].name)
        assertEquals("user", state.people[0].icon)
    }

    @Test fun deletePersonRemovesOnlyTheirEntries() {
        val id = store.addPerson(NewPerson("Rahul", "user", "#146b54"))
        val otherId = store.addPerson(NewPerson("Priya", "user", "#f59e0b"))
        store.addDebtEntry(NewDebtEntry(id, 500.0, "", ""))
        store.addDebtEntry(NewDebtEntry(otherId, 200.0, "", ""))
        store.deletePerson(id)
        assertEquals(listOf(otherId), state.people.map { it.id })
        assertEquals(listOf(otherId), state.debtEntries.map { it.personId })
    }

    @Test fun peopleAreClearedByReset() {
        val id = store.addPerson(NewPerson("Rahul", "user", "#146b54"))
        store.addDebtEntry(NewDebtEntry(id, 500.0, "", ""))
        store.resetToDefaults()
        assertTrue(state.people.isEmpty())
        assertTrue(state.debtEntries.isEmpty())
    }

    // ---- debt entries -------------------------------------------------------------------------

    private fun settledEntry(amount: Double = 500.0, txId: String = "settle") =
        store.addDebtEntry(NewDebtEntry("person-1", amount, "2026-01-05", "Settled up", settledTransactionId = txId))

    @Test fun addDebtEntryReturnsItsId() {
        val id = store.addDebtEntry(NewDebtEntry("person-1", 500.0, "2026-01-05", "Lunch"))
        assertEquals(id, state.debtEntries[0].id)
        assertEquals(500.0, state.debtEntries[0].amount, 0.0)
        assertTrue(state.debtEntries[0].createdAt.isNotEmpty())
    }

    @Test fun deleteDebtEntryReturnsTheRow() {
        val id = store.addDebtEntry(NewDebtEntry("person-1", 500.0, "2026-01-05", ""))
        assertEquals(id, store.deleteDebtEntry(id)?.id)
        assertTrue(state.debtEntries.isEmpty())
    }

    @Test fun deleteDebtEntryOfAMissingRowIsNull() {
        assertNull(store.deleteDebtEntry("missing"))
    }

    @Test fun restoreDebtEntryReinsertsVerbatim() {
        val id = store.addDebtEntry(NewDebtEntry("person-1", 500.0, "2026-01-05", "Lunch"))
        val removed = store.deleteDebtEntry(id)!!
        store.restoreDebtEntry(removed)
        assertEquals(listOf(removed), state.debtEntries)
    }

    @Test fun restoreDebtEntryGuardsADoubleUndo() {
        val removed = store.deleteDebtEntry(store.addDebtEntry(NewDebtEntry("person-1", 500.0, "2026-01-05", "")))!!
        store.restoreDebtEntry(removed)
        store.restoreDebtEntry(removed)
        assertEquals(1, state.debtEntries.size)
    }

    @Test fun deletingASettledEntryDeletesItsTransactionAndUndoRestoresBoth() {
        seed(listOf(account("a", 1500.0, 2000.0)), listOf(tx("settle", E, 500.0, "a")))
        val removed = store.deleteDebtEntry(settledEntry())!!
        assertTrue(state.debtEntries.isEmpty())
        assertTrue(state.transactions.isEmpty())
        assertEquals(2000.0, state.accounts[0].balance, 0.0)
        store.restoreDebtEntry(removed)
        store.restoreDebtEntry(removed)
        assertEquals(1, state.debtEntries.size)
        assertEquals(listOf("settle"), state.transactions.map { it.id })
        assertEquals(1500.0, state.accounts[0].balance, 0.0)
    }

    @Test fun deletingASettlementTransactionRemovesItsEntryAndUndoRestoresBoth() {
        seed(listOf(account("a", 1500.0, 2000.0)), listOf(tx("settle", E, 500.0, "a")))
        settledEntry()
        val other = store.addDebtEntry(NewDebtEntry("person-1", 200.0, "2026-01-06", ""))
        val removed = store.deleteTransaction("settle")!!
        assertEquals(listOf(other), state.debtEntries.map { it.id })
        assertEquals(2000.0, state.accounts[0].balance, 0.0)
        store.restoreTransaction(removed)
        store.restoreTransaction(removed)
        assertEquals(2, state.debtEntries.size)
        assertTrue(state.debtEntries.any { it.settledTransactionId == "settle" })
        assertEquals(1500.0, state.accounts[0].balance, 0.0)
    }

    @Test fun bulkDeletingASettlementRemovesItsEntryAndBulkUndoRestoresIt() {
        seed(listOf(account("a", 1500.0, 2000.0)), listOf(tx("settle", E, 500.0, "a"), tx("plain", E, 1.0, "a")))
        settledEntry()
        val removed = store.bulkDeleteTransactions(listOf("settle", "plain"))
        assertTrue(state.debtEntries.isEmpty())
        store.restoreTransactions(removed)
        assertEquals(1, state.debtEntries.size)
        assertEquals(2, state.transactions.size)
    }

    @Test fun deletesASettledEntryCleanlyWhenItsTransactionIsGone() {
        seed(listOf(account("a", 100.0)))
        store.deleteDebtEntry(settledEntry(txId = "missing"))
        assertTrue(state.debtEntries.isEmpty())
        assertEquals(100.0, state.accounts[0].balance, 0.0)
    }

    // ---- updateDebtEntry ----------------------------------------------------------------------

    private fun seedSettlement(): String {
        seed(
            listOf(account("a", 2500.0, 2000.0)),
            listOf(tx("settle", I, 500.0, "a", date = "2026-01-05T10:00:00.000Z", note = "Settled up with Rahul")),
        )
        return store.addDebtEntry(NewDebtEntry("person-1", -500.0, "2026-01-05T10:00:00.000Z", "Settled up with Rahul", "settle"))
    }

    @Test fun editsAPlainEntryIncludingFlippingItsDirection() {
        seed(listOf(account("a", 100.0)))
        val id = store.addDebtEntry(NewDebtEntry("person-1", 500.0, "2026-01-05", "Lunch"))
        assertTrue(store.updateDebtEntry(id, DebtEntryUpdate(amount = -750.0, date = "2026-01-07", note = "  Dinner ")))
        val entry = state.debtEntries[0]
        assertEquals(id, entry.id)
        assertEquals(-750.0, entry.amount, 0.0)
        assertEquals("2026-01-07", entry.date)
        assertEquals("Dinner", entry.note)
        assertEquals(100.0, state.accounts[0].balance, 0.0)
    }

    @Test fun aSettledEditUpdatesTheLinkedTransaction() {
        val id = seedSettlement()
        store.updateDebtEntry(id, DebtEntryUpdate(amount = -800.0, date = "2026-01-09T10:00:00.000Z", note = "Cash"))
        val entry = state.debtEntries[0]
        assertEquals(-800.0, entry.amount, 0.0)
        assertEquals("2026-01-09T10:00:00.000Z", entry.date)
        assertEquals("Cash", entry.note)
        val t = state.transactions[0]
        assertEquals("settle", t.id); assertEquals(I, t.type); assertEquals(800.0, t.amount, 0.0)
        assertEquals("2026-01-09T10:00:00.000Z", t.date)
        assertEquals("Settled up with Rahul", t.note)
        assertEquals(2800.0, state.accounts[0].balance, 0.0)
        assertEquals(2000.0, state.accounts[0].openingBalance, 0.0)
    }

    @Test fun aSettledEntryKeepsItsDirection() {
        val id = seedSettlement()
        store.updateDebtEntry(id, DebtEntryUpdate(amount = 300.0))
        assertEquals(-300.0, state.debtEntries[0].amount, 0.0)
        assertEquals(I, state.transactions[0].type)
        assertEquals(300.0, state.transactions[0].amount, 0.0)
        assertEquals(2300.0, state.accounts[0].balance, 0.0)
    }

    @Test fun updateTransactionCarriesAmountAndDateToItsEntry() {
        val id = seedSettlement()
        store.updateTransaction("settle") { it.copy(amount = 650.0, date = "2026-01-06T09:00:00.000Z") }
        val entry = state.debtEntries.first { it.id == id }
        assertEquals(-650.0, entry.amount, 0.0)
        assertEquals("2026-01-06T09:00:00.000Z", entry.date)
        assertEquals("Settled up with Rahul", entry.note)
        assertEquals(2650.0, state.accounts[0].balance, 0.0)
    }

    @Test fun updateTransactionLeavesEntriesAloneWhenAmountAndDateAreUnchanged() {
        seedSettlement()
        val before = state.debtEntries
        store.updateTransaction("settle") { it.copy(note = "Renamed") }
        assertSame(before, state.debtEntries)
    }

    @Test fun ignoresAZeroAmountAndAnUnknownIdIsANoOp() {
        val id = seedSettlement()
        val before = state
        assertFalse(store.updateDebtEntry("missing", DebtEntryUpdate(amount = 1.0)))
        assertSame(before, state)
        store.updateDebtEntry(id, DebtEntryUpdate(amount = 0.0))
        assertEquals(-500.0, state.debtEntries[0].amount, 0.0)
        assertEquals(500.0, state.transactions[0].amount, 0.0)
        assertEquals(2500.0, state.accounts[0].balance, 0.0)
    }

    @Test fun editsASettledEntryCleanlyWhenItsTransactionIsGone() {
        seed(listOf(account("a", 100.0)))
        val id = settledEntry(txId = "missing")
        store.updateDebtEntry(id, DebtEntryUpdate(amount = 900.0))
        assertEquals(900.0, state.debtEntries[0].amount, 0.0)
        assertEquals(100.0, state.accounts[0].balance, 0.0)
    }

    @Test fun flipsTheSettledEntryWhenItsTransactionChangesType() {
        seed(listOf(account("a", 1500.0, 2000.0)), listOf(tx("settle", E, 500.0, "a")))
        settledEntry()
        store.updateTransaction("settle") { it.copy(type = I) }
        assertEquals(-500.0, state.debtEntries[0].amount, 0.0)
        store.updateTransaction("settle") { it.copy(type = E) }
        assertEquals(500.0, state.debtEntries[0].amount, 0.0)
    }

    // ---- settleUp (moved from Debts.tsx) ------------------------------------------------------

    @Test fun settleUpCreatesTheTransactionAndBalancingEntryAtomically() {
        seed(listOf(account("a", 1000.0)))
        val rahul = store.addPerson(NewPerson("Rahul", "user", "#000"))
        store.addDebtEntry(NewDebtEntry(rahul, 800.0, "2026-06-01", "Lent"))
        val result = store.settleUp(rahul, 500.0, "a", "")!!
        val t = state.transactions.single()
        assertEquals(result.transactionId, t.id)
        assertEquals(I, t.type)
        assertEquals("cat-24", t.categoryId)
        assertEquals("Settled up with Rahul", t.note)
        val entry = state.debtEntries.first { it.id == result.entryId }
        assertEquals(-500.0, entry.amount, 0.0)
        assertEquals(t.id, entry.settledTransactionId)
        assertEquals(1500.0, state.accounts[0].balance, 0.0)
        // Undo is one deleteTransaction: it takes the settled entry with it.
        store.deleteTransaction(result.transactionId)
        assertEquals(1, state.debtEntries.size)
        assertEquals(1000.0, state.accounts[0].balance, 0.0)
    }

    @Test fun settleUpPaysOutWhenYouOweThem() {
        seed(listOf(account("a", 1000.0)))
        val priya = store.addPerson(NewPerson("Priya", "user", "#000"))
        store.addDebtEntry(NewDebtEntry(priya, -300.0, "2026-06-01", "Borrowed"))
        val result = store.settleUp(priya, 300.0, "a", "  Paid back ")!!
        assertEquals(E, state.transactions[0].type)
        assertEquals("Paid back", state.transactions[0].note)
        assertEquals(300.0, state.debtEntries.first { it.id == result.entryId }.amount, 0.0)
        assertEquals(700.0, state.accounts[0].balance, 0.0)
    }

    @Test fun settleUpRefusesInvalidInput() {
        seed(listOf(account("a", 1000.0)))
        val rahul = store.addPerson(NewPerson("Rahul", "user", "#000"))
        store.addDebtEntry(NewDebtEntry(rahul, 800.0, "2026-06-01", "Lent"))
        val before = state
        assertNull(store.settleUp("missing", 100.0, "a"))
        assertNull(store.settleUp(rahul, 0.0, "a"))
        assertNull(store.settleUp(rahul, 800.01, "a"))
        assertNull(store.settleUp(rahul, 100.0, ""))
        assertSame(before, state)
        assertNotNull(store.settleUp(rahul, 800.004, "a"))
    }

    // ---- importData ---------------------------------------------------------------------------

    private val payload = ImportPayload(
        accounts = listOf(account("imported", 0.0, 1000.0)),
        transactions = listOf(tx("it1", E, 250.0, "imported")),
    )

    @Test fun replaceRecomputesBalancesFromTheImportedTransactions() {
        seed(listOf(account("local", 4000.0)), listOf(tx("lt1", I, 4000.0, "local")))
        store.importData(payload, ImportMode.Replace)
        assertEquals(listOf("imported"), state.accounts.map { it.id })
        assertEquals(750.0, state.accounts[0].balance, 0.0)
        assertEquals(listOf("it1"), state.transactions.map { it.id })
    }

    @Test fun mergeIsByIdWithIncomingWinning() {
        seed(listOf(account("local", 4000.0, 4000.0), account("imported", 0.0, 0.0)), listOf(tx("lt1", I, 0.0, "local")))
        store.importData(payload, ImportMode.Merge)
        assertEquals(listOf("imported", "local"), state.accounts.map { it.id }.sorted())
        assertEquals(listOf("it1", "lt1"), state.transactions.map { it.id }.sorted())
        assertEquals(750.0, state.accounts.first { it.id == "imported" }.balance, 0.0)
        assertEquals(4000.0, state.accounts.first { it.id == "local" }.balance, 0.0)
    }

    @Test fun derivesAnOpeningBalanceForAccountsImportedWithoutOne() {
        val legacy = ImportPayload(
            accounts = listOf(account("legacy", 750.0, 0.0)),
            transactions = listOf(tx("lg1", E, 250.0, "legacy")),
            accountsMissingOpeningBalance = setOf("legacy"),
        )
        store.importData(legacy, ImportMode.Replace)
        assertEquals(1000.0, state.accounts[0].openingBalance, 0.0)
        assertEquals(750.0, state.accounts[0].balance, 0.0)
    }

    @Test fun replaceEmptiesMissingCollectionsButKeepsCategoriesAndLabels() {
        seed(listOf(account("local", 100.0)))
        store.addGoal(NewGoal("Old goal", "target", "#146b54", 1000.0))
        store.setState {
            it.copy(netWorthSnapshots = listOf(NetWorthSnapshot("snap", "2026-03", "2026-03-31", 500.0, 0.0, "2026-04-01T00:00:00.000Z")))
        }
        val categories = state.categories
        val labels = state.labels
        store.importData(ImportPayload(transactions = emptyList()), ImportMode.Replace)
        assertSame(categories, state.categories)
        assertSame(labels, state.labels)
        assertTrue(state.accounts.isEmpty())
        assertTrue(state.goals.isEmpty())
        assertTrue(state.netWorthSnapshots.isEmpty())
    }

    @Test fun mergeLeavesMissingCollectionsAlone() {
        seed(listOf(account("local", 100.0)))
        store.importData(ImportPayload(transactions = emptyList()), ImportMode.Merge)
        assertEquals(listOf("local"), state.accounts.map { it.id })
    }

    @Test fun mergesGoalsAndContributionsById() {
        val localId = store.addGoal(NewGoal("Local Goal", "target", "#146b54", 1000.0))
        store.importData(
            ImportPayload(
                goals = listOf(Goal("imported-goal", "Imported Goal", "plane", "#f59e0b", 5000.0, createdAt = "2026-01-01T00:00:00.000Z")),
                goalContributions = listOf(
                    GoalContribution("imported-contrib", "imported-goal", 1000.0, "2026-01-02T00:00:00.000Z", "", "2026-01-02T00:00:00.000Z"),
                ),
            ),
            ImportMode.Merge,
        )
        assertEquals(listOf(localId, "imported-goal").sorted(), state.goals.map { it.id }.sorted())
        assertEquals(listOf("imported-contrib"), state.goalContributions.map { it.id })
    }

    @Test fun mergesPeopleAndDebtEntriesById() {
        val localId = store.addPerson(NewPerson("Local Person", "user", "#146b54"))
        store.importData(
            ImportPayload(
                people = listOf(Person("imported-person", "Imported Person", "handshake", "#f59e0b", "2026-01-01T00:00:00.000Z")),
                debtEntries = listOf(DebtEntry("imported-entry", "imported-person", 500.0, "2026-01-02T00:00:00.000Z", "", createdAt = "2026-01-02T00:00:00.000Z")),
            ),
            ImportMode.Merge,
        )
        assertEquals(listOf(localId, "imported-person").sorted(), state.people.map { it.id }.sorted())
        assertEquals(listOf("imported-entry"), state.debtEntries.map { it.id })
    }

    @Test fun importKeepsThisDevicesOnboardedAt() {
        store.updateSettings { it.copy(onboardedAt = "2026-01-01T00:00:00.000Z", userName = "Me") }
        store.importData(ImportPayload(settings = defaultSettings.copy(userName = "Backup", theme = Theme.Dark)), ImportMode.Merge)
        assertEquals("2026-01-01T00:00:00.000Z", state.settings.onboardedAt)
        assertEquals("Backup", state.settings.userName)
        assertEquals(Theme.Dark, state.settings.theme)
    }

    // ---- addBudget ----------------------------------------------------------------------------

    @Test fun addBudgetReplacesTheSameScope() {
        store.addBudget(NewBudget("cat-1", null, 1000.0, BudgetPeriod.Monthly, false))
        store.addBudget(NewBudget("cat-1", null, 2000.0, BudgetPeriod.Monthly, false))
        assertEquals(1, state.budgets.size)
        assertEquals(2000.0, state.budgets[0].amount, 0.0)
    }

    @Test fun overallCategoryAndLabelBudgetsCoexist() {
        store.addBudget(NewBudget("", null, 1000.0, BudgetPeriod.Monthly, false))
        store.addBudget(NewBudget("cat-1", null, 1000.0, BudgetPeriod.Monthly, false))
        store.addBudget(NewBudget("", "lbl-1", 1000.0, BudgetPeriod.Monthly, false))
        assertEquals(3, state.budgets.size)
    }

    // ---- recurring ----------------------------------------------------------------------------

    private fun recurringRule(
        type: TransactionType = E,
        toAccountId: String? = null,
        goalId: String? = null,
    ) = RecurringTransaction(
        id = "r1", type = type, amount = 300.0, accountId = "a", toAccountId = toAccountId, categoryId = "cat-1",
        note = "", labels = emptyList(), frequency = RecurrenceFrequency.Monthly, startDate = "2020-01-01T00:00:00.000Z",
        maxOccurrences = 1, occurrenceCount = 0, lastRunDate = null, createdAt = "2020-01-01T00:00:00.000Z", goalId = goalId,
    )

    private fun setRules(vararg rules: RecurringTransaction) = store.setState { it.copy(recurring = rules.toList()) }

    @Test fun generatesATransferOccurrenceAndMovesBothBalances() {
        seed(listOf(account("a", 1000.0), account("b", 500.0)))
        setRules(recurringRule(type = T, toAccountId = "b"))
        assertEquals(1, store.processRecurring().size)
        assertEquals("b", state.transactions[0].toAccountId)
        assertEquals(700.0, state.accounts.first { it.id == "a" }.balance, 0.0)
        assertEquals(800.0, state.accounts.first { it.id == "b" }.balance, 0.0)
        assertEquals(1, state.recurring[0].occurrenceCount)
        assertTrue(store.processRecurring().isEmpty())
    }

    @Test fun skipsATransferRuleWhoseDestinationIsGone() {
        seed(listOf(account("a", 1000.0)))
        setRules(recurringRule(type = T, toAccountId = "missing"))
        assertTrue(store.processRecurring().isEmpty())
        assertEquals(1000.0, state.accounts[0].balance, 0.0)
    }

    @Test fun generatesNothingWhilePausedAndResumesCleanly() {
        seed(listOf(account("a", 1000.0)))
        setRules(recurringRule())
        store.setRecurringPaused("r1", true)
        assertTrue(store.processRecurring().isEmpty())
        store.setRecurringPaused("r1", false)
        assertNull(state.recurring[0].pausedAt)
        assertEquals(1, store.processRecurring().size)
    }

    @Test fun generatedRowsCanBeUndoneThroughBulkDelete() {
        seed(listOf(account("a", 1000.0)))
        setRules(recurringRule())
        val generated = store.processRecurring()
        assertEquals(1, generated.size)
        assertEquals("r1", generated[0].recurringId)
        store.bulkDeleteTransactions(generated.map { it.id })
        assertTrue(state.transactions.isEmpty())
        assertEquals(1000.0, state.accounts[0].balance, 0.0)
    }

    @Test fun dropsTransferRulesPointingAtADeletedAccount() {
        seed(listOf(account("a", 1000.0), account("b", 500.0)))
        setRules(recurringRule(type = T, toAccountId = "b"))
        store.deleteAccount("b")
        assertTrue(state.recurring.isEmpty())
    }

    @Test fun autoFundsALinkedGoal() {
        seed(listOf(account("a", 1000.0)))
        val goalId = emergencyFund()
        setRules(recurringRule(goalId = goalId))
        store.processRecurring()
        assertEquals(1, state.goalContributions.size)
        assertEquals(goalId, state.goalContributions[0].goalId)
        assertEquals(300.0, state.goalContributions[0].amount, 0.0)
    }

    @Test fun neverResurrectsAContributionForADeletedGoal() {
        seed(listOf(account("a", 1000.0)))
        setRules(recurringRule(goalId = "deleted-goal"))
        store.processRecurring()
        assertTrue(state.goalContributions.isEmpty())
    }

    // ---- migrations ---------------------------------------------------------------------------

    private val accountJson = """{"id":"a","name":"a","type":"checking","color":"#000","icon":"landmark","balance":100,"openingBalance":100,"createdAt":"2026-01-01T00:00:00.000Z"}"""

    private fun envelope(version: Int, state: String) = """{"version":$version,"state":$state}"""

    @Test fun v7GivesBudgetsAndRulesTheirPreV7Behaviour() {
        val s = decodePersisted(
            envelope(
                6,
                """{"accounts":[$accountJson],"transactions":[],
                "budgets":[{"id":"b1","categoryId":"cat-1","amount":500,"createdAt":"2026-01-01"}],
                "recurring":[{"id":"r1","type":"expense","amount":10,"accountId":"a","categoryId":"cat-1","note":"","labels":[],"frequency":"monthly","startDate":"2026-01-01T00:00:00.000Z","lastRunDate":null,"createdAt":"2026-01-01T00:00:00.000Z"}],
                "settings":{"theme":"dark","userName":"Alex","autoLocalBackup":false}}""",
            ),
            now,
        )
        assertEquals(BudgetPeriod.Monthly, s.budgets[0].period)
        assertFalse(s.budgets[0].rollover)
        assertEquals(0, s.recurring[0].occurrenceCount)
        assertEquals(1, s.settings.monthStartDay)
        assertEquals("Alex", s.settings.userName)
    }

    @Test fun v5BackfillsOpeningBalancesWithoutMovingAnyBalance() {
        val s = decodePersisted(
            envelope(
                4,
                """{"accounts":[
                  {"id":"a","name":"a","type":"checking","color":"#000","icon":"landmark","balance":750,"createdAt":"2026-01-01T00:00:00.000Z","currency":"INR"},
                  {"id":"b","name":"b","type":"checking","color":"#000","icon":"landmark","balance":1200,"createdAt":"2026-01-01T00:00:00.000Z"}],
                "transactions":[
                  {"id":"t1","type":"expense","amount":250,"accountId":"a","categoryId":"cat-1","date":"2026-06-01T00:00:00.000Z","note":"","labels":[],"createdAt":"2026-06-01T00:00:00.000Z"},
                  {"id":"t2","type":"transfer","amount":200,"accountId":"a","toAccountId":"b","categoryId":"cat-1","date":"2026-06-01T00:00:00.000Z","note":"","labels":[],"createdAt":"2026-06-01T00:00:00.000Z"}]}""",
            ),
            now,
        )
        assertEquals(listOf(750.0, 1200.0), s.accounts.map { it.balance })
        assertEquals(listOf(1200.0, 1000.0), s.accounts.map { it.openingBalance })
        store.replaceState(s)
        assertEquals(0, store.recomputeBalances().changed)
    }

    private val settingsV8 = """{"theme":"dark","userName":"Alex","autoLocalBackup":false,"monthStartDay":1,"hideAmounts":false}"""

    @Test fun v9SeedsEmptyGoals() {
        val s = decodePersisted(envelope(8, """{"accounts":[$accountJson],"transactions":[],"settings":$settingsV8}"""), now)
        assertTrue(s.goals.isEmpty())
        assertTrue(s.goalContributions.isEmpty())
    }

    @Test fun v10SeedsEmptyPeople() {
        val s = decodePersisted(envelope(9, """{"accounts":[$accountJson],"transactions":[],"settings":$settingsV8}"""), now)
        assertTrue(s.people.isEmpty())
        assertTrue(s.debtEntries.isEmpty())
    }

    @Test fun v11SeedsEmptyRulesAndRecategorizesNothing() {
        val s = decodePersisted(
            envelope(
                10,
                """{"accounts":[$accountJson],"transactions":[{"id":"t1","type":"expense","amount":20,"accountId":"a","categoryId":"cat-1","date":"2026-06-01T00:00:00.000Z","note":"Uber","labels":[],"createdAt":"2026-06-01T00:00:00.000Z"}],"settings":$settingsV8}""",
            ),
            now,
        )
        assertTrue(s.rules.isEmpty())
        assertEquals("cat-1", s.transactions[0].categoryId)
    }

    @Test fun v8SeedsHideAmountsAndTemplates() {
        val s = decodePersisted(
            envelope(7, """{"accounts":[$accountJson],"transactions":[],"settings":{"theme":"dark","userName":"Alex","autoLocalBackup":false,"monthStartDay":1}}"""),
            now,
        )
        assertFalse(s.settings.hideAmounts)
        assertEquals("Alex", s.settings.userName)
        assertTrue(s.templates.isEmpty())
    }

    @Test fun v6MarksAnExistingInstallAsOnboarded() {
        val s = decodePersisted(
            envelope(5, """{"accounts":[$accountJson],"transactions":[],"settings":{"theme":"dark","userName":"Alex","autoLocalBackup":false}}"""),
            now,
        )
        assertEquals(now.toIso(), s.settings.onboardedAt)
        assertEquals("Alex", s.settings.userName)
        assertEquals(Theme.Dark, s.settings.theme)
    }

    @Test fun aFreshInstallIsNotOnboarded() {
        assertNull(defaultSettings.onboardedAt)
        assertEquals("", defaultSettings.userName)
        assertNull(initialFinanceState().settings.onboardedAt)
    }

    @Test fun staysOnboardedAfterADataReset() {
        store.updateSettings { defaultSettings.copy(userName = "Riya", onboardedAt = "2026-01-01T00:00:00.000Z") }
        seed(listOf(account("a", 100.0)), listOf(tx("t1", E, 10.0, "a")))
        store.resetToDefaults()
        assertTrue(state.accounts.isEmpty())
        assertTrue(state.transactions.isEmpty())
        assertEquals("2026-01-01T00:00:00.000Z", state.settings.onboardedAt)
        assertEquals("Riya", state.settings.userName)
    }

    // ---- captureNetWorthSnapshots -------------------------------------------------------------

    @Test fun freezesEachCompletedMonthOnce() {
        seed(
            listOf(account("a", 10000.0, 3000.0)),
            listOf(tx("t1", I, 4000.0, "a", date = "2026-04-10T00:00:00.000Z"), tx("t2", I, 3000.0, "a", date = "2026-05-10T00:00:00.000Z")),
        )
        assertEquals(2, store.captureNetWorthSnapshots())
        assertEquals(listOf("2026-04", "2026-05"), state.netWorthSnapshots.map { it.periodKey })
        assertEquals(7000.0, state.netWorthSnapshots[0].assets, 0.0)
        assertEquals(0, store.captureNetWorthSnapshots())
    }

    @Test fun capturedMonthsStaySteadyWhenHistoryIsEdited() {
        seed(listOf(account("a", 10000.0, 6000.0)), listOf(tx("t1", I, 4000.0, "a", date = "2026-05-10T00:00:00.000Z")))
        store.captureNetWorthSnapshots()
        val before = state.netWorthSnapshots[0]
        store.deleteTransaction("t1")
        assertEquals(before, state.netWorthSnapshots[0])
    }

    @Test fun snapshotsAreClearedByReset() {
        seed(listOf(account("a", 10000.0, 6000.0)), listOf(tx("t1", I, 4000.0, "a", date = "2026-05-10T00:00:00.000Z")))
        store.captureNetWorthSnapshots()
        assertEquals(1, state.netWorthSnapshots.size)
        store.resetToDefaults()
        assertTrue(state.netWorthSnapshots.isEmpty())
    }

    // ---- importData with snapshots ------------------------------------------------------------

    private fun snapshot(periodKey: String, assets: Double, createdAt: String) =
        NetWorthSnapshot("snap-$periodKey-$createdAt", periodKey, "$periodKey-28T23:59:59.999Z", assets, 0.0, createdAt)

    @Test fun keepsOneSnapshotPerMonthPreferringTheLaterCapture() {
        store.setState { it.copy(netWorthSnapshots = listOf(snapshot("2026-04", 1000.0, "2026-05-01T00:00:00.000Z"))) }
        store.importData(ImportPayload(netWorthSnapshots = listOf(snapshot("2026-04", 2000.0, "2026-05-02T00:00:00.000Z"))), ImportMode.Merge)
        assertEquals(1, state.netWorthSnapshots.size)
        assertEquals(2000.0, state.netWorthSnapshots[0].assets, 0.0)
    }

    @Test fun replaceSwapsTheSnapshotListOutright() {
        store.setState { it.copy(netWorthSnapshots = listOf(snapshot("2026-03", 500.0, "2026-04-01T00:00:00.000Z"))) }
        store.importData(ImportPayload(netWorthSnapshots = listOf(snapshot("2026-04", 900.0, "2026-05-01T00:00:00.000Z"))), ImportMode.Replace)
        assertEquals(listOf("2026-04"), state.netWorthSnapshots.map { it.periodKey })
    }

    @Test fun v12SeedsAnEmptySnapshotList() {
        val s = decodePersisted(envelope(11, """{"accounts":[$accountJson],"transactions":[],"settings":$settingsV8}"""), now)
        assertTrue(s.netWorthSnapshots.isEmpty())
    }

    // ---- v13 ----------------------------------------------------------------------------------

    private fun preV13(extra: String = "") = envelope(
        12,
        """{"accounts":[$accountJson],"transactions":[],"settings":{"theme":"dark","userName":"Alex","autoLocalBackup":false,"monthStartDay":1,"hideAmounts":false$extra}}""",
    )

    @Test fun v13LeavesRemindersOff() {
        assertFalse(decodePersisted(preV13(), now).settings.notificationsEnabled)
    }

    @Test fun v13DefaultsThePerTriggerSwitchesOn() {
        val s = decodePersisted(preV13(), now).settings
        assertTrue(s.notifyBills && s.notifyBudgets && s.notifyCreditDue && s.notifyDailyLog)
        assertEquals(2, s.notifyLeadDays)
    }

    @Test fun v13PreservesExistingSettings() {
        val s = decodePersisted(preV13(""","theme":"dark","monthStartDay":25"""), now).settings
        assertEquals(Theme.Dark, s.theme)
        assertEquals("Alex", s.userName)
        assertEquals(25, s.monthStartDay)
    }

    @Test fun persistedStateRoundTrips() {
        seed(listOf(account("a", 800.0, 1000.0)), listOf(tx("t1", E, 200.0, "a")))
        store.addRecurring(NewRecurring(E, 10.0, "a", categoryId = "cat-1", note = "", frequency = RecurrenceFrequency.Monthly, startDate = "2026-01-01T00:00:00.000Z"))
        assertEquals(state, decodePersisted(encodePersisted(state), now))
        assertEquals(initialFinanceState(), decodePersisted("not json", now))
    }

    // ---- deposits -----------------------------------------------------------------------------

    private fun day(d: String) = iso(d).toIso()

    private fun fd(start: String, maturity: String, rate: Double = 6.65, amount: Double = 50000.0) =
        DepositTerms(amount = amount, interestRate = rate, startDate = day(start), maturityDate = day(maturity), linkedAccountId = "bank")

    @Test fun fundsAnFdFromItsSourceAccount() {
        now = iso("2026-09-15")
        seed(listOf(account("bank", 100000.0)))
        val id = store.addDeposit(NewDeposit(AccountType.Fd, "FD", "#000", fd("2026-10-01", "2029-09-01")))
        assertEquals(50000.0, state.accounts.first { it.id == "bank" }.balance, 0.0)
        val deposit = state.accounts.first { it.id == id }
        assertEquals(50000.0, deposit.balance, 0.0)
        assertEquals(0.0, deposit.openingBalance, 0.0)
        assertEquals(DepositCompounding.Quarterly, deposit.deposit?.compounding)
        assertEquals(1, state.transactions.size)
        val t = state.transactions[0]
        assertEquals(T, t.type); assertEquals("bank", t.accountId); assertEquals(id, t.toAccountId)
    }

    private fun pastFd(deductPast: Boolean): String {
        now = iso("2026-10-15")
        seed(listOf(account("bank", 100000.0)))
        return store.addDeposit(NewDeposit(AccountType.Fd, "FD", "#000", fd("2026-07-05", "2029-07-05", rate = 6.5), deductPast))
    }

    @Test fun treatsAPastFdAsAnOpeningBalanceWhenNotDeducting() {
        val id = pastFd(false)
        val deposit = state.accounts.first { it.id == id }
        assertEquals(50000.0, deposit.balance, 0.0)
        assertEquals(50000.0, deposit.openingBalance, 0.0)
        assertTrue(state.transactions.isEmpty())
        assertEquals(100000.0, state.accounts.first { it.id == "bank" }.balance, 0.0)
    }

    @Test fun postsAPastFdFundingTransferWhenDeducting() {
        val id = pastFd(true)
        assertEquals(50000.0, state.accounts.first { it.id == "bank" }.balance, 0.0)
        assertEquals(50000.0, state.accounts.first { it.id == id }.balance, 0.0)
        assertEquals(0.0, state.accounts.first { it.id == id }.openingBalance, 0.0)
        assertEquals(1, state.transactions.size)
    }

    private fun rd(deductPast: Boolean): String {
        now = iso("2026-10-15")
        seed(listOf(account("bank", 100000.0)))
        return store.addDeposit(
            NewDeposit(
                AccountType.Rd, "RD", "#000",
                DepositTerms(amount = 5000.0, interestRate = 7.0, startDate = day("2026-07-05"), tenureMonths = 12, linkedAccountId = "bank"),
                deductPast,
            ),
        )
    }

    @Test fun foldsPastRdInstallmentsIntoTheOpeningBalance() {
        val id = rd(false)
        val deposit = state.accounts.first { it.id == id }
        assertEquals(20000.0, deposit.openingBalance, 0.0)
        val rule = state.recurring.first { it.id == deposit.deposit?.recurringId }
        assertEquals(4, rule.occurrenceCount)
        assertEquals(day("2026-10-05"), rule.lastRunDate)
        assertTrue(store.processRecurring().isEmpty())
        assertEquals(100000.0, state.accounts.first { it.id == "bank" }.balance, 0.0)
    }

    @Test fun postsPastRdInstallmentsWhenDeducting() {
        val id = rd(true)
        assertEquals(4, store.processRecurring().size)
        assertEquals(80000.0, state.accounts.first { it.id == "bank" }.balance, 0.0)
        assertEquals(20000.0, state.accounts.first { it.id == id }.balance, 0.0)
    }

    @Test fun paysOutAMaturedFdExactlyOnceAndArchivesIt() {
        now = iso("2026-10-01")
        seed(listOf(account("bank", 100000.0)))
        val id = store.addDeposit(NewDeposit(AccountType.Fd, "FD", "#000", fd("2026-10-01", "2029-09-01")))
        assertTrue(store.processMaturities().isEmpty())

        now = iso("2029-09-02")
        val posted = store.processMaturities()
        assertEquals(listOf(I, T), posted.map { it.type })
        assertEquals(10621.08, posted[0].amount, 0.0)
        assertEquals("cat-23", posted[0].categoryId)
        val deposit = state.accounts.first { it.id == id }
        assertEquals(0.0, deposit.balance, 0.0)
        assertNotNull(deposit.archivedAt)
        assertEquals(day("2029-09-01"), deposit.deposit?.maturedAt)
        assertEquals(110621.08, state.accounts.first { it.id == "bank" }.balance, 0.0)
        assertTrue(store.processMaturities().isEmpty())
    }

    @Test fun refusesToDeleteAnAccountAnOpenDepositPaysInto() {
        now = iso("2026-09-15")
        seed(listOf(account("bank", 100000.0)))
        val id = store.addDeposit(NewDeposit(AccountType.Fd, "FD", "#000", fd("2026-10-01", "2029-09-01")))
        assertFalse(store.deleteAccount("bank"))
        assertEquals(2, state.accounts.size)
        assertTrue(store.deleteAccount(id))
        assertEquals(100000.0, state.accounts[0].balance, 0.0)
        assertTrue(store.deleteAccount("bank"))
    }

    // ---- loans --------------------------------------------------------------------------------

    private fun pastLoan(logPastEmis: Boolean): RecurringTransaction {
        now = iso("2026-10-15")
        seed(listOf(account("a", 100_000.0, 100_000.0)))
        val loanId = store.addLoan(NewLoan("Bike", 36_000.0, 0.0, 12, day("2026-07-05"), "a", "cat-1"), logPastEmis)
        val loan = state.loans.first { it.id == loanId }
        return state.recurring.first { it.id == loan.recurringId }
    }

    @Test fun treatsPastEmisAsAlreadyPaidByDefault() {
        val rule = pastLoan(false)
        assertEquals(4, rule.occurrenceCount)
        assertNotNull(rule.lastRunDate)
        assertTrue(store.processRecurring().isEmpty())
        assertEquals(100_000.0, state.accounts[0].balance, 0.0)
    }

    @Test fun postsPastEmisWhenLogPastEmisIsOn() {
        val rule = pastLoan(true)
        assertEquals(0, rule.occurrenceCount)
        assertNull(rule.lastRunDate)
        val posted = store.processRecurring()
        assertEquals(4, posted.size)
        assertTrue(posted.all { it.type == E && it.amount == 3000.0 })
        assertEquals(88_000.0, state.accounts[0].balance, 0.0)
    }

    private fun carLoan(): String {
        seed(listOf(account("a", 1_000_000.0, 1_000_000.0)))
        return store.addLoan(NewLoan("Car", 500_000.0, 0.0, 36, now.plusSeconds(40L * 86_400).toIso(), "a", "cat-1"))
    }

    @Test fun capsAnOversizedPrepaymentAndClosesTheLoan() {
        val loanId = carLoan()
        assertNotNull(store.addLoanPrepayment(NewLoanPrepayment(loanId, 9_999_999.0, now.toIso(), "")))
        assertEquals(500_000.0, state.loanPrepayments[0].amount, 0.0)
        assertEquals(500_000.0, state.accounts[0].balance, 0.0)
        assertNotNull(state.loans[0].closedAt)
        assertNotNull(state.recurring[0].pausedAt)
    }

    @Test fun recordsAPartialPrepaymentAndLeavesTheLoanOpen() {
        val loanId = carLoan()
        store.addLoanPrepayment(NewLoanPrepayment(loanId, 100_000.0, now.toIso(), ""))
        assertEquals(100_000.0, state.loanPrepayments[0].amount, 0.0)
        assertNull(state.loans[0].closedAt)
    }

    @Test fun refusesOnceNothingIsLeftToPrepay() {
        val loanId = carLoan()
        store.addLoanPrepayment(NewLoanPrepayment(loanId, 500_000.0, now.toIso(), ""))
        assertNull(store.addLoanPrepayment(NewLoanPrepayment(loanId, 1.0, now.toIso(), "")))
    }

    // ---- state flow ---------------------------------------------------------------------------

    @Test fun everyActionPublishesOneCompleteState() {
        seed(listOf(account("a", 100.0)))
        val seen = mutableListOf<Int>()
        store.addTransaction(NewTransaction(E, 10.0, "a", categoryId = "cat-1", date = "2026-06-01T00:00:00.000Z", note = "  x  "))
        seen += state.transactions.size
        assertEquals(listOf(1), seen)
        assertEquals("x", state.transactions[0].note)
        assertEquals(90.0, state.accounts[0].balance, 0.0)
        assertSame(store.state.value, state)
    }
}
