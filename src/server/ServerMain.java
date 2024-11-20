// server/ServerMain.java
package server;

public class ServerMain {
    public static void main(String[] args) {
        GameServer server = new GameServer(12345);
        server.start();
    }
}
