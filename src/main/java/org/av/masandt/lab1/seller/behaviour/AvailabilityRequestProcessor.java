package org.av.masandt.lab1.seller.behaviour;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.HashMap;

public class AvailabilityRequestProcessor extends CyclicBehaviour {

    private HashMap<String, Double> catalogue;

    public AvailabilityRequestProcessor(HashMap<String, Double> catalogue) {
        super();
        this.catalogue = catalogue;
    }

    @Override
    public void action() {
        MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
        ACLMessage msg = this.myAgent.receive(mt);
        if (msg != null) {
            ACLMessage reply = msg.createReply();

            String title = msg.getContent();
            Double price = this.catalogue.get(title);
            if (price != null) { // check item exists
                reply.setPerformative(ACLMessage.PROPOSE);
                reply.setContent(String.valueOf(price));
            } else {
                reply.setPerformative(ACLMessage.REFUSE);
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
