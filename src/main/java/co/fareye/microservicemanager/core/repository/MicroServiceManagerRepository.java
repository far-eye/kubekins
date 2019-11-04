package co.fareye.microservicemanager.core.repository;

import co.fareye.microservicemanager.core.domain.MicroService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;

@Repository
    public interface MicroServiceManagerRepository extends JpaRepository<MicroService,Long> {

    MicroService getByCode(String code);

    MicroService getById(Long microServiceId);

    @Modifying
    @Transactional
    @Query("update  MicroService conn set conn.numberOfEnvironments = conn.numberOfEnvironments+1 where conn.code = ?1")
    Integer increaseEnvironmentCount(String code);

    @Query(value = "select conn from MicroService conn where conn.id in ?1")
    List<MicroService> findByIdList(List<Long> environmentIdList);
}
