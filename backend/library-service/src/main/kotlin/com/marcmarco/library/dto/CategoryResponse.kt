package com.marcmarco.library.dto

import com.marcmarco.library.LibraryCategory
import java.time.Instant

data class CategoryResponse(
    val id: Long,
    val name: String,
    val createdAt: Instant,
    val gameCount: Long = 0,
) {
    companion object {
        fun from(category: LibraryCategory, gameCount: Long = 0): CategoryResponse = CategoryResponse(
            id = requireNotNull(category.id),
            name = category.name,
            createdAt = category.createdAt,
            gameCount = gameCount,
        )
    }
}

data class CategorySummary(
    val id: Long,
    val name: String,
) {
    companion object {
        fun from(category: LibraryCategory): CategorySummary = CategorySummary(
            id = requireNotNull(category.id),
            name = category.name,
        )
    }
}