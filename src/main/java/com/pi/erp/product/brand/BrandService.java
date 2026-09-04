package com.pi.erp.product.brand;

import com.pi.erp.warehouse.Warehouse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BrandService {
    @Autowired
    private BrandRepository repository;

    // Verifica se o usuário inseriu algum nome e procura por marcas com esse nome
    // Se o usuário não tiver inserido nome, lista todas as marcas existentes
    public List<Brand> search(String name) {
        if (name == null || name.isBlank()) {
            return repository.findAll();
        }
        return repository.findByNameContainingIgnoreCase(name);
    }

    // Verifica se a marca a ser cadastrada já existe
    public Brand register(RequestBrandDTO data) {
        if (repository.existsByNameIgnoreCase(data.name())) {
            throw new IllegalArgumentException("This brand already exists.");
        }
        Brand brand = new Brand(data);
        return repository.save(brand);
    }

    @Transactional
    public void delete(Long id) {
        Brand brand = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Brand not found."));

        repository.delete(brand);
    }

}
