package data

fun filterFood(query: String, list: List<FoodSafetyInfo>): List<FoodSafetyInfo> {
    return if (query.isBlank()) list
    else list.filter { it.name.contains(query, ignoreCase = true) }
}
