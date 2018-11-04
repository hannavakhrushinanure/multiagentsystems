package org.av.masandt.lab2.speleologist;

import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.av.masandt.lab2.MessageValidationException;
import org.av.masandt.lab2.CaveRoom;
import org.av.masandt.lab2.PositionPointer;

import java.util.List;

import static java.util.Objects.nonNull;
import static org.av.masandt.lab2.WampusWorldUtils.FacingDirection.UP;
import static org.av.masandt.lab2.WampusWorldUtils.FacingDirection.DOWN;
import static org.av.masandt.lab2.WampusWorldUtils.FacingDirection.LEFT;
import static org.av.masandt.lab2.WampusWorldUtils.FacingDirection.RIGHT;
import static org.av.masandt.lab2.WampusWorldUtils.checkMessage;
import static org.av.masandt.lab2.WampusWorldUtils.sendMessage;
import static org.av.masandt.lab2.WampusWorldUtils.hasTimedOut;
import static org.av.masandt.lab2.WampusWorldUtils.processSensesToStr;
import static org.av.masandt.lab2.WampusWorldUtils.WORLD_INFO_CONVERSATION_ID;
import static org.av.masandt.lab2.WampusWorldUtils.NAVIGATION_CONVERSATION_ID;
import static org.av.masandt.lab2.WampusWorldUtils.getRandomElement;
import static org.av.masandt.lab2.WampusWorldUtils.getNextPosition;
import static org.av.masandt.lab2.WampusWorldUtils.Sense.SMELL;
import static org.av.masandt.lab2.WampusWorldUtils.Sense.WIND;
import static org.av.masandt.lab2.WampusWorldUtils.Sense.NOTHING;
import static org.av.masandt.lab2.WampusWorldUtils.Sense.SHINE;
import static org.av.masandt.lab2.WampusWorldUtils.Sense.DEATH;
import static org.av.masandt.lab2.WampusWorldVocabulary.getTextKeyLexemes;
import static org.av.masandt.lab2.WampusWorldVocabulary.SPELEOLOGIST_ENV_REQUEST_BLUEPRINT;
import static org.av.masandt.lab2.WampusWorldVocabulary.SPELEOLOGIST_NAVIGATOR_QUESTIONS_BLUEPRINT;
import static org.av.masandt.lab2.WampusWorldVocabulary.WORLD_RESPONSE_LEXEMES;
import static org.av.masandt.lab2.WampusWorldVocabulary.NAVIGATOR_RESPONSE_LEXEMES;

public class CommunicationProcessor extends Behaviour {

    private static final short MESSAGE_MAX_RESEND_TIMES = 5;

    private AID worldAgent;
    private AID navigatorAgent;

    private MessageTemplate messageTemplate;

    private PositionPointer currentPosition;
    private CaveRoom currenrSenses;

    private short step;
    private short failed;
    private boolean isAlive;
    private boolean hasWon;
    private boolean forceTermination = false;
    private boolean isInitilaStep = true;

    public CommunicationProcessor(AID worldAgent, AID navigatorAgent) {
        this.worldAgent = worldAgent;
        this.navigatorAgent = navigatorAgent;

        this.step = 0;
        currentPosition = new PositionPointer(1, 1);
        currenrSenses = new CaveRoom();
        isAlive = true;
        hasWon = false;
    }

    private long requestStartMillis;

