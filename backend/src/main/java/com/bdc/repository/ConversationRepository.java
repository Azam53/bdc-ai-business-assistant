package com.bdc.repository;
import com.bdc.entity.Conversation;import org.springframework.data.jpa.repository.JpaRepository;import java.util.List;
public interface ConversationRepository extends JpaRepository<Conversation,String>{List<Conversation> findAllByOrderByCreatedDesc();}
