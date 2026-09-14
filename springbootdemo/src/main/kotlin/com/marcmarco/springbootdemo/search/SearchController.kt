package com.marcmarco.springbootdemo.search

import com.marcmarco.springbootdemo.search.dto.SearchResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/search")
class SearchController(private val searchService: SearchService) {

    @GetMapping
    fun search(@RequestParam q: String): SearchResponse = searchService.globalSearch(q)

    @GetMapping("/suggestions")
    fun suggestions(
        @RequestParam q: String,
        @RequestParam(required = false, defaultValue = "5") limit: Int,
    ): List<String> = searchService.suggestions(q, limit)
}