package co.fareye.microservicemanager.clusterManager.repository;

import co.fareye.microservicemanager.clusterManager.domain.Cluster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;

@Repository
public interface ClusterManagerRepository extends JpaRepository<Cluster, Long> {
    @Query(value = "select clstr from Cluster clstr")
    List<Cluster> getClusterList();

    @Query(value = "select clstr from Cluster clstr where clstr.clustername = ?1")
    Cluster getByClusterName(String clusterName);

    @Query(value = "select clstr from Cluster clstr where clstr.id = ?1")
    Cluster getByClusterId(Long clusterId);

    @Transactional
    @Modifying
    @Query(value = "update Cluster clstr set clstr.projectid=?1, clstr.clustername=?2, clstr.region=?3, clstr.clusterDescription=?4 where clstr.id = ?5")
    Integer updateCluster(String projectId, String clusterName, String region, String clusterDescription, long id);

    @Transactional
    @Modifying
    @Query(value = "delete from Cluster clstr where clstr.id=?1")
    Integer deleteCluster(Long id);

}
