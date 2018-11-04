package org.av.masandt.lab2.navigator;

import jade.core.Agent;

public class NavigatorAgent extends Agent {

    public static String NAME = "navigator";

    @Override
    protected void setup() {
        this.addBehaviour(new NavigationProcessor());
        System.out.println("New NAVIGATOR AGENT [" + this.getAID().getName() + "] is ready.");
    }

    @Override
    protected void takeDown() {
        System.out.println("The NAVIGATOR AGENT [" + this.getAID().getName() + "] has been terminated successfully.");
    }

}
