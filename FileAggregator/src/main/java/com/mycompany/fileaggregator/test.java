/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.fileaggregator;

/**
 *
 * @author ntu-user
 */
public class test {
    public static void main(String[] args){
        FileService fs = new FileService();
        long id;
        try {
            id = fs.upload(1, "/home/ntu-user/update.sh", "/docs");
            System.out.println("uploaded id=" + id);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }
}
