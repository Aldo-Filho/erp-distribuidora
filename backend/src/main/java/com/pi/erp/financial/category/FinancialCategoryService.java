package com.pi.erp.financial.category;
import com.pi.erp.auth.CurrentUser;
import com.pi.erp.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class FinancialCategoryService {
    @Autowired private FinancialCategoryRepository repository;
    @Autowired private jakarta.persistence.EntityManager em;
    @Transactional
    public FinancialCategory register(RequestFinancialCategoryDTO data) {
        CurrentUser.require("finance.configure");
        var all=repository.lockConfiguration(); all.forEach(em::refresh);
        FinancialCategory category=new FinancialCategory();
        category.setName(name(data.name())); category.setType(data.type());
        if(Boolean.TRUE.equals(data.defaultForDocuments())) {
            clearDefault(all,data.type()); category.setDefaultForDocuments(true);
        }
        return repository.save(category);
    }
    @Transactional
    public FinancialCategory update(Long id, PatchFinancialCategoryDTO data) {
        CurrentUser.require("finance.configure");
        var all=repository.lockConfiguration(); all.forEach(em::refresh);
        FinancialCategory category=all.stream().filter(c -> c.getId().equals(id)).findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Categoria financeira não encontrada."));
        boolean active=data.active()==null ? category.isActive() : data.active();
        boolean defaultValue=data.defaultForDocuments()==null ? category.isDefaultForDocuments() : data.defaultForDocuments();
        if(!active && defaultValue) throw new IllegalArgumentException("Troque a categoria padrão antes de inativá-la.");
        if(defaultValue && !category.isDefaultForDocuments()) clearDefault(all,category.getType());
        if(data.name()!=null) category.setName(name(data.name()));
        category.setDefaultForDocuments(defaultValue); category.setActive(active);
        return category;
    }
    private void clearDefault(java.util.List<FinancialCategory> all, CategoryType type) {
        all.stream().filter(c -> c.getType()==type).forEach(c -> c.setDefaultForDocuments(false));
        repository.flush(); // Release the partial unique index before installing the new default.
    }
    private String name(String name) {
        if(name==null || name.isBlank() || name.trim().length()>100) throw new IllegalArgumentException("Nome inválido.");
        return name.trim();
    }
}
