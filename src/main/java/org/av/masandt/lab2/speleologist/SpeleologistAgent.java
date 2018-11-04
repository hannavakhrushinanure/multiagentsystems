package org.av.masandt.lab2.speleologist;

import jade.core.AID;
import jade.core.Agent;
import org.av.masandt.lab2.environment.WampusWorldAgent;
import org.av.masandt.lab2.navigator.NavigatorAgent;

import static org.av.masandt.lab2.WampusWorldUtils.NAVIGATION_CONVERSATION_ID;
import static org.av.masandt.lab2.WampusWorldUtils.WORLD_INFO_CONVERSATION_ID;
import static org.av.masandt.lab2.WampusWorldUtils.sendMessage;
import static org.av.masandt.lab2.WampusWorldVocabulary.GAME_OVER_MSG_CONTENT;

public class SpeleologistAgent extends Agent {

    public static String NAME = "speleologist";

    private AID worldAgent = new AID(WampusWorldAgent.NAME,  AID.ISLOCALNAME);
    private AID navigatorAgent = new AID(NavigatorAgent.NAME, AID.ISLOCALNAME);

    @Override
    protected void setup() {
        this.addBehaviour(new CommunicationProcessor(worldAgent, navigatorAgent));
        System.out.println("New SPELEOLOGIST AGENT [" + this.getAID().getName() + "] is ready.");
    }

    @Override
    protected void takeDown() {
        sendMessage(NAVIGATION_CONVERSATION_ID, GAME_OVER_MSG_CONTENT, navigatorAgent, this);
        sendMessage(WORLD_INFO_CONVERSATION_ID, GAME_OVER_MSG_CONTENT, worldAgent, this);
        super.takeDown();
        System.out.println("The SPELEOLOGIST AGENT [" + this.getAID().getName() + "] has been terminated successfully.");
    }

}
