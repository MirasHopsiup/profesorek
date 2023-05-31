package miras.slack.gpt;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import java.util.List;
import java.util.Random;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class Welcome {

    Random r = new Random();

    ChatCompletionRequest prepare(String userName) {

        var poemType = PoemType.values()[r.nextInt(PoemType.values().length)];

        var messages = List.of(
            new ChatMessage(ChatMessageRole.USER.value(), "Please write a " + poemType.getType() + " about sad developer named " + userName)
        );

        return
        ChatCompletionRequest.builder()
            .messages(messages)
            .temperature(0.5)
            .model("gpt-3.5-turbo")
            .build();
    }


    @RequiredArgsConstructor
    @Getter
    enum PoemType {
        LIM("two verses limerick"),
        HAIKU("haiku"),
        DIAM("diamante poem"),
        COU("couplet"),
        TAM("tanka poem");
        final String type;
    }

}
