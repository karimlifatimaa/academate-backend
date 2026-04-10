package com.example.academatebackend.repository;

import com.example.academatebackend.entity.ParentStudentLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ParentStudentLinkRepository extends JpaRepository<ParentStudentLink, ParentStudentLink.ParentStudentLinkId> {

    List<ParentStudentLink> findByIdParentId(UUID parentId);

    List<ParentStudentLink> findByIdStudentId(UUID studentId);

    boolean existsByIdParentIdAndIdStudentIdAndVerifiedTrue(UUID parentId, UUID studentId);
}
