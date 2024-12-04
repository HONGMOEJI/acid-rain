// client/event/GameEventListener.java
package client.event;

public interface GameEventListener {
    void onGameEvent(String eventType, Object... data);
}