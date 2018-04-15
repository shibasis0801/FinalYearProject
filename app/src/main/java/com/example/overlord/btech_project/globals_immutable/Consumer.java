package com.example.overlord.btech_project.globals_immutable;

/**
 * Created by overlord on 20/3/18.
 */

public interface Consumer<T> {
    void accept(T data);
}
