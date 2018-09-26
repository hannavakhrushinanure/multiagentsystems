package org.av.masandt.lab1.seller;

import javax.swing.*;
import java.awt.*;

public class SellerGui extends JFrame {

    private SellerAgent agent;

    public SellerGui(SellerAgent agent) {
        initUI();
        this.agent = agent;
    }

    private void initUI() {
        this.setTitle("Seller UI: CATS");
        this.setSize(400, 100);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.setVisible(true);

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());

        JLabel label1 = new JLabel("Enter name, cost: ");
        JTextField nameField = new JTextField();
        nameField.setBounds(128, 28, 86, 20);
        nameField.setColumns(10);

        JTextField costField = new JTextField();
        costField.setBounds(128, 28, 86, 20);
        costField.setColumns(10);

        JButton button = new JButton();
        button.setText("Add to catalogue");
        button.addActionListener(e -> {
            if (nameField.getText().isEmpty() || costField.getText().isEmpty()) {
                return;
            }
            try {
                this.agent.updateCatsCatalogue(nameField.getText(), Double.parseDouble(costField.getText()));
                System.out.println("The SELLER AGENT [" + agent.getName() + "] has added a new cat item to the catalogue. name: " + nameField.getText() + ", price : " + costField.getText());
            } catch (Exception e1) {
                System.out.println("The SELLER AGENT [" + agent.getName() + "] has failed to add a new cat item to the catalogue. name: " + nameField.getText() + ", price : " + costField.getText());
            }
            nameField.setText(null);
            costField.setText(null);
        });

        panel.add(label1);
        panel.add(nameField);
        panel.add(costField);
        panel.add(button);

        this.add(panel);
    }

    @Override
    public void dispose() {
        super.dispose();
        agent.doDelete();
    }

}
