package com.adityansh.zerodha.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Stock { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(nullable=false,unique=true) private String symbol; private String company; private double price; private double changePercent; private String sector; }
