package com.pi.erp.product.category;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {
    @Autowired
    private CategoryRepository repository;

    // Verifica se o usuário inseriu algum nome e procura por categorias com esse nome
    // Se o usuário não tiver inserido nome, lista todas as categorias existentes
    public List<Category> search(String name) {
        if (name == null || name.isBlank()) {
            return repository.findAll();
        }
        return repository.findByNameContainingIgnoreCase(name);
    }

    // Verifica se a categoria a ser cadastrada já existe
    public Category register(RequestCategoryDTO data) {

        if (repository.existsByNameIgnoreCase(data.name())) {
            throw new IllegalArgumentException("This category already exists.");
        }
        Category category = new Category(data);
        return repository.save(category);
    }

}
