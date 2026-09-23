package com.watchfulai.mywidgets.tasbeeh

import org.junit.Assert.assertEquals
import org.junit.Test

class TasbeehConfigTest {
    @Test fun countStopsAtTarget() {
        assertEquals(33, TasbeehConfig(count = 32).increment().increment().count)
    }
    @Test fun restartKeepsCompletedTargetVisibleUntilNextTap() {
        val complete = TasbeehConfig(count = 32, restartAtTarget = true).increment()
        assertEquals(33, complete.count)
        assertEquals(1, complete.increment().count)
    }
    @Test fun targetOfOneRestartsAtOne() {
        assertEquals(1, TasbeehConfig(target = 1, count = 1, restartAtTarget = true).increment().count)
    }
    @Test fun invalidPersistedValuesAreClamped() {
        assertEquals(1, TasbeehConfig(target = -4, count = 90).normalized().count)
        assertEquals(0, TasbeehConfig(count = -1).normalized().count)
        assertEquals(99999, TasbeehConfig(target = Int.MAX_VALUE).normalized().target)
    }
    @Test fun loweringTargetPreservesOnlyValidProgress() {
        assertEquals(10, TasbeehConfig(target = 10, count = 25).normalized().count)
    }
}
