package com.marcmarco.library

import com.marcmarco.library.dto.CategoryRequest
import com.marcmarco.library.dto.CategoryResponse
import com.marcmarco.shared.error.ConflictException
import com.marcmarco.shared.error.NotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Categorías personales de un usuario para organizar su propia biblioteca.
 * Siempre filtradas por `userId`: un usuario solo puede ver, crear, renombrar,
 * borrar y asignar categorías de las que es dueño, y solo a entradas de su
 * biblioteca. No existen categorías globales/sistema.
 */
@Service
class LibraryCategoryService(
    private val categoryRepository: LibraryCategoryRepository,
    private val libraryRepository: LibraryRepository,
) {

    @Transactional
    fun create(userId: Long, request: CategoryRequest): CategoryResponse {
        val name = request.name.trim()
        if (categoryRepository.existsByUserIdAndNameIgnoreCase(userId, name)) {
            throw ConflictException("Ya tienes una categoría llamada '$name'")
        }
        val saved = categoryRepository.save(LibraryCategory(userId = userId, name = name))
        return CategoryResponse.from(saved)
    }

    @Transactional(readOnly = true)
    fun list(userId: Long): List<CategoryResponse> {
        val counts = categoryRepository.countGamesByCategory(userId)
            .associate { it.categoryId to it.gameCount }
        return categoryRepository.findAllByUserIdOrderByName(userId)
            .map { CategoryResponse.from(it, counts[it.id] ?: 0L) }
    }

    @Transactional
    fun rename(userId: Long, categoryId: Long, request: CategoryRequest): CategoryResponse {
        val category = ownedCategory(userId, categoryId)
        val name = request.name.trim()
        if (categoryRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(userId, name, categoryId)) {
            throw ConflictException("Ya tienes una categoría llamada '$name'")
        }
        category.name = name
        return CategoryResponse.from(categoryRepository.save(category))
    }

    @Transactional
    fun delete(userId: Long, categoryId: Long) {
        categoryRepository.delete(ownedCategory(userId, categoryId))
    }

    @Transactional
    fun assignToLibraryEntry(userId: Long, gameId: Long, categoryId: Long): LibraryEntry {
        val entry = ownedEntry(userId, gameId)
        val category = ownedCategory(userId, categoryId)
        entry.categories.add(category)
        return libraryRepository.save(entry)
    }

    @Transactional
    fun unassignFromLibraryEntry(userId: Long, gameId: Long, categoryId: Long): LibraryEntry {
        val entry = ownedEntry(userId, gameId)
        val category = ownedCategory(userId, categoryId)
        val removed = entry.categories.removeIf { it.id == category.id }
        if (!removed) {
            throw NotFoundException("La categoría '${category.name}' no está asignada a ese juego")
        }
        return libraryRepository.save(entry)
    }

    private fun ownedEntry(userId: Long, gameId: Long): LibraryEntry =
        libraryRepository.findByUserIdAndGameId(userId, gameId)
            ?: throw NotFoundException("El usuario $userId no tiene el juego $gameId en su biblioteca")

    private fun ownedCategory(userId: Long, categoryId: Long): LibraryCategory =
        categoryRepository.findByUserIdAndId(userId, categoryId)
            ?: throw NotFoundException("Categoría con id $categoryId no encontrada")
}