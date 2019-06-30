package io.playtext.playtext.login;

import java.sql.Timestamp;

public class MyUser {

    private String display_name;
    private String email;
    private String photo_url;
    private String uid;
    private long creation_timestamp;
    private long last_signin_timestamp;
    private long delete_timestamp;
    private boolean pending_for_delete;

    private int earn_1;
    private int earn_2;
    private int earn_3;
    private int earn_4;
    private int earn_5;
    private int earn_6;
    private int earn_7;
    private int earn_8;
    private int earn_9;

    private int items_limit;

    public MyUser() {
    }

    public String getDisplay_name() {
        return display_name;
    }

    public void setDisplay_name(String display_name) {
        this.display_name = display_name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoto_url() {
        return photo_url;
    }

    public void setPhoto_url(String photo_url) {
        this.photo_url = photo_url;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public long getCreation_timestamp() {
        return creation_timestamp;
    }

    public void setCreation_timestamp(long creation_timestamp) {
        this.creation_timestamp = creation_timestamp;
    }

    public long getLast_signin_timestamp() {
        return last_signin_timestamp;
    }

    public void setLast_signin_timestamp(long last_signin_timestamp) {
        this.last_signin_timestamp = last_signin_timestamp;
    }

    public long getDelete_timestamp() {
        return delete_timestamp;
    }

    public void setDelete_timestamp(long delete_timestamp) {
        this.delete_timestamp = delete_timestamp;
    }

    public boolean isPending_for_delete() {
        return pending_for_delete;
    }

    public void setPending_for_delete(boolean pending_for_delete) {
        this.pending_for_delete = pending_for_delete;
    }

    public int getEarn_1() {
        return earn_1;
    }

    public void setEarn_1(int earn_1) {
        this.earn_1 = earn_1;
    }

    public int getEarn_2() {
        return earn_2;
    }

    public void setEarn_2(int earn_2) {
        this.earn_2 = earn_2;
    }

    public int getEarn_3() {
        return earn_3;
    }

    public void setEarn_3(int earn_3) {
        this.earn_3 = earn_3;
    }

    public int getEarn_4() {
        return earn_4;
    }

    public void setEarn_4(int earn_4) {
        this.earn_4 = earn_4;
    }

    public int getEarn_5() {
        return earn_5;
    }

    public void setEarn_5(int earn_5) {
        this.earn_5 = earn_5;
    }

    public int getEarn_6() {
        return earn_6;
    }

    public void setEarn_6(int earn_6) {
        this.earn_6 = earn_6;
    }

    public int getEarn_7() {
        return earn_7;
    }

    public void setEarn_7(int earn_7) {
        this.earn_7 = earn_7;
    }

    public int getEarn_8() {
        return earn_8;
    }

    public void setEarn_8(int earn_8) {
        this.earn_8 = earn_8;
    }

    public int getEarn_9() {
        return earn_9;
    }

    public void setEarn_9(int earn_9) {
        this.earn_9 = earn_9;
    }

    public int getItems_limit() {
        return items_limit;
    }

    public void setItems_limit(int items_limit) {
        this.items_limit = items_limit;
    }
}
