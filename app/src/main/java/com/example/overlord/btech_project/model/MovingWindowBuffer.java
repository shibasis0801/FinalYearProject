package com.example.overlord.btech_project.model;

import android.util.Log;

import com.example.overlord.btech_project.globals_immutable.Consumer;
import com.example.overlord.btech_project.globals_immutable.CollectionUtils;

import java.util.ArrayDeque;
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
    private Consumer<ArrayList<T>> dataBufferConsumer;


    public MovingWindowBuffer(int size) {
        this.size = size;
        dataBuffer = new ArrayDeque<>();
    }

    public MovingWindowBuffer<T> add(T data) {
        //Deque not full
        if (dataBuffer.size() != size) {
            dataBuffer.addLast(data);
        }
        //Full Deque
        else {
            //Deque got full for the first time
            if ( ! has_been_full_once ) {
                has_been_full_once = true;
                runConsumer();
            }


            //Moving Window of T/2 seconds
            else {
                dataBuffer.removeFirst();
                dataBuffer.addLast(data);
                count++;

                if (count == size / 2) {
                    count = 0;
                    runConsumer();
                }
            }
        }
        return this;
    }

    private void runConsumer() {
        dataBufferConsumer.accept(CollectionUtils.dequeToList(dataBuffer));
    }

    public void setOnBufferFullListener(Consumer<ArrayList<T>> dataBufferConsumer) {
        this.dataBufferConsumer = dataBufferConsumer;
    }
}
