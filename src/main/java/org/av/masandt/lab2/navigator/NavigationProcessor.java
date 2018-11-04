package org.av.masandt.lab2.navigator;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.av.masandt.lab2.CaveRoom;
import org.av.masandt.lab2.MessageValidationException;
import org.av.masandt.lab2.PositionPointer;
import org.av.masandt.lab2.WampusWorldUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.av.masandt.lab2.WampusWorldUtils.FacingDirection.UP;
import static org.av.masandt.lab2.WampusWorldUtils.FacingDirection.DOWN;
import static org.av.masandt.lab2.WampusWorldUtils.FacingDirection.LEFT;
import static org.av.masandt.lab2.WampusWorldUtils.FacingDirection.RIGHT;
import static org.av.masandt.lab2.WampusWorldUtils.checkMessage;
import static org.av.masandt.lab2.WampusWorldUtils.getRandomAvailableDirection;
import static org.av.masandt.lab2.WampusWorldUtils.ONTOLOGY;
import static org.av.masandt.lab2.WampusWorldUtils.LANGUAGE;
import static org.av.masandt.lab2.WampusWorldUtils.NAVIGATION_CONVERSATION_ID;
import static org.av.masandt.lab2.WampusWorldUtils.getRandomElement;
import static org.av.masandt.lab2.WampusWorldUtils.getNextPosition;
import static org.av.masandt.lab2.WampusWorldUtils.getNextDirection;
import static org.av.masandt.lab2.WampusWorldUtils.Sense.SMELL;
import static org.av.masandt.lab2.WampusWorldUtils.Sense.WIND;
import static org.av.masandt.lab2.WampusWorldVocabulary.GAME_OVER_MSG_CONTENT;
import static org.av.masandt.lab2.WampusWorldVocabulary.getTextKeyLexemes;
import static org.av.masandt.lab2.WampusWorldVocabulary.NAVIGATOR_INITIAL_REQUEST_LEXEMES;
import static org.av.masandt.lab2.WampusWorldVocabulary.NAVIGATOR_REQUEST_LEXEMES;
import static org.av.masandt.lab2.WampusWorldVocabulary.NAVIGATOR_SPELEOLOGIST_DIRECTION_ANSWER_BLUEPRINT;

public class NavigationProcessor extends CyclicBehaviour {

    private Map<PositionPointer, CaveRoom> caveRoomMap;
    private PositionPointer speleologistPreviousPosition;
    private PositionPointer speleologistCurrentPosition;
    private Map<PositionPointer, Integer> hasBeenCounter;

    public NavigationProcessor() {
        caveRoomMap = setUpCaveState();
        hasBeenCounter = new HashMap<>();
    }

    @Override
    public void action() {
        ACLMessage message = this.myAgent.receive(MessageTemplate.MatchConversationId(NAVIGATION_CONVERSATION_ID));
        if (nonNull(message)) {
            ACLMessage reply = message.createReply();
            try {
                System.out.println("[NavigationProcessor]: received message: " + message.getContent());
                checkMessage(message);

                if (message.getContent().equals(GAME_OVER_MSG_CONTENT)) {
                    this.myAgent.doDelete();
                    return;
                }

                reply.setReplyWith(message.getReplyWith());
                reply.setOntology(ONTOLOGY);
                reply.setLanguage(LANGUAGE);
                reply.setPerformative(ACLMessage.INFORM);
                CaveRoom receivedSenses = parseInputMessage(message.getContent());
                WampusWorldUtils.FacingDirection directionPointer = processSenses(receivedSenses);
                System.out.println("[NavigationProcessor]: defined move: " + directionPointer);
                reply.setContent(formResponse(directionPointer));

            } catch (Exception e) {
                e.printStackTrace();
                reply.setPerformative(ACLMessage.FAILURE);
            }

            System.out.println("[NavigationProcessor]: responded with: " + reply.getContent());
            this.myAgent.send(reply);
        } else {
            block();
        }
    }

    private CaveRoom parseInputMessage(String msg) throws MessageValidationException {
        List<String> textKeyLexemesFirstMsg = getTextKeyLexemes(msg, NAVIGATOR_INITIAL_REQUEST_LEXEMES);
        if (!textKeyLexemesFirstMsg.isEmpty()) // is the first message
            return null;

        List<String> textKeyLexemes = getTextKeyLexemes(msg, NAVIGATOR_REQUEST_LEXEMES);
        if (textKeyLexemes.isEmpty())
            throw new MessageValidationException("[NavigationProcessor]: Unable to understand the request.");

        final CaveRoom senses = new CaveRoom();
        textKeyLexemes.forEach(lexeme -> {
            if (SMELL.getCorrespondingLexemes().contains(lexeme))
                senses.setSmell(true);
            if (WIND.getCorrespondingLexemes().contains(lexeme))
                senses.setWind(true);
        });
        return senses;
    }

    private String formResponse(WampusWorldUtils.FacingDirection direction) {
        return String.format(getRandomElement(NAVIGATOR_SPELEOLOGIST_DIRECTION_ANSWER_BLUEPRINT), getRandomElement(direction.getCorrespondingLexemes()));
    }

