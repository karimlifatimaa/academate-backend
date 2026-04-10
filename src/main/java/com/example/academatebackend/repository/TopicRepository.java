package com.example.academatebackend.repository;

import com.example.academatebackend.entity.Topic;
import com.example.academatebackend.enums.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TopicRepository extends JpaRepository<Topic, UUID> {

    List<Topic> findBySubjectAndGradeOrderByOrderIndex(Subject subject, Short grade);

    List<Topic> findBySubjectOrderByGradeAscOrderIndexAsc(Subject subject);
}
