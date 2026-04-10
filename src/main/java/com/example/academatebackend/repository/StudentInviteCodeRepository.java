package com.example.academatebackend.repository;

import com.example.academatebackend.entity.StudentInviteCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentInviteCodeRepository extends JpaRepository<StudentInviteCode, UUID> {

    Optional<StudentInviteCode> findByCode(String code);
}
