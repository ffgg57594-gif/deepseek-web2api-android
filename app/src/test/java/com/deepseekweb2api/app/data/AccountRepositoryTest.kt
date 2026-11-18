package com.deepseekweb2api.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * اختبار JVM مستقل لـ AccountRepository.
 * لا يحتاج Android Emulator — يعمل على أي JVM.
 */
class AccountRepositoryTest {

    @Test
    fun addAndRemoveAccount() {
        val repo = AccountRepository()
        val acct = Account.Builder().email("test@example.com").token("token123").build()
        repo.add(acct)

        assertEquals(1, repo.accounts.value.size)
        assertEquals("test@example.com", repo.accounts.value.first().email)
        assertEquals("idle", repo.accounts.value.first().state)

        repo.remove(acct.id)
        assertEquals(0, repo.accounts.value.size)
    }

    @Test
    fun setStateAndMarkRecovered() {
        val repo = AccountRepository()
        val acct = Account.Builder().token("token123").build()
        repo.add(acct)

        repo.setState(acct.id, "error", "فشل الاتصال")
        val errorAcct = repo.accounts.value.first { it.id == acct.id }
        assertEquals("error", errorAcct.state)
        assertEquals("فشل الاتصال", errorAcct.lastError)
        assertEquals(1, errorAcct.errorCount)

        repo.markRecovered(acct.id)
        val recoveredAcct = repo.accounts.value.first { it.id == acct.id }
        assertEquals("idle", recoveredAcct.state)
        assertEquals(0, recoveredAcct.errorCount)
        assertNull(recoveredAcct.lastError)
    }

    @Test
    fun statsShouldReturnCorrectCounts() {
        val repo = AccountRepository()
        repo.add(Account.Builder().token("a").build())   // idle
        repo.add(Account.Builder().token("b").build())   // idle
        repo.add(Account.Builder().token("c").build())   // idle

        repo.setState(repo.accounts.value[0].id, "busy")
        repo.setState(repo.accounts.value[1].id, "error", "خطأ")

        val stats = repo.stats
        assertEquals(3, stats.total)
        assertEquals(1, stats.idle)
        assertEquals(1, stats.busy)
        assertEquals(1, stats.error)
    }

    @Test
    fun nextIdleAccountShouldReturnIdleAccount() {
        val repo = AccountRepository()
        val idle1 = Account.Builder().token("idle1").state("idle").build()
        val idle2 = Account.Builder().token("idle2").state("idle").build()
        val busy = Account.Builder().token("busy").state("busy").build()
        val error = Account.Builder().token("err").state("error").build()

        repo.add(idle1)
        repo.add(idle2)
        repo.add(busy)
        repo.add(error)

        val next = repo.nextIdleAccount()
        assertTrue("يجب إرجاع حساب بحالة idle", next != null && next.state == "idle")
        assertTrue("يجب أن يكون أحد الحسابات الخاملة", listOf(idle1.id, idle2.id).contains(next?.id))
    }

    @Test
    fun updateAccountShouldModifyFields() {
        val repo = AccountRepository()
        val acct = Account.Builder().email("old@test.com").token("old-token").build()
        repo.add(acct)

        repo.update(acct.id) {
            email("new@test.com")
            token("new-token")
            state("busy")
        }

        val updated = repo.accounts.value.first { it.id == acct.id }
        assertEquals("new@test.com", updated.email)
        assertEquals("new-token", updated.token)
        assertEquals("busy", updated.state)
    }
}
