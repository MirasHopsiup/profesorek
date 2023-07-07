package miras.slack.gpt;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import java.util.List;
import java.util.Random;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class WelcomePrompt {

    Random r = new Random();

    ChatCompletionRequest createWelcomeProgrammerPrompt(String userName) {

        var poemType = PoemType.values()[r.nextInt(PoemType.values().length)];

        var messages = List.of(
            new ChatMessage(ChatMessageRole.USER.value(), "Please write a " + poemType.getType() + " about sad developer named " + userName)
        );

        return
        ChatCompletionRequest.builder()
            .messages(messages)
            .temperature(0.8)
            .model("gpt-4")
            .build();
    }

    ChatCompletionRequest createPhilosophyClientPrompt(String userName) {
        var messages = List.of(
            new ChatMessage(ChatMessageRole.USER.value(),
                "You are professor of philosophy. You always answer with the question. You use complex, sophisticated language. you are interested in sense and motivation." +
                "A stranger named " + userName + " enters your office. Welcome him/her.")
        );
        return
            ChatCompletionRequest.builder()
                .messages(messages)
                .temperature(0.8)
                .model("gpt-3.5-turbo")
                .build();
    }

    ChatCompletionRequest createPoemPrompt(String topic) {

        var poemType = PoemType.values()[r.nextInt(PoemType.values().length)];

        var messages = List.of(
            new ChatMessage(ChatMessageRole.USER.value(),
                "Please write a " + poemType.getType() + " about " + topic)
        );
        return
            ChatCompletionRequest.builder()
                .messages(messages)
                .temperature(0.8)
                .model("gpt-4")
                .build();
    }

    @RequiredArgsConstructor
    @Getter
    enum PoemType {
        LIM("two verses limerick"),
        HAIKU("haiku"),
        DIAM("diamante poem"),
        COU("couplet"),
        CIN("cinquain"),
        TAM("tanka poem");
        final String type;
    }

}
