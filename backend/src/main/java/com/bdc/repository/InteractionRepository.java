package com.bdc.repository;
import com.bdc.entity.Interaction;import org.springframework.data.jpa.repository.JpaRepository;import java.util.List;
public interface InteractionRepository extends JpaRepository<Interaction,Long>{List<Interaction> findByConversationIdOrderByIdAsc(String id);List<Interaction> findTop200ByOrderByIdDesc();}
