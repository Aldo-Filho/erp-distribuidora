package com.pi.erp.user;

import com.pi.erp.exception.ResponseErrorDTO;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserRepository repository;

    @Autowired
    private UserService service;

    @GetMapping("/search")
    public ResponseEntity<List<User>> search(UserFilter filter) {
        return ResponseEntity.ok(service.search(filter));
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> findOne(@PathVariable Long id) {
        return repository
            .findById(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping
    public ResponseEntity<ResponseErrorDTO> register() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(new ResponseErrorDTO(
            405, "Method Not Allowed", "Create users through invitations.", LocalDateTime.now()
        ));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<User> update(
        @PathVariable Long id,
        @RequestBody @Valid PatchUserDTO data
    ) {
        User user = service.update(id, data);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
