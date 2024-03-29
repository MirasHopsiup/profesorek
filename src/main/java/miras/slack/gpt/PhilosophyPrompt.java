package miras.slack.gpt;

import static miras.slack.gpt.BotConstants.BOT_USER;

import com.slack.api.app_backend.events.payload.EventsApiPayload;
import com.slack.api.bolt.context.builtin.EventContext;
import com.slack.api.methods.SlackApiException;
import com.slack.api.model.event.MessageEvent;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PhilosophyPrompt {

    final EventsApiPayload<MessageEvent> payload;
    final EventContext ctx;

    public ChatCompletionRequest createPhilosophicPrompt() throws SlackApiException, IOException {

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system",
            "You are a tutor that always responds in the Socratic style. You never give the student the answer, "
                + "but always try to ask just the right question to help them learn to think for themselves. "
                + "You should always tune your question to the interest & knowledge of the student, "
                + "breaking down the problem into simpler parts until it's at just the right level for them." ));

        var previousMessages = getProviousMessages();
        messages.addAll(previousMessages);

        messages.add(new ChatMessage("user",
            payload.getEvent().getText()
        ));

        var chatRequest = ChatCompletionRequest.builder()
            .messages(messages)
            .temperature(0.2)
            .model("gpt-4")
            .build();

        return chatRequest;
    }

    List<ChatMessage> getProviousMessages() throws SlackApiException, IOException {
        List<ChatMessage> previousMessages = new ArrayList<>();

        if (payload.getEvent().getThreadTs() != null) {
            var res = ctx.client().conversationsReplies(r -> r
                .channel(payload.getEvent().getChannel())
                .limit(10)
                .ts(payload.getEvent().getThreadTs())
            );
            previousMessages =
                res.getMessages().stream()
                    .map(m -> {
                        if (BOT_USER.equals(m.getUser())) {
                            return new ChatMessage(ChatMessageRole.ASSISTANT.value(), m.getText());
                        } else {
                            return new ChatMessage(ChatMessageRole.USER.value(), m.getText());
                        }
                    })
                    .collect(Collectors.toList());
        }

        return previousMessages;
    }

}
