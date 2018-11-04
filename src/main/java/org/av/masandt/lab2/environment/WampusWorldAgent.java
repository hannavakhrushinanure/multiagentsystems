package org.av.masandt.lab2.environment;

import jade.core.Agent;
import org.av.masandt.lab2.CaveRoom;
import org.av.masandt.lab2.PositionPointer;

import java.util.HashMap;
import java.util.Map;

public class WampusWorldAgent extends Agent {

    public static String NAME = "wampus-world";

    private Map<PositionPointer, CaveRoom> caveRoomMap;

    @Override
    protected void setup() {
        setUpCaveState();
        this.addBehaviour(new WordInfoProcessor(caveRoomMap));
        System.out.println("New WAMPUS WORLD AGENT [" + this.getAID().getName() + "] is ready.");
    }

    @Override
    protected void takeDown() {
        System.out.println("The WAMPUS WORLD AGENT [" + this.getAID().getName() + "] has been terminated successfully.");
    }

    private void setUpCaveState() {
        this.caveRoomMap = new HashMap<>();
        this.caveRoomMap.put(new PositionPointer(1, 1), new CaveRoom());
        this.caveRoomMap.put(new PositionPointer(1, 2), new CaveRoom(false, true, false, false, false));
        this.caveRoomMap.put(new PositionPointer(1, 3), new CaveRoom(false, false, false, false, true));
        this.caveRoomMap.put(new PositionPointer(1, 4), new CaveRoom(false, true, false, false, false));

        this.caveRoomMap.put(new PositionPointer(2, 1), new CaveRoom(true, false, false, false, false));
        this.caveRoomMap.put(new PositionPointer(2, 2), new CaveRoom());
        this.caveRoomMap.put(new PositionPointer(2, 3), new CaveRoom(false, true, false, false, false));
        this.caveRoomMap.put(new PositionPointer(2, 4), new CaveRoom());

        this.caveRoomMap.put(new PositionPointer(3, 1), new CaveRoom(false, false, false, true, false));
        this.caveRoomMap.put(new PositionPointer(3, 2), new CaveRoom(true, true, true, false, false));
        this.caveRoomMap.put(new PositionPointer(3, 3), new CaveRoom(false, false, false, false, true));
        this.caveRoomMap.put(new PositionPointer(3, 4), new CaveRoom(false, true, false, false, false));

        this.caveRoomMap.put(new PositionPointer(4, 1), new CaveRoom(true, false, false, false, false));
        this.caveRoomMap.put(new PositionPointer(4, 2), new CaveRoom());
        this.caveRoomMap.put(new PositionPointer(4, 3), new CaveRoom(false, true, false, false, false));
        this.caveRoomMap.put(new PositionPointer(4, 4), new CaveRoom(false, true, false, false, true));
    }

}
