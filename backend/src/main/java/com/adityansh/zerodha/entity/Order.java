package com.adityansh.zerodha.entity;
import jakarta.persistence.*; import lombok.*; import java.time.LocalDateTime;
@Entity @Table(name="orders") @Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Order { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @ManyToOne(optional=false) private User user; @ManyToOne(optional=false) private Stock stock; @Enumerated(EnumType.STRING) private Type type; private int quantity; private double price; private LocalDateTime createdAt=LocalDateTime.now(); public enum Type { BUY,SELL } }
