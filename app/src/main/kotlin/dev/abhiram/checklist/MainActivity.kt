package dev.abhiram.checklist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhiram.checklist.ui.ChecklistViewModel
import dev.abhiram.checklist.ui.ListsScreen
import dev.abhiram.checklist.ui.theme.ChecklistTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ChecklistViewModel by viewModels {
        val app = application as ChecklistApplication
        ChecklistViewModel.factory(this, app.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChecklistTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    val selected = uiState.selectedChecklist
                    if (selected == null) {
                        ListsScreen(
                            checklists = uiState.checklists,
                            onOpenChecklist = viewModel::openChecklist,
                            onCreateChecklist = viewModel::createChecklist,
                            onRenameChecklist = viewModel::renameChecklist,
                            onDeleteChecklist = viewModel::deleteChecklist,
                            onSetPinned = viewModel::setPinned,
                        )
                    } else {
                        BackHandler { viewModel.closeChecklist() }
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = selected.title,
                                style = MaterialTheme.typography.titleLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}
