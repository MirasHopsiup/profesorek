package miras.slack.gpt;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import java.util.List;

public class Welcome {

    ChatCompletionRequest prepare(String userName) {

        var messages = List.of(
            new ChatMessage(ChatMessageRole.USER.value(), "Please write a short poem about sad developer named " + userName)
        );

        return
        ChatCompletionRequest.builder()
            .messages(messages)
            .temperature(0.9)
            .model("gpt-3.5-turbo")
            .build();
    }


}
