package org.av.masandt.lab1.seller.behaviour;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.HashMap;

public class PurchaseRequestProcessor extends CyclicBehaviour {

    private HashMap<String, Double> catalogue;

    public PurchaseRequestProcessor(HashMap<String, Double> catalogue) {
        super();
        this.catalogue = catalogue;
    }

    @Override
    public void action() {
        MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
        ACLMessage msg = this.myAgent.receive(mt);
        if (msg != null) {
            ACLMessage reply = msg.createReply();

            String title = msg.getContent();
            Double price = this.catalogue.remove(title);
            if (price != null) {
                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent("sold");
                System.out.println("[" + this.myAgent.getAID().getName() + "]: " + title + " sold to agent " + msg.getSender().getName());
                System.out.println("The SELLER AGENT`s [" + this.getAgent().getAID().getName() + "] catalogue state: " + catalogue);
            } else {
                reply.setPerformative(ACLMessage.FAILURE);
                reply.setContent("not-available");
            }
            this.myAgent.send(reply);
        } else {
            block();
            /*  This method marks
                the behaviour as “blocked” so that the agent does not schedule it for execution anymore. When a new
                message is inserted in the agent’s message queue all blocked behaviours becomes available for execution
                again so that they have a chance to process the received message.
            */
        }
    }
}
