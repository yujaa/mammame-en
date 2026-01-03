package data

data class ExpertOpinion(
    val name: String,
    val opinion: Boolean, // true = 먹어도 된다
    val source: String
)

data class FoodSafetyInfo(
    val name: String,
    val opinions: List<ExpertOpinion>
) {
    val score: Int
        get() = if (opinions.isEmpty()) 0 else (opinions.count { it.opinion } * 100) / opinions.size

    val approvedCount: Int
        get() = opinions.count { it.opinion }

    val totalCount: Int
        get() = opinions.size
}