    @Override
    public void action() {
        switch (step) {
            case 0:
                // Ask the navigator about current position
                sendMessage(NAVIGATION_CONVERSATION_ID, generateNavigationQuestion(isInitilaStep), navigatorAgent, this.myAgent);
                requestStartMillis = System.currentTimeMillis();
                isInitilaStep = false;
                // Prepare the template to get the response
                messageTemplate = MessageTemplate.MatchConversationId(NAVIGATION_CONVERSATION_ID);
                // move to the next step
                step = 1;
                break;
            case 1:
                // Get the response from the navigator
                if (hasTimedOut(requestStartMillis, System.currentTimeMillis())) {
                    System.out.println("[CommunicationProcessor]: Unable to communicate: request timed out!");
                    forceTermination = true;
                    break;
                }
                ACLMessage navReply = myAgent.receive(messageTemplate);
                if (nonNull(navReply)) {
                    try {
                        checkMessage(navReply);
                    } catch (MessageValidationException e) {
                        System.out.println("[CommunicationProcessor]: " + e.getMessage());
                        // if it is not possible to process the response - resend the question
                        resendMessage();
                        break;
                    }

                    if (navReply.getPerformative() == ACLMessage.FAILURE) {
                        //smth went wrong, resend the message
                        resendMessage();
                        break;
                    } else {
                        failed = 0;
                        try {
                            processNavigationResponse(navReply.getContent());
                        } catch (MessageValidationException e) {
                            System.out.println(e.getMessage());
                            // if it is not possible to process the response - resend the question
                            resendMessage();
                            break;
                        }
                        // move to the next step
                        step = 2;
                    }
                }
                break;
            case 2:
                // ping the environment
                sendMessage(WORLD_INFO_CONVERSATION_ID, generateEnvQuestion(), worldAgent, this.myAgent);
                // Prepare the template to get the response
                messageTemplate = MessageTemplate.MatchConversationId(WORLD_INFO_CONVERSATION_ID);
                // move to the next step
                step = 3;
                break;
            case 3:
                // Get response from the environment
                if (hasTimedOut(requestStartMillis, System.currentTimeMillis())) {
                    System.out.println("[CommunicationProcessor]: Unable to communicate: request timed out!");
                    forceTermination = true;
                    break;
                }
                ACLMessage envReply = myAgent.receive(messageTemplate);
                if (nonNull(envReply)) {
                    try {
                        checkMessage(envReply);
                    } catch (MessageValidationException e) {
                        //if it is not possible to process the response resend the question
                        resendMessage();
                        break;
                    }

                    if (envReply.getPerformative() == ACLMessage.FAILURE) {
                        //smth went wrong, resend the message
                        resendMessage();
                    } else {
                        failed = 0;
                        try {
                            processEnvironmentResponse(envReply.getContent());
                        } catch (MessageValidationException e) {
                            //smth went wrong, resend the message
                            resendMessage();
                            break;
                        }
                        // go and ask where to go next
                        step = 0;
                    }
                }
                break;
        }
    }

    @Override
    public boolean done() {
        if (forceTermination) {
            myAgent.doDelete();
        }

        if (failed > MESSAGE_MAX_RESEND_TIMES) {
            System.out.println("[CommunicationProcessor]: Too many failed responses, unable to maintain the conversation.");
            myAgent.doDelete();
        }

        if (isAlive && hasWon) {
            System.out.println("[CommunicationProcessor]: The quest has ended: I've WON!");
            myAgent.doDelete();
        }

        if (!isAlive) {
            System.out.println("[CommunicationProcessor]: The quest has ended: I was defeated.");
            myAgent.doDelete();
        }

        return false;
    }

    private void resendMessage() {
        step--;
        failed++;  // can resend MESSAGE_MAX_RESEND_TIMES times, see #done
    }

    private String generateEnvQuestion() {
        return String.format(getRandomElement(SPELEOLOGIST_ENV_REQUEST_BLUEPRINT),
                currentPosition.getJ() + " room on the " + currentPosition.getI() + " level");
    }

    private String generateNavigationQuestion(boolean isInitial) {
        if (isInitial)
            return SPELEOLOGIST_NAVIGATOR_QUESTIONS_BLUEPRINT.get(0);
        else
            return String.format(SPELEOLOGIST_NAVIGATOR_QUESTIONS_BLUEPRINT.get(1), !currenrSenses.hasNoSenses()
                    ? processSensesToStr(currenrSenses) : NOTHING.getCorrespondingLexemes().get(0));
    }

    private void processEnvironmentResponse(String message) throws MessageValidationException {
        List<String> textKeyLexemes = getTextKeyLexemes(message, WORLD_RESPONSE_LEXEMES);
        if (textKeyLexemes.isEmpty())
            throw new MessageValidationException("Unable to understand the env response.");

        textKeyLexemes.forEach(lexeme -> {
            if (SMELL.getCorrespondingLexemes().contains(lexeme))
                currenrSenses.setSmell(true);
            if (WIND.getCorrespondingLexemes().contains(lexeme))
                currenrSenses.setWind(true);
            if (SHINE.getCorrespondingLexemes().contains(lexeme))
                hasWon = true;
            if (DEATH.getCorrespondingLexemes().contains(lexeme))
                isAlive = false;
            if (NOTHING.getCorrespondingLexemes().contains(lexeme))
                currenrSenses.setNoSenses();
        });
    }

    private void processNavigationResponse(String message) throws MessageValidationException {
        List<String> textKeyLexemes = getTextKeyLexemes(message, NAVIGATOR_RESPONSE_LEXEMES);
        if (textKeyLexemes.isEmpty())
            throw new MessageValidationException("Unable to understand the response.");

        textKeyLexemes.forEach(lexeme -> {
            if (UP.getCorrespondingLexemes().contains(lexeme))
                currentPosition = getNextPosition(currentPosition, UP);
            if (DOWN.getCorrespondingLexemes().contains(lexeme))
                currentPosition = getNextPosition(currentPosition, DOWN);
            if (LEFT.getCorrespondingLexemes().contains(lexeme))
                currentPosition = getNextPosition(currentPosition, LEFT);
            if (RIGHT.getCorrespondingLexemes().contains(lexeme))
                currentPosition = getNextPosition(currentPosition, RIGHT);
        });
    }

}
