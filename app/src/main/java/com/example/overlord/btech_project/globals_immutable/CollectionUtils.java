package com.example.overlord.btech_project.globals_immutable;

import com.example.overlord.btech_project.model.Fused;

import java.util.ArrayList;
import java.util.Deque;

/**
 * Created by overlord on 15/4/18.
 */

public class CollectionUtils {
    public static <T> ArrayList<T> dequeToList(Deque<T> data) {
        ArrayList<T> dataArray = new ArrayList<>();
        dataArray.addAll(data);
        return dataArray;
    }
}
