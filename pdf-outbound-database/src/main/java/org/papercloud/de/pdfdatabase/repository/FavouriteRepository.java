package org.papercloud.de.pdfdatabase.repository;

import org.papercloud.de.pdfdatabase.entity.UserDocumentFavouriteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface FavouriteRepository extends JpaRepository<UserDocumentFavouriteEntity, Long> {

    @Query("SELECT f FROM UserDocumentFavouriteEntity f " +
            "JOIN FETCH f.document d " +
            "LEFT JOIN FETCH d.pages " +
            "WHERE f.user.username = :username")
    List<UserDocumentFavouriteEntity> findByUserUsernameWithDocument(@Param("username") String username);

    boolean existsByUserIdAndDocumentId(Long userId, Long documentId);

    void deleteByUserIdAndDocumentId(Long userId, Long documentId);

    @Query("SELECT f.document.id FROM UserDocumentFavouriteEntity f WHERE f.user.username = :username")
    Set<Long> findFavouriteDocumentIdsByUsername(@Param("username") String username);
}
