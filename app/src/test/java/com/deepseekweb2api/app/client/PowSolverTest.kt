package com.deepseekweb2api.app.client

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * اختبار JVM مستقل لـ DeepSeekHashV1 Proof-of-Work.
 * لا يحتاج Android Emulator — يعمل على أي JVM.
 *
 * المدخلات والمخرجات متوقعة ومحددة بدقة للتحقق من صحة الخوارزمية.
 */
class PowSolverTest {

    private val solver = PowSolver()

    @Test
    fun solveShouldFindNonceBelowDifficultyTarget() {
        val challenge = PowSolver.Challenge(
            challenge = "test-challenge-123",
            salt = "test-salt",
            expireAt = System.currentTimeMillis() + 3600_000,
            difficulty = 8, // 1 byte zero = fast test target (~256 iterations)
            signature = "test-signature",
            targetPath = "/api/v0/chat/completion"
        )

        val solution = solver.solve(challenge)

        // يجب أن يكون الحل غير سالب
        assertTrue("nonce يجب أن يكون غير سالب", solution.answer >= 0L)
        assertTrue("headerValue يجب ألا يكون فارغاً", solution.headerValue.isNotEmpty())

        // التحقق من أن الحل يحقق الهدف فعلياً (إعادة الحساب)
        val prefix = "${challenge.salt}_${challenge.expireAt}_"
        val input = (challenge.challenge + "|" + prefix + solution.answer).toByteArray(kotlin.text.Charsets.UTF_8)
        val hash = solver.sha3_256(input)

        val target = solver.difficultyTarget(challenge.difficulty)
        assertTrue("الحل يجب أن يحقق الهدف (hash < target)", solver.hashMeetsTarget(hash, target))
    }

    @Test
    fun difficultyTargetShouldProduceValidBytes() {
        val target0 = solver.difficultyTarget(0)
        assertEquals(32, target0.size)

        // difficulty 0 = كل البايتات 0xFF (أصعب هدف، أي هاش ينجح)
        assertTrue(target0.all { it == 0xFF.toByte() })

        val target256 = solver.difficultyTarget(254)
        assertEquals(32, target256.size)

        // 254 بايت = أول 31 بايت 0x00، البايت الأخير 부분اً
        for (i in 0 until 31) {
            assertEquals("البت $i يجب أن يكون صفر في الهدف 254", 0x00, target256[i].toInt())
        }
    }

    @Test
    fun hashMeetsTargetShouldWorkCorrectly() {
        val solver = PowSolver()
        val target = solver.difficultyTarget(8) // أول بايت يجب أن يكون 0x00

        // هاش يبدأ بـ 0x00 يجب أن ينجح
        val goodHash = ByteArray(32) { 0x00.toByte() }
        assertTrue(solver.hashMeetsTarget(goodHash, target))

        // هاش يبدأ بـ 0xFF يجب أن يفشل (لأن target الأول هو 0x00)
        val badHash = ByteArray(32) { 0xFF.toByte() }
        assertTrue(!solver.hashMeetsTarget(badHash, target))
    }

    @Test
    fun solveWithHighDifficultyShouldStillComplete() {
        val challenge = PowSolver.Challenge(
            challenge = "high-difficulty-test",
            salt = "salt",
            expireAt = System.currentTimeMillis() + 3600_000,
            difficulty = 16, // هدف صعب أكثر
            signature = "sig",
            targetPath = "/api/v0/chat/completion"
        )

        val start = System.currentTimeMillis()
        val solution = solver.solve(challenge)
        val elapsed = System.currentTimeMillis() - start

        assertTrue("يجب إيجاد حل خلال 30 ثانية", elapsed < 30_000)
        assertTrue("الحل يجب أن يحقق الهدف", solver.hashMeetsTarget(
            solver.sha3_256(
                (challenge.challenge + "|" + challenge.salt + "_" + challenge.expireAt + "_" + solution.answer)
                    .toByteArray(kotlin.text.Charsets.UTF_8)
            ),
            solver.difficultyTarget(challenge.difficulty)
        ))
    }
}
