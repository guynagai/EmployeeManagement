package com.example.app.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "part_time_employees")
public class PartTimeEmployee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "name_kanji")
    private String nameKanji;
    @Column(name = "name_hiragana")
    private String nameHiragana;
    private Integer age;
    @Column(name = "birthdate")
    private LocalDate birthdate;
    private String gender;
    private String email;
    private String phone;
    @Enumerated(EnumType.STRING)
    @Column(name = "skill_level")
    private SkillLevel skillLevel;
    public enum SkillLevel {
        LEADER, GENERAL, NEWCOMER
    }

    // --- Getter & Setter ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNameKanji() { return nameKanji; }
    public void setNameKanji(String nameKanji) { this.nameKanji = nameKanji; }

    public String getNameHiragana() { return nameHiragana; }
    public void setNameHiragana(String nameHiragana) { this.nameHiragana = nameHiragana; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public LocalDate getBirthdate() { return birthdate; }
    public void setBirthdate(LocalDate birthdate) { this.birthdate = birthdate; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public SkillLevel getSkillLevel() { return skillLevel; }
    public void setSkillLevel(SkillLevel skillLevel) { this.skillLevel = skillLevel; }
    
}


