package com.mariafonseca.financeanalytics

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mariafonseca.financeanalytics.core.designsystem.FinanceAnalyticsTheme
import com.mariafonseca.financeanalytics.core.navigation.FinanceAnalyticsNavHost

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FinanceAnalyticsTheme {
                FinanceAnalyticsNavHost()
            }
        }
    }
}
