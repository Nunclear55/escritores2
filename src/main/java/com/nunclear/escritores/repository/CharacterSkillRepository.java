package com.nunclear.escritores.repository;

import com.nunclear.escritores.entity.CharacterSkill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CharacterSkillRepository extends JpaRepository<CharacterSkill, Integer> {
    Page<CharacterSkill> findByStoryCharacterId(Integer storyCharacterId, Pageable pageable);
    Page<CharacterSkill> findBySkillId(Integer skillId, Pageable pageable);
    boolean existsByStoryCharacterIdAndSkillId(Integer storyCharacterId, Integer skillId);
}