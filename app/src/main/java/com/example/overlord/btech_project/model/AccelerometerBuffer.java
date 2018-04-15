package com.example.overlord.btech_project.model;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by overlord on 15/4/18.
 */
public class AccelerometerBuffer {
    private ConcurrentHashMap<Integer, ConcurrentLinkedQueue<float[]>> acceleroValues;

    private float averagedValues[] = new float[3];
    private long iterationCount = 0;

    public AccelerometerBuffer() {
        acceleroValues = new ConcurrentHashMap<>();

        for (int i = 0; i < 60; i++)
            acceleroValues.put(i, new ConcurrentLinkedQueue<>());
    }

    public void add(int second, float[] values) {
        acceleroValues.get(second).add(values);
    }


    public float[] getAverage(int second) {

        float[] average = new float[3];

        ConcurrentLinkedQueue<float[]> queue = acceleroValues.get(second);

        float n = (float) queue.size();

        if (queue.isEmpty())
            return averagedValues;

        else {
            iterationCount++;

            for (float[] values : queue)
                for (int i = 0; i < 3; i++)
                    average[i] += values[i];

            for (int i = 0; i < 3; i++) {
                average[i] = average[i] / n;
                averagedValues[i] += (average[i] - averagedValues[i]) / (float)(iterationCount);
            }

            return average;
        }
    }

    public void remove(int second) {
        acceleroValues.get(second).clear();
    }
}
