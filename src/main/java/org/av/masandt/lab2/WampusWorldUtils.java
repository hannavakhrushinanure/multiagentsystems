package org.av.masandt.lab2;

import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static java.util.stream.Collectors.joining;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WampusWorldUtils {

    @ToString
    @Getter
    public enum FacingDirection {
        UP("up"), DOWN("down"), RIGHT("right"), LEFT("left");

        List<String> correspondingLexemes = new ArrayList<>();

        FacingDirection(String... lexemes) {
            correspondingLexemes.addAll(Arrays.asList(lexemes));
        }
    }

    @ToString
    @Getter
    public enum Sense {
        WIND("wind"), SHINE("shin"), SMELL("smell"), DEATH("wampus", "pit"), NOTHING("nothing");

        List<String> correspondingLexemes = new ArrayList<>();

        Sense(String... lexemes) {
            correspondingLexemes.addAll(Arrays.asList(lexemes));
        }
    }

    public static final String NAVIGATION_CONVERSATION_ID = "spel-nav-554433342";
    public static final String WORLD_INFO_CONVERSATION_ID = "spel-world-554433342";

    private static final String SECRET_CONVERSATION_KEY = "spel-554433342";

    public static final String LANGUAGE = "wampus-world eng";
    public static final String ONTOLOGY = "wampus-world ont";

    private static final long COMMUNICATION_TIMEOUT = 30000; // in millis

    public static boolean hasTimedOut(long startMillis, long currentMillis) {
        return currentMillis - startMillis > COMMUNICATION_TIMEOUT;
    }

    public static void sendMessage(String conversationId, String content, AID receiver, Agent sender) {
        if (!NAVIGATION_CONVERSATION_ID.equals(conversationId) && !WORLD_INFO_CONVERSATION_ID.equals(conversationId)) {
            throw new UnsupportedOperationException("Unable to define the message target.");
        }
        ACLMessage message = new ACLMessage(ACLMessage.INFORM);
        message.setContent(content);
        message.setLanguage(LANGUAGE);
        message.setOntology(ONTOLOGY);
        message.setReplyWith(SECRET_CONVERSATION_KEY); // защита от вражеских голосов
        message.addReceiver(receiver);
        message.setConversationId(conversationId);
        System.out.println("The message [" + message + "] was sent to the agent " + receiver.getLocalName());
        sender.send(message);
    }

    public static void checkMessage(ACLMessage message) throws MessageValidationException {
        if (!message.getOntology().equals(ONTOLOGY) || !message.getLanguage().equals(LANGUAGE)
                || !message.getReplyWith().equals(SECRET_CONVERSATION_KEY))
            throw new MessageValidationException("Unable to process the message received: " + message);
    }

    public static PositionPointer getNextPosition(PositionPointer currentPosition, FacingDirection nextDirection) {
        PositionPointer positionPointer = new PositionPointer();
        switch (nextDirection) {
            case UP:
                positionPointer.setI(currentPosition.getI() + 1);
                positionPointer.setJ(currentPosition.getJ());
                break;
            case DOWN:
                positionPointer.setI(currentPosition.getI() - 1);
                positionPointer.setJ(currentPosition.getJ());
                break;
            case LEFT:
                positionPointer.setI(currentPosition.getI());
                positionPointer.setJ(currentPosition.getJ() - 1);
                break;
            case RIGHT:
                positionPointer.setI(currentPosition.getI());
                positionPointer.setJ(currentPosition.getJ() + 1);
                break;
            default:
                return null;
        }
        return positionPointer;
    }

    public static FacingDirection getNextDirection(PositionPointer currentPosition, PositionPointer nextPosition) {
        if (nextPosition.getI() - currentPosition.getI() > 0) return FacingDirection.UP;
        if (nextPosition.getI() - currentPosition.getI() < 0) return FacingDirection.DOWN;
        if (nextPosition.getJ() - currentPosition.getJ() < 0) return FacingDirection.LEFT;
        if (nextPosition.getJ() - currentPosition.getJ() > 0) return FacingDirection.RIGHT;
        throw new UnsupportedOperationException();
    }

    public static String getRandomElement(List<String> list) {
        return list.get(new Random().nextInt(list.size()));
    }

    public static String processSensesToStr(CaveRoom currentSenses) {
        List<String> sensesStr = new ArrayList<>();
        if (currentSenses.isSmell())
            sensesStr.add(getRandomElement(Sense.SMELL.getCorrespondingLexemes()));
        if (currentSenses.isWind())
            sensesStr.add(getRandomElement(Sense.WIND.getCorrespondingLexemes()));
        if (currentSenses.isWampus())
            sensesStr.add(Sense.DEATH.getCorrespondingLexemes().get(0));
        if (currentSenses.isPit())
            sensesStr.add(Sense.DEATH.getCorrespondingLexemes().get(1));
        if (currentSenses.isShine())
            sensesStr.add(getRandomElement(Sense.SHINE.getCorrespondingLexemes()));
        return sensesStr.stream().collect(joining(", "));
    }

    public static FacingDirection getRandomAvailableDirection(PositionPointer position) {
        System.out.println(position);
        List<FacingDirection> availableDirections = new ArrayList<>();
        availableDirections.add(FacingDirection.RIGHT);
        availableDirections.add(FacingDirection.LEFT);
        availableDirections.add(FacingDirection.UP);
        availableDirections.add(FacingDirection.DOWN);

        if (position.getI() == 1) {
            availableDirections.remove(FacingDirection.DOWN);
        }
        if (position.getJ() == 1) {
            availableDirections.remove(FacingDirection.LEFT);
        }
        if (position.getI() == 4) {
            availableDirections.remove(FacingDirection.UP);
        }
        if (position.getJ() == 4) {
            availableDirections.remove(FacingDirection.RIGHT);
        }

        return availableDirections.isEmpty() ? FacingDirection.RIGHT :
                availableDirections.get(new Random().nextInt(availableDirections.size()));
    }

}
