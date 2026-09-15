package com.marcmarco.library

import com.marcmarco.library.dto.CategoryRequest
import com.marcmarco.shared.error.ConflictException
import com.marcmarco.shared.error.NotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify

class LibraryCategoryServiceTest {

    private val categoryRepository: LibraryCategoryRepository = mock(LibraryCategoryRepository::class.java)
    private val libraryRepository: LibraryRepository = mock(LibraryRepository::class.java)
    private val service = LibraryCategoryService(categoryRepository, libraryRepository)

    private fun category(id: Long, name: String) = LibraryCategory(id = id, userId = 42L, name = name)

    private fun libraryEntry(): LibraryEntry = LibraryEntry(userId = 42L, game = Game(id = 1L))

    private data class CategoryCount(override val categoryId: Long, override val gameCount: Long) :
        LibraryCategoryRepository.CategoryGameCount

    // ===== Crear =====

    @Test
    fun `create con nombre duplicado case-insensitive lanza Conflict`() {
        given(categoryRepository.existsByUserIdAndNameIgnoreCase(42L, "Terror")).willReturn(true)

        val error = assertThrows<ConflictException> { service.create(42L, CategoryRequest(name = "Terror")) }

        assertTrue(error.message.orEmpty().contains("Terror"))
    }

    @Test
    fun `create recorta el nombre, guarda y devuelve la categoria`() {
        val saved = category(7L, "Terror")
        given(categoryRepository.existsByUserIdAndNameIgnoreCase(42L, "Terror")).willReturn(false)
        given(categoryRepository.save(any(LibraryCategory::class.java))).willReturn(saved)

        val response = service.create(42L, CategoryRequest(name = "  Terror  "))

        assertEquals(7L, response.id)
        assertEquals("Terror", response.name)
        assertEquals(0L, response.gameCount)
        verify(categoryRepository).save(any(LibraryCategory::class.java))
    }

    // ===== Listar =====

    @Test
    fun `list devuelve las categorias del usuario con su gameCount`() {
        val catA = category(1L, "Accion")
        val catB = category(2L, "Rol")
        given(categoryRepository.findAllByUserIdOrderByName(42L)).willReturn(listOf(catA, catB))
        given(categoryRepository.countGamesByCategory(42L)).willReturn(listOf(CategoryCount(1, 3), CategoryCount(2, 0)))

        val response = service.list(42L)

        assertEquals(listOf(1L, 2L), response.map { it.id })
        assertEquals(listOf(3L, 0L), response.map { it.gameCount })
    }

    // ===== Renombrar =====

    @Test
    fun `rename a nombre ya usado por otra categoria lanza Conflict`() {
        val cat = category(1L, "Antes")
        given(categoryRepository.findByUserIdAndId(42L, 1L)).willReturn(cat)
        given(categoryRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(42L, "Despues", 1L)).willReturn(true)

        assertThrows<ConflictException> { service.rename(42L, 1L, CategoryRequest(name = "Despues")) }
    }

    @Test
    fun `rename de categoria inexistente o ajena lanza NotFound`() {
        given(categoryRepository.findByUserIdAndId(42L, 99L)).willReturn(null)

        assertThrows<NotFoundException> { service.rename(42L, 99L, CategoryRequest(name = "X")) }
    }

    @Test
    fun `rename actualiza el nombre de la categoria`() {
        val cat = category(1L, "Antes")
        given(categoryRepository.findByUserIdAndId(42L, 1L)).willReturn(cat)
        given(categoryRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(42L, "Despues", 1L)).willReturn(false)
        given(categoryRepository.save(cat)).willReturn(cat)

        val response = service.rename(42L, 1L, CategoryRequest(name = "Despues"))

        assertEquals("Despues", response.name)
        assertEquals("Despues", cat.name)
        verify(categoryRepository).save(cat)
    }

    // ===== Borrar =====

    @Test
    fun `delete de categoria inexistente o ajena lanza NotFound`() {
        given(categoryRepository.findByUserIdAndId(42L, 5L)).willReturn(null)

        assertThrows<NotFoundException> { service.delete(42L, 5L) }

        verify(categoryRepository, never()).delete(any(LibraryCategory::class.java))
    }

    @Test
    fun `delete borra la categoria del usuario`() {
        val cat = category(5L, "MUERTA")
        given(categoryRepository.findByUserIdAndId(42L, 5L)).willReturn(cat)

        service.delete(42L, 5L)

        verify(categoryRepository).delete(cat)
    }

    // ===== Asignar / desasignar =====

    @Test
    fun `assign a un juego que no esta en mi biblioteca lanza NotFound`() {
        given(libraryRepository.findByUserIdAndGameId(42L, 1L)).willReturn(null)

        assertThrows<NotFoundException> { service.assignToLibraryEntry(42L, 1L, 7L) }
    }

    @Test
    fun `assign con categoria ajena lanza NotFound`() {
        val entry = libraryEntry()
        given(libraryRepository.findByUserIdAndGameId(42L, 1L)).willReturn(entry)
        given(categoryRepository.findByUserIdAndId(42L, 7L)).willReturn(null)

        assertThrows<NotFoundException> { service.assignToLibraryEntry(42L, 1L, 7L) }
    }

    @Test
    fun `assign anade la categoria a la entrada y guarda`() {
        val entry = libraryEntry()
        val cat = category(7L, "Favoritas")
        given(libraryRepository.findByUserIdAndGameId(42L, 1L)).willReturn(entry)
        given(categoryRepository.findByUserIdAndId(42L, 7L)).willReturn(cat)
        given(libraryRepository.save(entry)).willReturn(entry)

        val result = service.assignToLibraryEntry(42L, 1L, 7L)

        assertTrue(result.categories.contains(cat))
        verify(libraryRepository).save(entry)
    }

    @Test
    fun `unassign de una categoria no asignada lanza NotFound`() {
        val entry = libraryEntry()
        val cat = category(7L, "NoAsignada")
        given(libraryRepository.findByUserIdAndGameId(42L, 1L)).willReturn(entry)
        given(categoryRepository.findByUserIdAndId(42L, 7L)).willReturn(cat)

        assertThrows<NotFoundException> { service.unassignFromLibraryEntry(42L, 1L, 7L) }
    }

    @Test
    fun `unassign quita la categoria de la entrada y guarda`() {
        val cat = category(7L, "Temporal")
        val entry = LibraryEntry(userId = 42L, game = Game(id = 1L), categories = mutableSetOf(cat))
        given(libraryRepository.findByUserIdAndGameId(42L, 1L)).willReturn(entry)
        given(categoryRepository.findByUserIdAndId(42L, 7L)).willReturn(cat)
        given(libraryRepository.save(entry)).willReturn(entry)

        val result = service.unassignFromLibraryEntry(42L, 1L, 7L)

        assertFalse(result.categories.contains(cat))
        verify(libraryRepository).save(entry)
    }
}