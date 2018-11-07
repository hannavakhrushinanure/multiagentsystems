package org.av.masandt.lab2.environment;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.av.masandt.lab2.CaveRoom;
import org.av.masandt.lab2.MessageValidationException;
import org.av.masandt.lab2.PositionPointer;

import java.util.Map;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.av.masandt.lab2.WampusWorldUtils.Sense.NOTHING;
import static org.av.masandt.lab2.WampusWorldUtils.WORLD_INFO_CONVERSATION_ID;
import static org.av.masandt.lab2.WampusWorldUtils.checkMessage;
import static org.av.masandt.lab2.WampusWorldUtils.ONTOLOGY;
import static org.av.masandt.lab2.WampusWorldUtils.LANGUAGE;
import static org.av.masandt.lab2.WampusWorldUtils.processSensesToStr;
import static org.av.masandt.lab2.WampusWorldUtils.getRandomElement;
import static org.av.masandt.lab2.WampusWorldVocabulary.ENV_SPELEOLOGIST_ANSWER_BLUEPRINT;
import static org.av.masandt.lab2.WampusWorldVocabulary.GAME_OVER_MSG_CONTENT;
import static org.av.masandt.lab2.WampusWorldVocabulary.getTextKeyMask;

public class WordInfoProcessor extends CyclicBehaviour {

    private Map<PositionPointer, CaveRoom> roomMap;

    public WordInfoProcessor(Map<PositionPointer, CaveRoom> roomMap) {
        this.roomMap = roomMap;
    }

    @Override
    public void action() {
        ACLMessage message = this.myAgent.receive(MessageTemplate.MatchConversationId(WORLD_INFO_CONVERSATION_ID));
        if (nonNull(message)) {
            ACLMessage reply = message.createReply();
            try {
                System.out.println("[WordInfoProcessor]: received message: " + message.getContent());

                reply.setReplyWith(message.getReplyWith());
                reply.setOntology(ONTOLOGY);
                reply.setLanguage(LANGUAGE);

                checkMessage(message);

                if (message.getContent().equals(GAME_OVER_MSG_CONTENT)) {
                    this.myAgent.doDelete();
                    return;
                }

                PositionPointer position = parseInputMessage(message.getContent());
                CaveRoom roomState = this.roomMap.get(position);

                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent(formResponse(roomState));

            } catch (Exception e) {
                System.out.println(e.getMessage());
                reply.setPerformative(ACLMessage.FAILURE);
                reply.setContent(e.getMessage());
            }

            System.out.println("[WordInfoProcessor]: responded with: " + reply.getContent());
            this.myAgent.send(reply);
        } else {
            block();
        }
    }

    private PositionPointer parseInputMessage(String msg) throws MessageValidationException {
        String iRegexp = "\\d level";
        String jRegexp = "\\d room";

        String i = getTextKeyMask(msg, iRegexp);
        String j = getTextKeyMask(msg, jRegexp);
        if (isNull(i) || isNull(j))
            throw new MessageValidationException("[WordInfoProcessor]: Unable to parse the input message!");
        return new PositionPointer(Integer.parseInt(getTextKeyMask(i, "\\d")), Integer.parseInt(getTextKeyMask(j, "\\d")));
    }

    private String formResponse(CaveRoom roomState) {
        String sensesStr = processSensesToStr(roomState);
        return String.format(getRandomElement(ENV_SPELEOLOGIST_ANSWER_BLUEPRINT), sensesStr.isEmpty()
                ? NOTHING.getCorrespondingLexemes().get(0) : sensesStr);
    }

}
