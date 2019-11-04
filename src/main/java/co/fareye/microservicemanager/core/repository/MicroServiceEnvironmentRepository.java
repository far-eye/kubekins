package co.fareye.microservicemanager.core.repository;

import co.fareye.microservicemanager.core.domain.MicroServiceEnvironment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface MicroServiceEnvironmentRepository extends JpaRepository<MicroServiceEnvironment,Long> {

    @Query("select ie from MicroServiceEnvironment ie where ie.microService.id = ?1")
    List<MicroServiceEnvironment> getMicroServiceEnvironmentByMicroServiceId(Long microServiceId);

    MicroServiceEnvironment getByCode(String code);


    @Query("select ie from MicroServiceEnvironment ie where (lower(ie.name) like ?1 or ie.code like ?1) and ie.microService.id = ?2")
    Page<MicroServiceEnvironment> getListWithQuery(String query, Long microServiceId, Pageable request);

    @Query("select ie from MicroServiceEnvironment ie where ie.microService.id = ?1")
    Page<MicroServiceEnvironment> getList(Long microServiceId, Pageable request);

    @Query("select ie from MicroServiceEnvironment ie where (lower(ie.name) like ?1 or ie.code like ?1) and ie.microService.id = ?2 and ie.id in ?3")
    Page<MicroServiceEnvironment> getListWithQueryAndId(String query, Long microServiceId, List<Long> microServiceIdList , Pageable request);

    @Query("select ie from MicroServiceEnvironment ie where ie.microService.id = ?1 and ie.id in ?2")
    Page<MicroServiceEnvironment> getListWithId(Long microServiceId, List<Long> microServiceIdList, Pageable request);

    @Query("select ie from MicroServiceEnvironment ie")
    List<MicroServiceEnvironment> getAllMicroServiceEnvironments();

    @Query("select ie from MicroServiceEnvironment ie where ie.id in ?1")
    List<MicroServiceEnvironment> getMicroServiceEnvironmentsByIdList(List<Long> microServiceIdList);

    @Query("select count(ie) from MicroServiceEnvironment ie WHERE ie.clusterid=?1 and ie.status in ('success','pending','processing')")
    Long countByClusterId(Long id);
}


