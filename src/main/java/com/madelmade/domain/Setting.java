package com.madelmade.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "setting")
public class Setting {
    @Id
    private String key;
    private String value;

    protected Setting() {}

    public Setting(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() { return key; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
