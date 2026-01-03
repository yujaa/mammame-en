import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.sampleFoodList

@Composable
fun App() {
    MaterialTheme {
        var query by remember { mutableStateOf("") }

        val filteredList = sampleFoodList.filter {
            it.name.contains(query, ignoreCase = true)
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text("맘먀미: 임산부 음식 안심 검색", style = MaterialTheme.typography.h6)

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("음식 이름을 입력하세요") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            LazyColumn {
                items(filteredList) { food ->
                    Card(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("🍽️ ${food.name}", style = MaterialTheme.typography.h6)
                            Text("안전 점수: ${food.score}점")
                            Text("총 ${food.opinions.size}명 중 ${food.opinions.count { it.opinion }}명 찬성")
                        }
                    }
                }
            }
        }
    }
}
