package com.bdc.entity;
import jakarta.persistence.*;import java.time.Instant;
@Entity public class Interaction {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public String conversationId;public String userName="demo-user";@Column(length=2000) public String question;public String intent;public String functionName;public Instant timestamp=Instant.now();public String dataSource;public String status;
 @Lob @Column(columnDefinition="LONGTEXT") public String response;
}
