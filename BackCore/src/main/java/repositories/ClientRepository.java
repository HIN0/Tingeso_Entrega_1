package repositories;

// Entidad Cliente y Status
import entities.ClientEntity;       
import entities.enums.ClientStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface ClientRepository extends JpaRepository<ClientEntity, Long> {
    Optional<ClientEntity> findByRut(String rut);
    List<ClientEntity> findByStatus(ClientStatus status);
}
