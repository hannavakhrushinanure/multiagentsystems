package org.av.masandt.lab2;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaveRoom {

    boolean smell;
    boolean wind;
    boolean shine;

    boolean wampus;
    boolean pit;

    public boolean hasNoSenses(){
        return !smell && !wind && !shine && !wampus && !pit;
    }

    public void setNoSenses() {
        this.smell = false;
        this.wind = false;
        this.shine = false;
        this.wampus = false;
        this.pit = false;
    }
}
