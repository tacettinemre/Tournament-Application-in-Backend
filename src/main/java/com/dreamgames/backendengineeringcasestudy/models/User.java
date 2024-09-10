package com.dreamgames.backendengineeringcasestudy.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int coins = 5000;  // Default value

    @Column(nullable = false)
    private int level = 1;  // Default value

    @Column(nullable = false)
    private String country;

    @Column(nullable = false)
    private int score = 0;  // Default value

    @Column(nullable = false)
    private int hasReward = 0;

    // Many users can belong to one group
    @ManyToOne
    @JoinColumn(name = "group_id")
    private Group group;

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getCoins() {
        return coins;
    }

    public void setCoins(int coins) {
        this.coins = coins;
    }

    public int getHasReward() {
        return hasReward;
    }

    public void setHasReward(int hasReward) {
        this.hasReward = hasReward;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }
}
