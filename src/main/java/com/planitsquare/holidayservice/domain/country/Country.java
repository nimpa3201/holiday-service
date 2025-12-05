package com.planitsquare.holidayservice.domain.country;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Country {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false,length = 10)
    private String code;

    @Column(nullable = false,length = 100)
    private String name;

    @Column(nullable = false)
    private boolean used = true;

    public Country(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public void updateCountry(String code,String name){
        this.code = code;
        this.name = name;
    }

    public void markSupported(boolean used) {
        this.used= used;
    }
}
