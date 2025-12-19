/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.loadbalancer;

/**
 *
 * @author ntu-user
 */
class Request {
    final String id;
    final long arrivalTime;
    final long delay;
    long delayStartTime;

    Request(String id, long delay) {
        this.id = id;
        this.delay = delay;
        this.arrivalTime = System.currentTimeMillis();
    }
}
