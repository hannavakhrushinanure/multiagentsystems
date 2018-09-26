package org.av.masandt.lab1.seller;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import org.av.masandt.lab1.seller.behaviour.AvailabilityRequestProcessor;
import org.av.masandt.lab1.seller.behaviour.PurchaseRequestProcessor;

import java.util.HashMap;

public class SellerAgent extends Agent {

    public static final String SERVICE_TYPE = "cats-selling";
    public static final String SERVICE_NAME = "cats-trading";

    private HashMap<String, Double> catsListWithPrices;

    protected void setup() {
        this.catsListWithPrices = new HashMap<>();
        new SellerGui(this);

        // Add the behaviour serving requests for offer from buyer agents
        this.addBehaviour(new AvailabilityRequestProcessor(this.catsListWithPrices));
        // Add the behaviour serving purchase orders from buyer agents
        this.addBehaviour(new PurchaseRequestProcessor(this.catsListWithPrices));

        registerSellerService();

        System.out.println("New SELLER AGENT [" + this.getAID().getName() + "] is ready to proceed with selling cats.");
    }

    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("The SELLER AGENT [" + this.getAID().getName() + "] has been terminated successfully.");
    }

    public void updateCatsCatalogue(String name, double price) {
        this.addBehaviour(new OneShotBehaviour() {
            public void action() {
                catsListWithPrices.put(name, price);
                System.out.println("The SELLER AGENT`s [" + this.getAgent().getAID().getName() + "] catalogue state: " + catsListWithPrices);
            }
        });
    }

    private void registerSellerService() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(this.getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(SERVICE_TYPE);
        sd.setName(SERVICE_NAME);
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

}