    private WampusWorldUtils.FacingDirection processSenses(CaveRoom receivedSenses) {
        System.out.println("[NavigationProcessor]: received senses: " + receivedSenses);

        if (isNull(receivedSenses)) { //the first entrance case // next move is safe, move default
            speleologistPreviousPosition = new PositionPointer(1, 1);
            WampusWorldUtils.FacingDirection randomAvailableDirection = getRandomAvailableDirection(speleologistPreviousPosition);
            speleologistCurrentPosition = getNextPosition(speleologistPreviousPosition, randomAvailableDirection);
            incHasBeen();
            return randomAvailableDirection;
        }

        // update the map
        caveRoomMap.put(speleologistCurrentPosition, receivedSenses);

        if (!receivedSenses.isSmell() && !receivedSenses.isWind()) { // next move is safe, move default
            speleologistPreviousPosition = speleologistCurrentPosition;
            WampusWorldUtils.FacingDirection randomAvailableDirection = getRandomAvailableDirection(speleologistPreviousPosition);
            speleologistCurrentPosition = getNextPosition(speleologistPreviousPosition, randomAvailableDirection);
            incHasBeen();
            return randomAvailableDirection;
        }

        PositionPointer upRoom = getNextPosition(speleologistCurrentPosition, UP);
        PositionPointer downRoom = getNextPosition(speleologistCurrentPosition, DOWN);
        PositionPointer rightRoom = getNextPosition(speleologistCurrentPosition, RIGHT);
        PositionPointer leftRoom = getNextPosition(speleologistCurrentPosition, LEFT);

        // adjust false danger prognoses
        if (!receivedSenses.isWind()) {
            Stream.of(upRoom, downRoom, leftRoom, rightRoom)
                    .filter(room -> nonNull(caveRoomMap.get(room)))
                    .forEach(room -> caveRoomMap.get(room).setPit(false));
        }
        if (!receivedSenses.isSmell()) {
            Stream.of(upRoom, downRoom, leftRoom, rightRoom)
                    .filter(room -> nonNull(caveRoomMap.get(room)))
                    .forEach(room -> caveRoomMap.get(room).setWampus(false));
        }

        // update the map by SMELL sense
        if (receivedSenses.isSmell()) {
            if (nonNull(upRoom) && upRoom != speleologistPreviousPosition && nonNull(caveRoomMap.get(upRoom)))
                caveRoomMap.get(upRoom).setWampus(true);
            if (nonNull(downRoom) && downRoom != speleologistPreviousPosition && nonNull(caveRoomMap.get(downRoom)))
                caveRoomMap.get(downRoom).setWampus(true);
            if (nonNull(rightRoom) && rightRoom != speleologistPreviousPosition && nonNull(caveRoomMap.get(rightRoom)))
                caveRoomMap.get(rightRoom).setWampus(true);
            if (nonNull(leftRoom) && leftRoom != speleologistPreviousPosition && nonNull(caveRoomMap.get(leftRoom)))
                caveRoomMap.get(leftRoom).setWampus(true);
        }

        // update the map by WIND sense
        if (receivedSenses.isWind()) {
            if (nonNull(upRoom) && upRoom != speleologistPreviousPosition && nonNull(caveRoomMap.get(upRoom)))
                caveRoomMap.get(upRoom).setPit(true);
            if (nonNull(downRoom) && downRoom != speleologistPreviousPosition && nonNull(caveRoomMap.get(downRoom)))
                caveRoomMap.get(downRoom).setPit(true);
            if (nonNull(rightRoom) && rightRoom != speleologistPreviousPosition && nonNull(caveRoomMap.get(rightRoom)))
                caveRoomMap.get(rightRoom).setPit(true);
            if (nonNull(leftRoom) && leftRoom != speleologistPreviousPosition && nonNull(caveRoomMap.get(leftRoom)))
                caveRoomMap.get(leftRoom).setPit(true);
        }

        // define next move
        final AtomicReference<PositionPointer> newSafeRoom = new AtomicReference<>();
        List<PositionPointer> newSafeRooms = Stream.of(upRoom, downRoom, leftRoom, rightRoom)
                .filter(room -> isRoomSafe(room) && !room.equals(speleologistPreviousPosition)).collect(toList());
        if (!newSafeRooms.isEmpty()) {
            Map<PositionPointer, Integer> map = newSafeRooms.stream().collect(toMap(Function.identity(), room -> hasBeenCounter.get(room)));
            map.forEach((key, value) -> {
                int min = 3;
                if (nonNull(value) && value < min) {
                    newSafeRoom.set(key);
                }
            });
        }

        if (isNull(newSafeRoom.get())) {
            newSafeRoom.set(hasBeenCounter.get(speleologistPreviousPosition) < 3 ? speleologistPreviousPosition : null);
        }

        if (nonNull(newSafeRoom.get())) {
            speleologistPreviousPosition = speleologistCurrentPosition;
            speleologistCurrentPosition = newSafeRoom.get();
            incHasBeen();
            return getNextDirection(speleologistPreviousPosition, newSafeRoom.get());
        }

        //there is no safe room, move default
        speleologistPreviousPosition = speleologistCurrentPosition;
        WampusWorldUtils.FacingDirection randomAvailableDirection = getRandomAvailableDirection(speleologistPreviousPosition);
        speleologistCurrentPosition = getNextPosition(speleologistPreviousPosition, randomAvailableDirection);
        incHasBeen();
        return randomAvailableDirection;
    }

    private void incHasBeen() {
        hasBeenCounter.put(speleologistPreviousPosition, isNull(hasBeenCounter.get(speleologistPreviousPosition)) ? 1
                : hasBeenCounter.get(speleologistPreviousPosition) + 1);
    }

    private boolean isRoomSafe(PositionPointer positionPointer) {
        CaveRoom room = caveRoomMap.get(positionPointer);
        return nonNull(room) && !room.isPit() && !room.isWampus();
    }

    private Map<PositionPointer, CaveRoom> setUpCaveState() {
        Map<PositionPointer, CaveRoom> caveRoomMap = new HashMap<>();
        for (int i = 1; i <= 4; i++) {
            for (int j = 1; j <= 4; j++) {
                caveRoomMap.put(new PositionPointer(i, j), new CaveRoom());
            }
        }

        return caveRoomMap;
    }

}
