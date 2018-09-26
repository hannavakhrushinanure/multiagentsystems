package org.av.masandt.lab1.buyer.behaviour;

import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.List;

public class RequestPerformer extends Behaviour {

    private static final String CONVERSATION_ID = "cats-trade";

    private String targetCatName;
    private List<AID> sellerAgents;

    private AID bestSeller;
    private double bestPrice;
    private int repliesCnt = 0; // The counter of replies from seller agents
    private MessageTemplate mt; // The template to receive replies
    private int step = 0;

    public RequestPerformer(String targetCatName, List<AID> sellerAgents) {
        super();
        this.targetCatName = targetCatName;
        this.sellerAgents = sellerAgents;
    }

    public void action() {
        switch (step) {
            case 0:
                // Send the cfp to all sellers
                ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                sellerAgents.forEach(cfp::addReceiver);
                cfp.setContent(targetCatName);
                cfp.setConversationId(CONVERSATION_ID);
                cfp.setReplyWith("cfp" + System.currentTimeMillis()); // Unique value
                myAgent.send(cfp);

                // Prepare the template to get proposals
                mt = MessageTemplate.and(MessageTemplate.MatchConversationId(CONVERSATION_ID),
                        MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                step = 1;
                break;
            case 1:
                // Receive all proposals/refusals from seller agents
                ACLMessage reply = myAgent.receive(mt);
                if (reply != null) {
                    if (reply.getPerformative() == ACLMessage.PROPOSE) {
                        double price = Double.parseDouble(reply.getContent());
                        if (bestSeller == null || price < bestPrice) {
                            bestPrice = price;
                            bestSeller = reply.getSender();
                        }
                    }
                    repliesCnt++;
                    if (repliesCnt >= sellerAgents.size()) {
                        // We received all replies
                        step = 2;
                    }
                } else {
                    block();
                }
                break;
            case 2:
                // Send the purchase order to the seller that provided the best offer
                ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                order.addReceiver(bestSeller);
                order.setContent(targetCatName);
                order.setConversationId(CONVERSATION_ID);
                order.setReplyWith("order" + System.currentTimeMillis());
                myAgent.send(order);

                // Prepare the template to get the purchase order reply
                mt = MessageTemplate.and(MessageTemplate.MatchConversationId(CONVERSATION_ID),
                        MessageTemplate.MatchInReplyTo(order.getReplyWith()));
                step = 3;
                break;
            case 3:
                // Receive the purchase order reply
                reply = myAgent.receive(mt);
                if (reply != null) {
                    if (reply.getPerformative() == ACLMessage.INFORM) {
                        System.out.println("[" + myAgent.getAID().getName() + "]: " + targetCatName + " successfully purchased from agent " + reply.getSender().getName() + " for only " + bestPrice + " $. What a bargain!");
                        myAgent.doDelete();
                    } else {
                        System.out.println("[" + myAgent.getAID().getName() + "]: " + "Purchasing attempt failed: requested cat has been already sold.");
                    }

                    step = 4;
                } else {
                    block();
                }
                break;
        }
    }

    public boolean done() {
        if (step == 2 && bestSeller == null) {
            System.out.println("[" + myAgent.getAID().getName() + "]: " + "Purchasing attempt failed: "
                    + targetCatName + " is not available for sale.");
            return true;
        }
        return step == 4;
    }

}
