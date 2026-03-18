package com.pharmasys.repository;

import com.pharmasys.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RolRepository extends JpaRepository<Rol, Long> {

    boolean existsByNombre(String nombre);

    Optional<Rol> findByNombre(String nombre);

    List<Rol> findAllByOrderByNombreAsc();
}
