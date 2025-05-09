package com.example.app.entity;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "part_time_employees")
public class PartTimeEmployee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String nameKanji;
    private String nameHiragana;
    private Integer age;
    private LocalDate birthdate;
    private String gender;
    private String email;
    private String phone;

    // --- Getter & Setter ---
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

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
}


