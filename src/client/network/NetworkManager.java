// client/network/NetworkManager.java
package client.network;

import java.io.*;
import java.net.Socket;

public class NetworkManager {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final MessageHandler messageHandler;
    private boolean connected = false;

    public NetworkManager(String host, int port, MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
        connect(host, port);
    }

    public void connect(String host, int port) {
        if (connected && socket != null && !socket.isClosed()) {
            System.out.println("이미 연결된 상태입니다. 새 연결을 생성하지 않습니다.");
            return;
        }

        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            connected = true;
            startReceiving();
        } catch (IOException e) {
            throw new RuntimeException("서버 연결 실패: " + e.getMessage());
        }
    }

    private void startReceiving() {
        new Thread(() -> {
            try {
                String message;
                while (connected && (message = in.readLine()) != null) {
                    messageHandler.handleMessage(message);
                }
            } catch (IOException e) {
                if (connected) {
                    System.err.println("서버와의 연결이 끊어졌습니다: " + e.getMessage());
                    disconnect();
                }
            }
        }).start();
    }

    public void sendMessage(String message) {
        if (connected && out != null) {
            out.println(message);
        }
    }

    public void disconnect() {
        connected = false;
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("연결 종료 중 오류 발생: " + e.getMessage());
        }
    }
}
