package com.example.overlord.btech_project.model;

import com.example.overlord.btech_project.globals_immutable.Consumer;
import com.example.overlord.btech_project.globals_immutable.CollectionUtils;

import java.util.ArrayList;
import java.util.Deque;

/**
 * Created by overlord on 15/4/18.
 */

public class MovingWindowBuffer <T> {
    private int count;
    private int size;
    private Deque<T> dataBuffer;
    private boolean has_been_full_once = false;

    public MovingWindowBuffer(int size) {
        this.size = size;
    }

    public MovingWindowBuffer<T> add(T data) {
        //Deque not full
        if (dataBuffer.size() != size) {
            dataBuffer.addLast(data);
        }
        //Full Deque
        else {
            //Deque got full for the first time
            if ( ! has_been_full_once )
                has_been_full_once = true;

            //Moving Window of T/2 seconds
            else {
                dataBuffer.removeFirst();
                dataBuffer.addLast(data);
                count++;

                if (count == size / 2)
                    count = 0;
            }
        }
        return this;
    }

    public void onBufferFull(Consumer<ArrayList<T>> dataBufferConsumer) {
        if (has_been_full_once)
            dataBufferConsumer.accept(CollectionUtils.dequeToList(dataBuffer));
    }
}
