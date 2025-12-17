package io.github.xinfra.lab.raft;

public class Test {
    public static void main(String[] args) throws InterruptedException {

      Thread t =  new Thread(){
            public void run(){
                System.out.println("before interrupt");
                interrupt();
                System.out.println("after interrupt");
            }
          };
        t.start();


        Thread.currentThread().join();
    }


}
