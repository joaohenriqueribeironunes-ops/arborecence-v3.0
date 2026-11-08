package com.arborecence.feature.review

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arborecence.core.domain.repository.ReviewRepository
import com.arborecence.core.ui.components.GlassSurface
import com.arborecence.core.ui.components.PrimaryGlassButton
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReviewViewModel @Inject constructor(private val repo: ReviewRepository) : ViewModel() {
    val due = repo.dueItems().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun grade(id: String, q: Int) = viewModelScope.launch { repo.grade(id, q) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewRoute(onBack: () -> Unit, vm: ReviewViewModel = hiltViewModel()) {
    val due by vm.due.collectAsState()
    var showAnswer by remember { mutableStateOf(setOf<String>()) }
    Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent, topBar = { TopAppBar(title = { Text("Revisão espaçada") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)) }) { pad ->
        LazyColumn(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (due.isEmpty()) item { GlassSurface { Text("Nada para revisar agora. As perguntas geradas a partir das suas notas/fichas aparecem aqui quando chegar a hora.", Modifier.padding(16.dp)) } }
            items(due, key = { it.id }) { item ->
                GlassSurface { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.question, style = MaterialTheme.typography.titleMedium)
                    if (item.id in showAnswer) {
                        Text(item.answer, style = MaterialTheme.typography.bodyLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { vm.grade(item.id, 2) }) { Text("Rever") }
                            OutlinedButton(onClick = { vm.grade(item.id, 4) }) { Text("Lembrei") }
                            Button(onClick = { vm.grade(item.id, 5) }) { Text("Fácil") }
                        }
                    } else {
                        PrimaryGlassButton("Mostrar resposta", onClick = { showAnswer = showAnswer + item.id })
                    }
                } }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
