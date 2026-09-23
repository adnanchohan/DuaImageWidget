package com.watchfulai.mywidgets.tasbeeh

import android.content.Context
import android.view.ContextThemeWrapper
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.test.platform.app.InstrumentationRegistry
import com.watchfulai.mywidgets.R
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TasbeehWidgetTest {
    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun concurrentTapsPersistAndKeepWidgetsIndependent() {
        val repository = TasbeehRepository(context)
        val first = -901
        val second = -902
        try {
            repository.update(first) { TasbeehConfig(target = 100) }
            repository.update(second) { TasbeehConfig(count = 7) }
            val executor = Executors.newFixedThreadPool(4)
            repeat(40) { executor.execute { TasbeehRepository(context).update(first) { it!!.increment() } } }
            executor.shutdown()
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS))
            assertEquals(40, TasbeehRepository(context).get(first)?.count)
            assertEquals(7, repository.get(second)?.count)
            repository.remove(first)
            assertNull(repository.get(first))
            assertEquals(7, repository.get(second)?.count)
        } finally {
            repository.remove(first)
            repository.remove(second)
        }
    }

    @Test fun widgetRemoteViewsInflateWithRuntimeColorAndText() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val themed = ContextThemeWrapper(context, android.R.style.Theme_Material_Light)
            val views = RemoteViews(context.packageName, R.layout.tasbeeh_widget).apply {
                setInt(R.id.tasbeeh_background, "setColorFilter", TasbeehTheme.BLUE.background.toInt())
                setTextViewText(R.id.tasbeeh_count, "40 / 100")
            }
            val inflated = views.apply(themed, FrameLayout(themed))
            assertEquals("40 / 100", inflated.findViewById<android.widget.TextView>(R.id.tasbeeh_count).text.toString())
        }
    }
}
