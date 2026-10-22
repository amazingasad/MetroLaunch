package com.metroos.launcher.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metroos.launcher.ui.theme.MetroLime
import com.metroos.launcher.util.Prefs
import org.json.JSONArray
import org.json.JSONObject

data class Note(val id: String, val title: String, val body: String, val updated: Long)

/**
 * Notes app backed by Preferences DataStore-style persistence (a single
 * JSON document in app preferences, keeping the footprint light).
 */
@Composable
fun NotesApp() {
    val context = LocalContext.current
    var notes by remember { mutableStateOf<List<Note>>(emptyList()) }
    var editing by remember { mutableStateOf<Note?>(null) }

    LaunchedEffect(Unit) { notes = loadNotes(context) }

    val save: (List<Note>) -> Unit = { updated ->
        notes = updated
        persistNotes(context, updated)
    }

    editing?.let { note ->
        NoteEditor(
            initial = note,
            onSave = { saved ->
                save(if (note.id.isEmpty()) notes + saved else notes.map { if (it.id == saved.id) saved else it })
                editing = null
            },
            onDelete = {
                save(notes.filter { it.id != note.id })
                editing = null
            },
            onBack = { editing = null }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05070E))
            .padding(horizontal = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 18.dp)) {
            Text("Notes", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.Filled.Add,
                contentDescription = "New note",
                tint = Color.White,
                modifier = Modifier.clickable {
                    editing = Note(id = "", title = "", body = "", updated = System.currentTimeMillis())
                }
            )
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(notes, key = { it.id.ifEmpty { it.updated.toString() } }) { note ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable { editing = note }
                        .padding(14.dp)
                ) {
                    Text(
                        note.title.ifEmpty { "Untitled" },
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        note.body,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteEditor(
    initial: Note,
    onSave: (Note) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf(initial.title) }
    var body by remember { mutableStateOf(initial.body) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05070E))
            .imePadding()
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.clickable(onClick = onBack)
            )
            Spacer(Modifier.width(12.dp))
            Text("Note", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Delete",
                tint = Color(0xFFE51400),
                modifier = Modifier.clickable(onClick = onDelete)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                "Save",
                color = MetroLime,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable {
                    onSave(
                        Note(
                            id = initial.id.ifEmpty { "n${System.currentTimeMillis()}" },
                            title = title.trim(),
                            body = body.trim(),
                            updated = System.currentTimeMillis()
                        )
                    )
                }
            )
        }

        Spacer(Modifier.height(20.dp))
        BasicTextField(
            value = title,
            onValueChange = { title = it },
            textStyle = TextStyle(color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
            cursorBrush = SolidColor(Color.White),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box {
                    if (title.isEmpty()) Text("Title", color = Color.White.copy(alpha = 0.4f), fontSize = 22.sp)
                    inner()
                }
            }
        )
        Spacer(Modifier.height(14.dp))
        BasicTextField(
            value = body,
            onValueChange = { body = it },
            textStyle = TextStyle(color = Color.White.copy(alpha = 0.9f), fontSize = 15.sp),
            cursorBrush = SolidColor(Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            decorationBox = { inner ->
                Box {
                    if (body.isEmpty()) Text("Write something…", color = Color.White.copy(alpha = 0.4f), fontSize = 15.sp)
                    inner()
                }
            }
        )
    }
}

private const val NOTES_KEY = "notes_json"

private fun loadNotes(context: android.content.Context): List<Note> = runCatching {
    val raw = Prefs.getString(context, NOTES_KEY, "")
    if (raw.isBlank()) return@runCatching emptyList()
    val arr = JSONArray(raw)
    (0 until arr.length()).mapNotNull { i ->
        val o = arr.optJSONObject(i) ?: return@mapNotNull null
        Note(
            id = o.optString("id"),
            title = o.optString("title"),
            body = o.optString("body"),
            updated = o.optLong("updated")
        )
    }.sortedByDescending { it.updated }
}.getOrDefault(emptyList())

private fun persistNotes(context: android.content.Context, notes: List<Note>) {
    val arr = JSONArray()
    notes.forEach { n ->
        arr.put(
            JSONObject()
                .put("id", n.id)
                .put("title", n.title)
                .put("body", n.body)
                .put("updated", n.updated)
        )
    }
    Prefs.putString(context, NOTES_KEY, arr.toString())
}
