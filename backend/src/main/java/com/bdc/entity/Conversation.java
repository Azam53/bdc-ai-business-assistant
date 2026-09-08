package com.bdc.entity;
import jakarta.persistence.*;import java.time.Instant;
@Entity public class Conversation {
 @Id public String id;public String title;public Instant created=Instant.now();@Lob @Column(columnDefinition="LONGTEXT") public String context;
 public Conversation(){}public Conversation(String id,String title){this.id=id;this.title=title;}
}
