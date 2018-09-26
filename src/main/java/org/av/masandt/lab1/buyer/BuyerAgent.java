package org.av.masandt.lab1.buyer;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import org.av.masandt.lab1.buyer.behaviour.RequestPerformer;

import java.util.ArrayList;
import java.util.List;

import static org.av.masandt.lab1.seller.SellerAgent.SERVICE_TYPE;

public class BuyerAgent extends Agent {

    // The name of the cat to buy
    private String targetCatName;

    // The list of known seller agents
    private List<AID> sellerAgents = new ArrayList<>();

    @Override
    protected void setup() {
        super.setup();
        System.out.println("New BUYER AGENT [" + this.getAID().getName() + "] is ready to proceed with buying cats.");

        // Get the target cat name as a start-up argument
        Object[] args = this.getArguments();
        if (args != null && args.length > 0) {
            this.targetCatName = (String) args[0];
            System.out.println("The BUYER AGENT [" + this.getAID().getName() + "] will try to buy " + targetCatName);

            // Add a TickerBehaviour that schedules a request to seller agents every 10 seconds,
            // fails after 1 minute of asking
            searchForSellersAndStartConversation(10000, 60000);
        } else {
            // Make the agent terminate immediately
            System.out.println("The BUYER AGENT [" + this.getAID().getName() + "] does not want to buy any cats now, terminating..");
            this.doDelete();
        }
    }

    private void searchForSellersAndStartConversation(long period, long maxDurationMillis) {
        final long startTime = System.currentTimeMillis();
        this.addBehaviour(new TickerBehaviour(this, period) {
            protected void onTick() {
                if (System.currentTimeMillis() - startTime < maxDurationMillis) {
                    System.out.println("The BUYER AGENT [" + myAgent.getAID().getName() + "] is looking for cats sellers...");
                    updateSellersList(this.myAgent);
                    this.myAgent.addBehaviour(new RequestPerformer(targetCatName, sellerAgents));
                } else {
                    System.out.println("The BUYER AGENT [" + myAgent.getAID().getName() + "]: " + "Purchasing attempt failed: timeout error");
                    this.myAgent.doDelete();
                }
            }
        });
    }

    private void updateSellersList(Agent agent) {
        // Update the list of seller agents
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(SERVICE_TYPE);
        template.addServices(sd);
        try {
            DFAgentDescription[] result = DFService.search(agent, template);
            for (DFAgentDescription aResult : result) {
                sellerAgents.add(aResult.getName());
                System.out.println("The BUYER AGENT [" + this.getAID().getName() + "] has discovered a new seller: [" + aResult.getName().getName() + "]");
            }
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    @Override
    protected void takeDown() {
        System.out.println("The BUYER AGENT [" + this.getAID().getName() + "] has been terminated successfully.");
        super.takeDown();
    }

}
