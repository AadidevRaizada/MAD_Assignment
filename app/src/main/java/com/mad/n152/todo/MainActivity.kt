package com.mad.n152.todo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import com.google.firebase.database.database
import com.google.firebase.Firebase
import com.mad.n152.todo.ui.TodoRoute
import com.mad.n152.todo.ui.TodoScreen
import com.mad.n152.todo.ui.TodoUiState
import com.mad.n152.todo.ui.theme.N152Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep the last-known tasks on disk so the list renders instantly and edits made while
        // offline are replayed once the device reconnects. Must be set once, before any other
        // database use, or Firebase throws.
        runCatching { Firebase.database.setPersistenceEnabled(true) }

        enableEdgeToEdge()
        setContent {
            N152Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TodoRoute()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainActivityPreview() {
    N152Theme(dynamicColor = false) {
        TodoScreen(
            uiState = TodoUiState(isLoading = false),
            onAdd = { _, _ -> },
            onUpdate = { _, _, _ -> },
            onDelete = {}
        )
    }
}
