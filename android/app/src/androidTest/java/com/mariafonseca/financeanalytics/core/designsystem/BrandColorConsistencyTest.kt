package com.mariafonseca.financeanalytics.core.designsystem

import androidx.compose.ui.graphics.toArgb
import androidx.test.platform.app.InstrumentationRegistry
import com.mariafonseca.financeanalytics.R
import org.junit.Assert.assertEquals
import org.junit.Test

class BrandColorConsistencyTest {

    @Test
    fun brandPrimaryMatchesColorResource() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        assertEquals(context.getColor(R.color.brand_primary), BrandPrimary.toArgb())
    }
}
