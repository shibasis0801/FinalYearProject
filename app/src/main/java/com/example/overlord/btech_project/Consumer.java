package com.example.overlord.btech_project;

/**
 * Created by overlord on 20/3/18.
 */

public interface Consumer<T> {
    void accept(T data);
}
