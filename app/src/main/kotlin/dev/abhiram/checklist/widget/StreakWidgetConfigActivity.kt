@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.abhiram.checklist.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhiram.checklist.ChecklistApplication
import dev.abhiram.checklist.MainActivity
import dev.abhiram.checklist.R
import dev.abhiram.checklist.data.model.StreakTracker
import dev.abhiram.checklist.ui.theme.ChecklistTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class StreakWidgetConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val glanceId = GlanceAppWidgetManager(this).getGlanceIdBy(intent)
        if (glanceId == null) {
            finish()
            return
        }

        val repository = (application as ChecklistApplication).repository
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            ChecklistTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val state by repository.state.collectAsStateWithLifecycle()
                    val scope = rememberCoroutineScope()
                    StreakWidgetConfigScreen(
                        trackers = state.streakTrackers,
                        onSelect = { trackerId ->
                            scope.launch {
                                try {
                                    val bound = bindSelectedStreakTracker(
                                        trackerId = trackerId,
                                        trackers = state.streakTrackers,
                                        setBinding = { id ->
                                            repository.setStreakWidgetBinding(appWidgetId, id)
                                        },
                                        refresh = {
                                            StreakWidget().update(
                                                this@StreakWidgetConfigActivity,
                                                glanceId,
                                            )
                                        },
                                    )
                                    if (bound) {
                                        setResult(
                                            RESULT_OK,
                                            Intent().putExtra(
                                                AppWidgetManager.EXTRA_APPWIDGET_ID,
                                                appWidgetId,
                                            ),
                                        )
                                        finish()
                                    }
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (error: Exception) {
                                    Log.e("StreakWidgetConfig", "Failed to bind streak widget", error)
                                    finish()
                                }
                            }
                        },
                        onOpenApp = {
                            startActivity(
                                Intent(
                                    this@StreakWidgetConfigActivity,
                                    MainActivity::class.java,
                                ),
                            )
                            finish()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun StreakWidgetConfigScreen(
    trackers: List<StreakTracker>,
    onSelect: (String) -> Unit,
    onOpenApp: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.streak_widget_config_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        if (trackers.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.streak_widget_config_empty_title),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.streak_widget_config_empty_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = onOpenApp) {
                    Text(stringResource(R.string.widget_config_open_app))
                }
            }
        } else {
            StreakWidgetPicker(
                trackers = trackers,
                onSelect = onSelect,
                modifier = Modifier.padding(padding),
            )
        }
    }
}
