package com.zzstack.paas.underlying.dao.bean;

public class Acc {
    
    private int acc_id;
    private String acc_name;
    
    public Acc(int id, String name) {
        super();
        this.acc_id = id;
        this.acc_name = name;
    }

    public int getId() {
        return acc_id;
    }

    public void setId(int id) {
        this.acc_id = id;
    }

    public String getName() {
        return acc_name;
    }

    public void setName(String name) {
        this.acc_name = name;
    }

}
