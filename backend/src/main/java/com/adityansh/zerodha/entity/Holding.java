package com.adityansh.zerodha.entity;
import jakarta.persistence.*; import lombok.*;
@Entity @Table(uniqueConstraints=@UniqueConstraint(columnNames={"user_id","stock_id"})) @Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Holding { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @ManyToOne(optional=false) private User user; @ManyToOne(optional=false) private Stock stock; private int quantity; private double averagePrice; }
