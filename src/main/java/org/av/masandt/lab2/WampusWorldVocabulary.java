package org.av.masandt.lab2;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;

public class WampusWorldVocabulary {

    public static String GAME_OVER_MSG_CONTENT = "GAME OVER";

    public static List<String> SPELEOLOGIST_NAVIGATOR_QUESTIONS_BLUEPRINT = asList("This is my first move! Where should I go?", "I feel %s");
    public static List<String> NAVIGATOR_SPELEOLOGIST_DIRECTION_ANSWER_BLUEPRINT = asList("You should go %s", "Step %s", "Move %s");
    public static List<String> SPELEOLOGIST_ENV_REQUEST_BLUEPRINT = asList("My navigator made me go in the %s. What is in here?");
    public static List<String> ENV_SPELEOLOGIST_ANSWER_BLUEPRINT = asList("You feel % here.", "You discovered yourself in a room with %s.");

    public final static List<String> WORLD_RESPONSE_LEXEMES = asList("smell", "wind", "shin", "no wampus", "wampus", "pit", "no pit", "gold", "nothing");
    public final static List<String> NAVIGATOR_INITIAL_REQUEST_LEXEMES = asList("first move");
    public final static List<String> NAVIGATOR_REQUEST_LEXEMES = asList("wind", "smell", "shin", "nothing");
    public final static List<String> NAVIGATOR_RESPONSE_LEXEMES = asList("move", "left", "right", "up", "down", "step", "go");

    public static List<String> getTextKeyLexemes(String text, List<String> lexemesList) {
        List<String> keys = new ArrayList<>();
        lexemesList.forEach(lexeme -> {
            String key = getTextKeyMask(text, lexeme);
            if (nonNull(key) && !key.isEmpty())
                keys.add(lexeme);
        });
        return keys;
    }

    public static String getTextKeyMask(String text, String gerExp) {
        Matcher matcher = Pattern.compile(gerExp).matcher(normalizeInput(text));
        return matcher.find() ? normalizeInput(text).substring(matcher.start(), matcher.end()) : null;
    }

    private static String normalizeInput(String text) {
        return text.toLowerCase().replace("?", "").replace(".", "").replace("!", "");
    }

}
