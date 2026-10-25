package dev.abhiram.checklist

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dev.abhiram.checklist.ui.ChecklistApp
import dev.abhiram.checklist.ui.ChecklistViewModel
import dev.abhiram.checklist.ui.theme.ChecklistTheme
import dev.abhiram.checklist.widget.ChecklistWidget

class MainActivity : ComponentActivity() {

    private val viewModel: ChecklistViewModel by viewModels {
        val app = application as ChecklistApplication
        ChecklistViewModel.factory(this, app.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        openFromIntent(intent)
        setContent {
            ChecklistTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    ChecklistApp(viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openFromIntent(intent)
    }

    private fun openFromIntent(intent: Intent?) {
        intent?.getStringExtra(ChecklistWidget.EXTRA_OPEN_CHECKLIST)
            ?.let(viewModel::openChecklist)
    }
}
